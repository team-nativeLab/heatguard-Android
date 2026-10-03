package com.nativelap.heartguard.data.record.local

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecordSubmissionPersistenceTest {
    private lateinit var directory: File
    private lateinit var context: ContextWrapper

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(appContext.cacheDir, "submission-test-${System.nanoTime()}")
        check(directory.mkdirs())
        context = object : ContextWrapper(appContext) {
            override fun getNoBackupFilesDir(): File = directory
        }
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun durableMarkerSurvivesRecreationAndStaysAccountScoped() = runBlocking {
        val original = RecordSubmissionLocalDataSourceImpl(context, Dispatchers.IO)
        assertEquals(ApiResult.Success(true), original.begin("worker-a", submission()))
        val restarted = RecordSubmissionLocalDataSourceImpl(context, Dispatchers.IO)
        assertEquals(ApiResult.Success(listOf(submission())), restarted.read("worker-a"))
        assertEquals(ApiResult.Success(emptyList<StoredRecordSubmission>()), restarted.read("worker-b"))
        assertEquals(ApiResult.Success(false), restarted.begin("worker-a", submission()))
        assertEquals(ApiResult.Success(Unit), restarted.complete("worker-a", "intent-1"))
        assertEquals(ApiResult.Success(emptyList<StoredRecordSubmission>()), restarted.read("worker-a"))
    }

    @Test
    fun concurrentBeginCommitsOneMarker() = runBlocking {
        val journal = RecordSubmissionLocalDataSourceImpl(context, Dispatchers.IO)
        val attempts = (1..20).map {
            async { journal.begin("worker-a", submission()) }
        }.awaitAll()
        assertEquals(1, attempts.count { it == ApiResult.Success(true) })
        assertEquals(19, attempts.count { it == ApiResult.Success(false) })
        assertEquals(ApiResult.Success(listOf(submission())), journal.read("worker-a"))
    }

    @Test
    fun corruptJournalFailsClosedWithoutOverwritingOriginal() = runBlocking {
        val journal = RecordSubmissionLocalDataSourceImpl(context, Dispatchers.IO)
        journal.begin("worker-a", submission())
        val file = File(directory, "record-submissions").listFiles()!!.single()
        file.writeText("corrupted")
        assertEquals(ApiResult.Failure(ApiError.LocalStorage), journal.read("worker-a"))
        assertEquals(ApiResult.Failure(ApiError.LocalStorage), journal.begin("worker-a", submission()))
        assertEquals("corrupted", file.readText())
    }

    @Test
    fun interruptedFirstWriteRecoversWithoutDiscardingCommittedEntries() = runBlocking {
        val journal = RecordSubmissionLocalDataSourceImpl(context, Dispatchers.IO)
        journal.begin("worker-a", submission())
        val base = File(directory, "record-submissions").listFiles()!!.single()
        check(base.delete())
        File(base.path + ".new").writeText("partial initial write")
        assertEquals(ApiResult.Success(emptyList<StoredRecordSubmission>()), journal.read("worker-a"))
        assertEquals(ApiResult.Success(true), journal.begin("worker-a", submission()))
        assertEquals(ApiResult.Success(listOf(submission())), journal.read("worker-a"))
    }

    @Test
    fun unavailableStorageDoesNotPretendToCommit() = runBlocking {
        File(directory, "record-submissions").writeText("not a directory")
        val journal = RecordSubmissionLocalDataSourceImpl(context, Dispatchers.IO)
        assertEquals(ApiResult.Failure(ApiError.LocalStorage), journal.begin("worker-a", submission()))
    }

    private fun submission() = StoredRecordSubmission("intent-1", "WORK", "2026-10-04T01:00:00+09:00")
}
