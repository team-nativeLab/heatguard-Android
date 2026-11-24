package com.nativelap.heartguard.data.inquiry.local

import android.content.Context
import android.util.AtomicFile
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionScopedDataCleaner
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionConfirmation
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionGuard
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InquirySubmissionGuardLocalDataSourceImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val fingerprinter: InquiryInputFingerprinter,
    ) : InquirySubmissionGuardLocalDataSource,
        SessionScopedDataCleaner {
        private val journal = AtomicFile(File(context.noBackupFilesDir, JOURNAL_FILE_NAME))
        private val journalMutex = Mutex()
        private val journalJson = Json

        override suspend fun read(ownerId: String): ApiResult<InquirySubmissionGuard?> =
            accessJournal {
                val storedSubmission = readEntry()
                if (storedSubmission == null) {
                    null
                } else if (storedSubmission.ownerDigest != digestOwner(ownerId)) {
                    deleteJournal()
                    null
                } else {
                    InquirySubmissionGuard(storedSubmission.baselineInquiryIds.toSet())
                }
            }

        override suspend fun begin(
            ownerId: String,
            baselineInquiryIds: Set<String>,
            title: String,
            content: String,
        ): ApiResult<InquirySubmissionGuard> =
            accessJournal {
                require(ownerId.isNotBlank())
                require(baselineInquiryIds.size <= MAX_BASELINE_IDS)
                require(baselineInquiryIds.all(String::isNotBlank))
                val existingSubmission = readEntry()
                if (existingSubmission != null && existingSubmission.ownerDigest == digestOwner(ownerId)) {
                    throw IOException("An inquiry submission is already pending")
                }
                if (existingSubmission != null) deleteJournal()

                val inputFingerprint =
                    fingerprinter.fingerprint(
                        ownerId = ownerId,
                        title = title,
                        content = content,
                        createKeyIfMissing = true,
                    )
                val storedSubmission =
                    StoredInquirySubmission(
                        version = JOURNAL_VERSION,
                        ownerDigest = digestOwner(ownerId),
                        baselineInquiryIds = baselineInquiryIds.sorted(),
                        inputFingerprint = inputFingerprint,
                    )
                writeEntry(storedSubmission)
                InquirySubmissionGuard(baselineInquiryIds)
            }

        override suspend fun confirmAndClear(
            ownerId: String,
            inquiries: List<InquirySummary>,
            currentTitle: String,
            currentContent: String,
        ): ApiResult<InquirySubmissionConfirmation?> =
            accessJournal {
                val storedSubmission = readEntry() ?: return@accessJournal null
                if (storedSubmission.ownerDigest != digestOwner(ownerId)) {
                    deleteJournal()
                    return@accessJournal null
                }
                val baselineIds = storedSubmission.baselineInquiryIds.toSet()
                val isNewMatchingInquiryPresent =
                    inquiries.any { inquiry ->
                        matchesStoredInput(inquiry, baselineIds, storedSubmission.inputFingerprint, ownerId)
                    }
                if (!isNewMatchingInquiryPresent) return@accessJournal null

                val currentDraftFingerprint =
                    fingerprinter.fingerprint(
                        ownerId = ownerId,
                        title = currentTitle,
                        content = currentContent,
                        createKeyIfMissing = false,
                    )
                deleteJournal()
                InquirySubmissionConfirmation(currentDraftFingerprint == storedSubmission.inputFingerprint)
            }

        override suspend fun clear(ownerId: String): ApiResult<Unit> =
            accessJournal {
                val storedSubmission = readEntry()
                if (storedSubmission == null || storedSubmission.ownerDigest == digestOwner(ownerId)) {
                    deleteJournal()
                }
                Unit
            }

        override suspend fun clearSessionData() {
            withContext(ioDispatcher) {
                journalMutex.withLock {
                    deleteJournal()
                }
            }
        }

        override suspend fun onSessionRestored(sessionIdentifier: String) {
            withContext(ioDispatcher) {
                journalMutex.withLock {
                    val storedSubmission = readEntry() ?: return@withLock
                    if (storedSubmission.ownerDigest != digestOwner(sessionIdentifier)) {
                        deleteJournal()
                    }
                }
            }
        }

        private fun matchesStoredInput(
            inquiry: InquirySummary,
            baselineIds: Set<String>,
            inputFingerprint: String,
            ownerId: String,
        ): Boolean {
            val title = inquiry.title ?: return false
            val content = inquiry.content ?: return false
            if (inquiry.inquiryId in baselineIds) return false
            return fingerprinter.fingerprint(
                ownerId = ownerId,
                title = title,
                content = content,
                createKeyIfMissing = false,
            ) == inputFingerprint
        }

        private suspend fun <Value> accessJournal(operation: () -> Value): ApiResult<Value> =
            withContext(ioDispatcher) {
                journalMutex.withLock {
                    try {
                        val directory = journal.baseFile.parentFile
                        check(directory != null && (directory.isDirectory || directory.mkdirs()))
                        ApiResult.Success(operation())
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        ApiResult.Failure(ApiError.LocalStorage)
                    }
                }
            }

        private fun readEntry(): StoredInquirySubmission? {
            if (!journal.baseFile.exists() && !backupFile.exists()) return null
            val bytes = journal.openRead().use { input -> input.readNBytes(MAX_JOURNAL_BYTES + 1) }
            if (bytes.size > MAX_JOURNAL_BYTES) throw IOException("Inquiry guard exceeds size limit")
            val entry = journalJson.decodeFromString<StoredInquirySubmission>(bytes.toString(Charsets.UTF_8))
            if (entry.version != JOURNAL_VERSION ||
                !entry.ownerDigest.isLowerHexDigest() ||
                !entry.inputFingerprint.isLowerHexDigest() ||
                entry.baselineInquiryIds.size > MAX_BASELINE_IDS ||
                entry.baselineInquiryIds.any(String::isBlank) ||
                entry.baselineInquiryIds.distinct().size != entry.baselineInquiryIds.size
            ) {
                throw SerializationException("Invalid inquiry submission guard")
            }
            return entry
        }

        private fun writeEntry(entry: StoredInquirySubmission) {
            val bytes = journalJson.encodeToString(entry).toByteArray(Charsets.UTF_8)
            if (bytes.size > MAX_JOURNAL_BYTES) throw IOException("Inquiry guard exceeds size limit")
            val stream = journal.startWrite()
            try {
                stream.write(bytes)
                stream.fd.sync()
                journal.finishWrite(stream)
                if (!journal.openRead().use { it.readBytes() }.contentEquals(bytes)) {
                    throw IOException("Inquiry guard commit failed")
                }
            } catch (exception: Exception) {
                journal.failWrite(stream)
                throw exception
            }
        }

        private fun checkJournalDeleted() {
            check(
                !journal.baseFile.exists() &&
                    !backupFile.exists() &&
                    !newFile.exists(),
            )
        }

        private fun deleteJournal() {
            journal.delete()
            checkJournalDeleted()
        }

        private fun String.isLowerHexDigest(): Boolean =
            length == HEX_DIGEST_LENGTH &&
                all { character -> character in '0'..'9' || character in 'a'..'f' }

        private val backupFile: File get() = File(journal.baseFile.path + ".bak")
        private val newFile: File get() = File(journal.baseFile.path + ".new")

        private fun digestOwner(ownerId: String): String {
            require(ownerId.isNotBlank())
            return MessageDigest
                .getInstance(OWNER_DIGEST_ALGORITHM)
                .digest(ownerId.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
        }

        @Serializable
        private data class StoredInquirySubmission(
            @SerialName("version") val version: Int,
            @SerialName("owner_digest") val ownerDigest: String,
            @SerialName("baseline_inquiry_ids") val baselineInquiryIds: List<String>,
            @SerialName("input_fingerprint") val inputFingerprint: String,
        )

        private companion object {
            const val JOURNAL_FILE_NAME = "inquiry-submission-guard.json"
            const val JOURNAL_VERSION = 1
            const val MAX_BASELINE_IDS = 100
            const val MAX_JOURNAL_BYTES = 65_536
            const val HEX_DIGEST_LENGTH = 64
            const val OWNER_DIGEST_ALGORITHM = "SHA-256"
        }
    }
