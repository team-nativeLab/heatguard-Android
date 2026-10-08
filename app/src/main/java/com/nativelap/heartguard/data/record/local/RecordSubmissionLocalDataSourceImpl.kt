package com.nativelap.heartguard.data.record.local

import android.content.Context
import android.util.AtomicFile
import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 재시작에도 미확정 제출을 보존하며 백업에는 포함하지 않는다. */
@Singleton
class RecordSubmissionLocalDataSourceImpl
    @Inject
    constructor(
        @param:ApplicationContext context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : RecordSubmissionLocalDataSource {
        private val directory = File(context.noBackupFilesDir, "record-submissions")
        private val journalMutex = Mutex()
        private val journalJson = Json

        override suspend fun read(userId: String): ApiResult<List<StoredRecordSubmission>> =
            accessJournal(userId) { journal -> readEntries(journal) }

        override suspend fun begin(
            userId: String,
            submission: StoredRecordSubmission,
        ): ApiResult<Boolean> =
            accessJournal(userId) { journal ->
                val entries = readEntries(journal)
                if (entries.any { entry -> entry.submissionId == submission.submissionId }) {
                    false
                } else {
                    writeEntries(journal, entries + submission)
                    true
                }
            }

        override suspend fun complete(
            userId: String,
            submissionId: String,
        ): ApiResult<Unit> =
            accessJournal(userId) { journal ->
                val entries = readEntries(journal)
                writeEntries(journal, entries.filterNot { entry -> entry.submissionId == submissionId })
            }

        private suspend fun <JournalValue> accessJournal(
            userId: String,
            operation: (AtomicFile) -> JournalValue,
        ): ApiResult<JournalValue> =
            withContext(ioDispatcher) {
                journalMutex.withLock {
                    try {
                        require(userId.isNotBlank())
                        check(directory.isDirectory || directory.mkdirs())
                        val identity = "${BuildConfig.BASE_URL}\n$userId"
                        val fileName =
                            MessageDigest
                                .getInstance("SHA-256")
                                .digest(identity.toByteArray(Charsets.UTF_8))
                                .joinToString("") { byte -> "%02x".format(byte) }
                        ApiResult.Success(operation(AtomicFile(File(directory, "$fileName.json"))))
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        ApiResult.Failure(ApiError.LocalStorage)
                    }
                }
            }

        private fun readEntries(journal: AtomicFile): List<StoredRecordSubmission> {
            // 처음 쓰던 .new만 남았다면 아직 POST 이전이다. 완료된 base/backup 손상과 구분한다.
            if (!journal.baseFile.exists() && !File(journal.baseFile.path + ".bak").exists()) {
                return emptyList()
            }
            val contents =
                journal.openRead().use { stream ->
                    val bytes = stream.readNBytes(MAX_JOURNAL_BYTES + 1)
                    if (bytes.size > MAX_JOURNAL_BYTES) {
                        throw IOException("Submission journal exceeds size limit")
                    }
                    bytes.toString(Charsets.UTF_8)
                }
            val storedJournal = journalJson.decodeFromString<StoredJournal>(contents)
            if (storedJournal.version != JOURNAL_VERSION ||
                storedJournal.entries
                    .map { entry -> entry.submissionId }
                    .distinct()
                    .size != storedJournal.entries.size
            ) {
                throw SerializationException("Invalid submission journal")
            }
            storedJournal.entries.forEach { entry ->
                require(entry.submissionId.isNotBlank())
                require(entry.type in SUPPORTED_RECORD_TYPES)
                OffsetDateTime.parse(entry.measuredAt)
            }
            return storedJournal.entries
        }

        private fun writeEntries(
            journal: AtomicFile,
            entries: List<StoredRecordSubmission>,
        ) {
            val bytes = journalJson.encodeToString(StoredJournal(entries = entries)).toByteArray(Charsets.UTF_8)
            if (bytes.size > MAX_JOURNAL_BYTES) {
                throw IOException("Submission journal exceeds size limit")
            }
            val stream = journal.startWrite()
            try {
                stream.write(bytes)
                stream.fd.sync()
                journal.finishWrite(stream)
                if (!journal.readFully().contentEquals(bytes)) {
                    throw IOException("Submission journal commit failed")
                }
            } catch (failure: Exception) {
                journal.failWrite(stream)
                throw failure
            }
        }

        @Serializable
        private data class StoredJournal(
            @SerialName("version") val version: Int = JOURNAL_VERSION,
            @SerialName("entries") val entries: List<StoredRecordSubmission>,
        )

        private companion object {
            const val JOURNAL_VERSION = 1
            const val MAX_JOURNAL_BYTES = 1_048_576
            val SUPPORTED_RECORD_TYPES = setOf("THERMOMETER", "WORK", "REST")
        }
    }
