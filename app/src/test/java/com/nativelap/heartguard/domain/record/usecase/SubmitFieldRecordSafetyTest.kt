package com.nativelap.heartguard.domain.record.usecase

import android.net.Uri
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.createTestSessionManager
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission
import com.nativelap.heartguard.domain.record.model.RecordSubmitResult
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import com.nativelap.heartguard.domain.record.repository.RecordSubmissionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.OffsetDateTime

class SubmitFieldRecordSafetyTest {
    @Test
    fun `응답 유실 후 같은 제출은 POST하지 않고 별도 새 제출은 허용한다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.records.outcome = ApiResult.Failure(ApiError.Network)
            assertTrue(fixture.submit("first") is RecordSubmitResult.Unknown)
            assertTrue(fixture.submit("first") is RecordSubmitResult.Unknown)
            assertEquals(1, fixture.records.postCount)
            assertTrue(fixture.submit("new-intention") is RecordSubmitResult.Unknown)
            assertEquals(2, fixture.records.postCount)
            assertEquals(2, (fixture.pending() as ApiResult.Success).value.size)
        }

    @Test
    fun `새 UseCase 인스턴스도 이전 미확정 제출을 재전송하지 않는다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.records.outcome = ApiResult.Failure(ApiError.Network)
            fixture.submit("first")
            fixture.useCase = fixture.newUseCase()
            assertTrue(fixture.submit("first") is RecordSubmitResult.Unknown)
            assertEquals(1, fixture.records.postCount)
        }

    @Test
    fun `빈 기록 ID와 서버 오류 및 이미 사용된 업로드는 결과 불명이다`() =
        runBlocking {
            val failures =
                listOf(
                    ApiResult.Success(FieldRecord(" ", null, null, emptyList(), null)),
                    ApiResult.Failure(ApiError.Serialization),
                    ApiResult.Failure(ApiError.Http(500, "INTERNAL_ERROR")),
                    ApiResult.Failure(ApiError.Http(409, "UPLOAD_ALREADY_USED")),
                    ApiResult.Failure(ApiError.ServerRejected("UPLOAD_ALREADY_USED")),
                )
            failures.forEach { failure ->
                val fixture = Fixture()
                fixture.records.outcome = failure
                assertTrue(fixture.submit("first") is RecordSubmitResult.Unknown)
                assertEquals(1, (fixture.pending() as ApiResult.Success).value.size)
            }
        }

    @Test
    fun `명시 검증 거절은 같은 제출을 안전하게 재시도할 수 있다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.records.outcome = ApiResult.Failure(ApiError.Http(422, "WEATHER_BASELINE_REQUIRED"))
            assertTrue(fixture.submit("first") is RecordSubmitResult.NotSaved)
            assertEquals(emptyList<PendingRecordSubmission>(), (fixture.pending() as ApiResult.Success).value)
            fixture.records.outcome = ApiResult.Success(FieldRecord("record-1", null, null, emptyList(), null))
            assertTrue(fixture.submit("first") is RecordSubmitResult.Saved)
            assertEquals(2, fixture.records.postCount)
            assertEquals(listOf(fixture.measuredAt, fixture.measuredAt), fixture.records.measuredTimes)
        }

    @Test
    fun `저장소 기록 실패나 계정 확인 실패면 POST를 보내지 않는다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.journal.failWrites = true
            assertEquals(RecordSubmitResult.NotSaved(ApiError.LocalStorage), fixture.submit("first"))
            fixture.journal.failWrites = false
            fixture.profile.failure = ApiError.Network
            assertEquals(RecordSubmitResult.NotSaved(ApiError.Network), fixture.submit("first"))
            assertEquals(0, fixture.records.postCount)
        }

    @Test
    fun `POST 중 취소는 전파하고 제출 표식을 남긴다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.records.beforeReturn = { throw CancellationException("cancel") }
            try {
                fixture.submit("first")
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
                assertEquals(1, (fixture.pending() as ApiResult.Success).value.size)
            }
        }

    @Test
    fun `계정 A의 미확정 결과는 B에게 보이지 않고 A 재로그인시 복원된다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.records.outcome = ApiResult.Failure(ApiError.Network)
            fixture.submit("first")
            fixture.profile.userId = "B"
            fixture.session.onLoginSucceeded(SessionToken("session-b"))
            assertTrue((fixture.pending() as ApiResult.Success).value.isEmpty())
            fixture.profile.userId = "A"
            fixture.session.onLoginSucceeded(SessionToken("session-a-new"))
            assertEquals("first", (fixture.pending() as ApiResult.Success).value.single().submissionId)
            assertTrue(fixture.submit("first") is RecordSubmitResult.Unknown)
            assertEquals(1, fixture.records.postCount)
        }

    @Test
    fun `프로필 요청 중 세션이 바뀌면 이전 계정 표식과 POST를 만들지 않는다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.profile.beforeReturn = { fixture.session.onLoginSucceeded(SessionToken("changed")) }
            assertEquals(RecordSubmitResult.NotSaved(ApiError.SessionChanged), fixture.submit("first"))
            assertEquals(0, fixture.records.postCount)
            assertTrue(fixture.journal.entries.isEmpty())
        }

    @Test
    fun `거절 뒤 표식 정리에 실패하면 재전송을 허용하지 않는다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.records.outcome = ApiResult.Failure(ApiError.Http(400, "VALIDATION_ERROR"))
            fixture.journal.failCompletion = true
            assertEquals(RecordSubmitResult.Unknown(ApiError.LocalStorage), fixture.submit("first"))
            assertTrue(fixture.submit("first") is RecordSubmitResult.Unknown)
            assertEquals(1, fixture.records.postCount)
        }

    @Test
    fun `서버 성공 후 로컬 정리 실패는 성공 사실을 유지하고 로컬 정리만 재시도한다`() =
        runBlocking {
            val fixture = Fixture()
            fixture.journal.failCompletion = true
            val saved = fixture.submit("first") as RecordSubmitResult.Saved
            assertEquals("record-1", saved.record.recordId)
            assertNotNull(saved.pendingCleanup)
            fixture.journal.failCompletion = false
            val cleanup =
                RetryRecordSubmissionCleanupUseCase(
                    fixture.journal,
                    GetWorkerProfileUseCase(fixture.profile),
                    fixture.session,
                )
            assertEquals(ApiResult.Success(Unit), cleanup(requireNotNull(saved.pendingCleanup)))
            assertEquals(1, fixture.records.postCount)
            assertTrue((fixture.pending() as ApiResult.Success).value.isEmpty())
        }

    private class Fixture {
        val session = createTestSessionManager(Dispatchers.Unconfined)
        val journal = MemoryJournal()
        val profile = FakeProfile()
        val records = FakeRecords(journal)
        val measuredAt = OffsetDateTime.parse("2026-10-04T01:00:00+09:00")
        var useCase = newUseCase()

        fun newUseCase() = SubmitFieldRecordUseCase(records, journal, GetWorkerProfileUseCase(profile), session)

        suspend fun submit(id: String) =
            useCase(
                submissionId = id,
                type = FieldRecordType.WORK,
                photoKeys = listOf("photo-key"),
                measuredAt = measuredAt,
                temperature = null,
                humidity = null,
                memo = null,
                restStartedAt = null,
                restEndedAt = null,
            )

        suspend fun pending() = GetPendingRecordSubmissionsUseCase(journal, GetWorkerProfileUseCase(profile), session)()
    }

    private class MemoryJournal : RecordSubmissionRepository {
        val entries = mutableMapOf<String, MutableMap<String, PendingRecordSubmission>>()
        var failWrites = false
        var failCompletion = false

        override suspend fun getPendingSubmissions(userId: String) =
            ApiResult.Success(entries[userId]?.values.orEmpty().toList())

        override suspend fun beginSubmission(
            userId: String,
            submission: PendingRecordSubmission,
        ): ApiResult<Boolean> {
            if (failWrites) {
                return ApiResult.Failure(ApiError.LocalStorage)
            }
            val accountEntries = entries.getOrPut(userId) { mutableMapOf() }
            if (submission.submissionId in accountEntries) {
                return ApiResult.Success(false)
            }
            accountEntries[submission.submissionId] = submission
            return ApiResult.Success(true)
        }

        override suspend fun completeSubmission(
            userId: String,
            submissionId: String,
        ): ApiResult<Unit> {
            if (failCompletion) {
                return ApiResult.Failure(ApiError.LocalStorage)
            }
            entries[userId]?.remove(submissionId)
            return ApiResult.Success(Unit)
        }
    }

    private class FakeProfile : WorkerProfileRepository {
        var userId = "A"
        var failure: ApiError? = null
        var beforeReturn: suspend () -> Unit = {}

        override suspend fun getWorkerProfile(): ApiResult<WorkerProfile> {
            beforeReturn()
            return failure?.let { ApiResult.Failure(it) } ?: ApiResult.Success(WorkerProfile(userId, null, null))
        }

        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
        ) = ProfileUpdateResult.Failure

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ) = PasswordChangeResult.Failure
    }

    private class FakeRecords(
        private val journal: MemoryJournal,
    ) : RecordRepository {
        var postCount = 0
        val measuredTimes = mutableListOf<OffsetDateTime>()
        var beforeReturn: suspend () -> Unit = {}
        var outcome: ApiResult<FieldRecord> = ApiResult.Success(FieldRecord("record-1", null, null, emptyList(), null))

        override suspend fun uploadFieldPhotos(
            photoUris: List<Uri>,
            alreadyUploadedPhotoKeys: Map<Uri, String>,
            onPhotoUploaded: (Uri, String) -> Unit,
        ): ApiResult<List<String>> = error("Unused")

        override suspend fun submitFieldRecord(
            expectedSessionGeneration: Long,
            type: FieldRecordType,
            photoKeys: List<String>,
            measuredAt: OffsetDateTime,
            temperature: Double?,
            humidity: Double?,
            memo: String?,
            restStartedAt: OffsetDateTime?,
            restEndedAt: OffsetDateTime?,
        ): ApiResult<FieldRecord> {
            assertTrue(journal.entries.values.any { it.isNotEmpty() })
            postCount += 1
            measuredTimes += measuredAt
            beforeReturn()
            return outcome
        }
    }
}
