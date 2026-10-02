package com.nativelap.heartguard.core.session

import com.nativelap.heartguard.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 세션 토큰 저장과 인증 상태 공개를 단일 진입점으로 관리한다. */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val sessionMutex = Mutex()
    private var isInitialized = false

    // 앱 시작 때 저장 토큰으로 복원한 세션의 세대다. 새로 로그인한 세션과 구분해 이전 프로세스의 입력 복원 여부를 정한다.
    @Volatile
    private var restoredGeneration: Long? = null

    // 로그인·로그아웃마다 세대가 바뀌므로, 상태가 빠르게 왕복해도 수집자가 계정 전환을 놓치지 않는다.
    private val mutableSessionSnapshot = MutableStateFlow(
        SessionSnapshot(generation = 0L, accessToken = null),
    )

    val sessionSnapshot: StateFlow<SessionSnapshot> = mutableSessionSnapshot.asStateFlow()

    private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.Initializing)

    val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

    // 세션 만료처럼 화면이 "지금 막 발생한 일"로 한 번만 반응해야 하는 이벤트를 위한 별도 채널이다.
    // sessionState만으로는 Unauthenticated로의 전환이 로그아웃 직후인지 앱 시작 시 최초 상태인지 구분할 수 없다.
    private val mutableSessionEvents = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val sessionEvents: SharedFlow<SessionEvent> = mutableSessionEvents.asSharedFlow()

    fun getSnapshot(): SessionSnapshot = mutableSessionSnapshot.value

    /** 현재 인증 세션이 앱 시작 때 저장 토큰으로 복원된 세션인지다. 새 로그인 세션이면 false다. */
    fun isRestoredSession(): Boolean {
        return mutableSessionState.value == SessionState.Authenticated &&
            restoredGeneration == getSnapshot().generation
    }

    /** 저장된 토큰을 읽어 최초 세션을 정한다. 읽기·복호화가 실패하면 로그아웃 상태로 시작한다. */
    suspend fun initialize() {
        sessionMutex.withLock {
            if (isInitialized) {
                return
            }
            withContext(ioDispatcher) {
                val storedTokenRead = readStoredAccessToken()
                currentCoroutineContext().ensureActive()
                withContext(NonCancellable) {
                    val storedAccessToken = storedTokenRead.accessToken
                    val validAccessToken = storedAccessToken?.takeIf {
                        it.isNotBlank() && it != LEGACY_PLACEHOLDER_ACCESS_TOKEN
                    }
                    if (storedTokenRead.isUnreadable || (storedAccessToken != null && validAccessToken == null)) {
                        clearStoredTokenSafely()
                    }
                    val initializedGeneration = getSnapshot().generation + 1L
                    mutableSessionSnapshot.value = SessionSnapshot(
                        generation = initializedGeneration,
                        accessToken = validAccessToken,
                    )
                    restoredGeneration = initializedGeneration.takeIf { validAccessToken != null }
                    mutableSessionState.value = if (validAccessToken == null) {
                        SessionState.Unauthenticated
                    } else {
                        SessionState.Authenticated
                    }
                    isInitialized = true
                }
            }
        }
    }

    suspend fun onLoginSucceeded(sessionToken: SessionToken) {
        sessionMutex.withLock {
            currentCoroutineContext().ensureActive()
            withContext(ioDispatcher + NonCancellable) {
                tokenStorage.saveAccessToken(sessionToken.accessToken)
                mutableSessionSnapshot.value = SessionSnapshot(
                    generation = getSnapshot().generation + 1L,
                    accessToken = sessionToken.accessToken,
                )
                mutableSessionState.value = SessionState.Authenticated
                isInitialized = true
            }
        }
    }

    /** 탈퇴한 세션의 요청 자격만 없애고 완료 화면을 위한 논리 세션을 유지한다.
     * 저장소 삭제가 실패해도 메모리 토큰은 비우고 성공으로 처리한다(저장소가 키 폐기로 복구한다). */
    suspend fun clearAccessTokenAfterWithdrawal(expectedGeneration: Long = getSnapshot().generation): Boolean {
        return sessionMutex.withLock {
            if (getSnapshot().generation != expectedGeneration ||
                mutableSessionState.value != SessionState.Authenticated
            ) {
                return@withLock false
            }
            currentCoroutineContext().ensureActive()
            withContext(ioDispatcher + NonCancellable) {
                mutableSessionSnapshot.value = getSnapshot().copy(accessToken = null)
                clearStoredTokenSafely()
            }
            true
        }
    }

    /** 현재 인증 요청과 정확히 같은 세션일 때만 401을 반영한다. */
    suspend fun expireSessionForRequest(expectedSnapshot: SessionSnapshot): Boolean {
        return sessionMutex.withLock {
            if (expectedSnapshot.accessToken.isNullOrBlank() || getSnapshot() != expectedSnapshot) {
                return@withLock false
            }
            clearSession(expectedSnapshot.generation)
        }
    }

    /** 명시적 종료는 토큰 선삭제 후에도 해당 논리 세션만 만료한다. */
    suspend fun expireSession(expectedGeneration: Long = getSnapshot().generation): Boolean {
        return sessionMutex.withLock {
            clearSession(expectedGeneration)
        }
    }

    /** 메모리 세션을 먼저 끝내고 저장소를 정리한다. 저장소 삭제 실패가 로그아웃을 막지 않는다. */
    private suspend fun clearSession(expectedGeneration: Long): Boolean {
        if (getSnapshot().generation != expectedGeneration ||
            mutableSessionState.value == SessionState.Unauthenticated
        ) {
            return false
        }
        currentCoroutineContext().ensureActive()
        withContext(ioDispatcher + NonCancellable) {
            mutableSessionSnapshot.value = SessionSnapshot(
                generation = getSnapshot().generation + 1L,
                accessToken = null,
            )
            mutableSessionState.value = SessionState.Unauthenticated
            isInitialized = true
            mutableSessionEvents.tryEmit(SessionEvent.Expired)
            clearStoredTokenSafely()
        }
        return true
    }

    private fun readStoredAccessToken(): StoredTokenRead {
        return try {
            StoredTokenRead(
                accessToken = tokenStorage.readAccessToken(),
                isUnreadable = false,
            )
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            // 손상된 저장값은 신뢰하지 않고 로그아웃 상태로 시작한 뒤 삭제를 다시 시도한다.
            StoredTokenRead(
                accessToken = null,
                isUnreadable = true,
            )
        }
    }

    private data class StoredTokenRead(
        val accessToken: String?,
        val isUnreadable: Boolean,
    )

    private fun clearStoredTokenSafely() {
        try {
            tokenStorage.clear()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            // 저장소 구현이 키 폐기까지 실패한 경우다. 메모리 세션은 이미 끝났고 다음 시작에서 다시 시도한다.
        }
    }

    private companion object {
        const val LEGACY_PLACEHOLDER_ACCESS_TOKEN = "placeholder-access-token"
    }
}
