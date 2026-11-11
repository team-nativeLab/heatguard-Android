package com.nativelap.heartguard.core.session

import android.util.Log
import com.nativelap.heartguard.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmSuppressWildcards

/** 세션 토큰 저장과 인증 상태 공개를 단일 진입점으로 관리한다. */
@Singleton
class SessionManager
    @Inject
    constructor(
        private val tokenStorage: TokenStorage,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val sessionScopedDataCleaners: Set<@JvmSuppressWildcards SessionScopedDataCleaner>,
    ) {
        private val sessionMutex = Mutex()
        private val selectedTeamLock = Any()
        private var isInitialized = false

        // 앱 시작 때 저장 토큰으로 복원한 세션의 세대다. 새로 로그인한 세션과 구분해 이전 프로세스의 입력 복원 여부를 정한다.
        @Volatile
        private var restoredGeneration: Long? = null

        // 복원 세션의 서버 확인을 끝낸 세대다. 프로세스 안에서만 기억해 Activity를 다시 열 때 확인을 반복하지 않는다.
        @Volatile
        private var startupCheckedGeneration: Long? = null

        @Volatile
        private var selectedTeamValidatedGeneration: Long? = null

        @Volatile
        private var selectedTeamSelectionVersion: Long = 0L

        // 로그인·로그아웃마다 세대가 바뀌므로, 상태가 빠르게 왕복해도 수집자가 계정 전환을 놓치지 않는다.
        private val mutableSessionSnapshot =
            MutableStateFlow(
                SessionSnapshot(generation = 0L, accessToken = null),
            )

        val sessionSnapshot: StateFlow<SessionSnapshot> = mutableSessionSnapshot.asStateFlow()

        private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.Initializing)

        val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

        // 세션 만료처럼 화면이 "지금 막 발생한 일"로 한 번만 반응해야 하는 이벤트를 위한 별도 채널이다.
        // sessionState만으로는 Unauthenticated로의 전환이 로그아웃 직후인지 앱 시작 시 최초 상태인지 구분할 수 없다.
        private val mutableSessionEvents =
            MutableSharedFlow<SessionEvent>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        val sessionEvents: SharedFlow<SessionEvent> = mutableSessionEvents.asSharedFlow()

        fun getSnapshot(): SessionSnapshot = mutableSessionSnapshot.value

        fun getSelectedSiteId(): String? = tokenStorage.readSelectedSiteId()

        fun getSelectedTeamId(): String? = tokenStorage.readSelectedTeamId()

        fun getSelectedTeamIdForSession(expectedSnapshot: SessionSnapshot): String? =
            synchronized(selectedTeamLock) {
                if (getSnapshot() == expectedSnapshot &&
                    expectedSnapshot.role == SessionRole.TEAM_MEMBER &&
                    selectedTeamValidatedGeneration == expectedSnapshot.generation
                ) {
                    tokenStorage.readSelectedTeamId()
                } else {
                    null
                }
            }

        fun getSelectedTeamRequestSnapshot(expectedSnapshot: SessionSnapshot): SelectedTeamRequestSnapshot? =
            synchronized(selectedTeamLock) {
                val teamId = getSelectedTeamIdForSession(expectedSnapshot)?.takeIf(String::isNotBlank) ?: return null
                SelectedTeamRequestSnapshot(teamId, selectedTeamSelectionVersion)
            }

        fun isSelectedTeamRequestSnapshotCurrent(
            expectedSnapshot: SessionSnapshot,
            selectedTeamRequestSnapshot: SelectedTeamRequestSnapshot,
        ): Boolean =
            synchronized(selectedTeamLock) {
                getSnapshot() == expectedSnapshot &&
                    selectedTeamValidatedGeneration == expectedSnapshot.generation &&
                    selectedTeamSelectionVersion == selectedTeamRequestSnapshot.selectionVersion &&
                    tokenStorage.readSelectedTeamId() == selectedTeamRequestSnapshot.teamId
            }

        suspend fun saveSelectedTeamAndSiteIdsForSession(
            teamId: String,
            siteId: String,
            expectedTeamId: String?,
            expectedSiteId: String?,
            expectedSnapshot: SessionSnapshot,
        ): Boolean =
            sessionMutex.withLock {
                if (getSnapshot() != expectedSnapshot || expectedSnapshot.role != SessionRole.TEAM_MEMBER ||
                    tokenStorage.readSelectedTeamId() != expectedTeamId ||
                    tokenStorage.readSelectedSiteId() != expectedSiteId
                ) {
                    return@withLock false
                }
                currentCoroutineContext().ensureActive()
                withContext(ioDispatcher) {
                    synchronized(selectedTeamLock) {
                        tokenStorage.saveSelectedTeamAndSiteIds(teamId, siteId)
                        selectedTeamSelectionVersion += 1L
                        selectedTeamValidatedGeneration = expectedSnapshot.generation
                    }
                }
                true
            }

        suspend fun clearSelectedTeamAndSiteIdsForSession(
            expectedTeamId: String?,
            expectedSiteId: String?,
            expectedSnapshot: SessionSnapshot,
        ): Boolean =
            sessionMutex.withLock {
                if (getSnapshot() != expectedSnapshot || expectedSnapshot.role != SessionRole.TEAM_MEMBER ||
                    tokenStorage.readSelectedTeamId() != expectedTeamId ||
                    tokenStorage.readSelectedSiteId() != expectedSiteId
                ) {
                    return@withLock false
                }
                currentCoroutineContext().ensureActive()
                withContext(ioDispatcher) {
                    synchronized(selectedTeamLock) {
                        tokenStorage.clearSelectedTeamAndSiteIds()
                        selectedTeamSelectionVersion += 1L
                        selectedTeamValidatedGeneration = expectedSnapshot.generation
                    }
                }
                true
            }

        /** 세션 전환과 경쟁하지 않도록 현재 Team 세션에서만 현장 선택을 저장한다. */
        suspend fun saveSelectedSiteIdForSession(
            siteId: String,
            expectedSnapshot: SessionSnapshot,
        ): Boolean =
            sessionMutex.withLock {
                if (getSnapshot() != expectedSnapshot || expectedSnapshot.role != SessionRole.TEAM_MEMBER) {
                    return@withLock false
                }
                currentCoroutineContext().ensureActive()
                withContext(ioDispatcher) {
                    tokenStorage.saveSelectedSiteId(siteId)
                }
                true
            }

        suspend fun clearSelectedSiteIdForSession(
            expectedSnapshot: SessionSnapshot,
            expectedSiteId: String,
        ): Boolean =
            sessionMutex.withLock {
                if (getSnapshot() != expectedSnapshot || tokenStorage.readSelectedSiteId() != expectedSiteId) {
                    return@withLock false
                }
                currentCoroutineContext().ensureActive()
                withContext(ioDispatcher) {
                    tokenStorage.clearSelectedSiteId()
                }
                true
            }

        /** 현재 인증 세션이 앱 시작 때 저장 토큰으로 복원된 세션인지다. 새 로그인 세션이면 false다. */
        fun isRestoredSession(): Boolean =
            mutableSessionState.value == SessionState.Authenticated &&
                restoredGeneration == getSnapshot().generation

        fun isStartupCheckDone(generation: Long): Boolean = startupCheckedGeneration == generation

        fun markStartupCheckDone(generation: Long) {
            startupCheckedGeneration = generation
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
                        val validAccessToken =
                            storedAccessToken?.takeIf {
                                it.isNotBlank() && it != LEGACY_PLACEHOLDER_ACCESS_TOKEN
                            }
                        if (storedTokenRead.isUnreadable || (storedAccessToken != null && validAccessToken == null)) {
                            clearStoredTokenSafely()
                        }
                        if (validAccessToken == null) {
                            clearSessionScopedData()
                        }
                        val sessionIdentifier = validAccessToken?.let { getOrCreateSessionIdentifier() }
                        val initializedGeneration = getSnapshot().generation + 1L
                        synchronized(selectedTeamLock) {
                            mutableSessionSnapshot.value =
                                SessionSnapshot(
                                    generation = initializedGeneration,
                                    accessToken = validAccessToken,
                                    role =
                                        if (validAccessToken == null) {
                                            null
                                        } else {
                                            tokenStorage.readSessionRole() ?: SessionRole.TEAM_MEMBER
                                        },
                                    sessionIdentifier = sessionIdentifier,
                                )
                            restoredGeneration = initializedGeneration.takeIf { validAccessToken != null }
                            selectedTeamValidatedGeneration = null
                            selectedTeamSelectionVersion += 1L
                        }
                        if (sessionIdentifier != null) {
                            pruneSessionDataAfterRestore(sessionIdentifier)
                        }
                        mutableSessionState.value =
                            if (validAccessToken == null) {
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
                    synchronized(selectedTeamLock) {
                        tokenStorage.saveSession(sessionToken.accessToken, sessionToken.role)
                        val sessionIdentifier = getOrCreateSessionIdentifier()
                        mutableSessionSnapshot.value =
                            SessionSnapshot(
                                generation = getSnapshot().generation + 1L,
                                accessToken = sessionToken.accessToken,
                                role = sessionToken.role,
                                sessionIdentifier = sessionIdentifier,
                            )
                        mutableSessionState.value = SessionState.Authenticated
                        selectedTeamValidatedGeneration = null
                        selectedTeamSelectionVersion += 1L
                    }
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
                    synchronized(selectedTeamLock) {
                        mutableSessionSnapshot.value =
                            getSnapshot().copy(
                                accessToken = null,
                            )
                        selectedTeamValidatedGeneration = null
                        selectedTeamSelectionVersion += 1L
                    }
                    try {
                        clearSessionScopedData()
                    } finally {
                        synchronized(selectedTeamLock) {
                            clearStoredTokenSafely()
                        }
                    }
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
        suspend fun expireSession(expectedGeneration: Long = getSnapshot().generation): Boolean =
            sessionMutex.withLock {
                clearSession(expectedGeneration)
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
                synchronized(selectedTeamLock) {
                    mutableSessionSnapshot.value =
                        SessionSnapshot(
                            generation = getSnapshot().generation + 1L,
                            accessToken = null,
                            role = null,
                            sessionIdentifier = null,
                        )
                    selectedTeamValidatedGeneration = null
                    selectedTeamSelectionVersion += 1L
                }
                try {
                    clearSessionScopedData()
                } finally {
                    synchronized(selectedTeamLock) {
                        mutableSessionState.value = SessionState.Unauthenticated
                        clearStoredTokenSafely()
                    }
                    isInitialized = true
                    mutableSessionEvents.tryEmit(SessionEvent.Expired)
                }
            }
            return true
        }

        private suspend fun clearSessionScopedData() {
            sessionScopedDataCleaners.forEach { cleaner ->
                try {
                    cleaner.clearSessionData()
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to clear session-scoped data", exception)
                }
            }
        }

        private suspend fun pruneSessionDataAfterRestore(sessionIdentifier: String) {
            sessionScopedDataCleaners.forEach { cleaner ->
                try {
                    cleaner.onSessionRestored(sessionIdentifier)
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (exception: Exception) {
                    Log.w(TAG, "Failed to prune restored session data", exception)
                }
            }
        }

        private fun getOrCreateSessionIdentifier(): String {
            val storedIdentifier =
                try {
                    tokenStorage.readSessionIdentifier()?.takeIf(String::isNotBlank)
                } catch (exception: Exception) {
                    Log.w(TAG, "Unable to read session identifier", exception)
                    null
                }
            if (storedIdentifier != null) return storedIdentifier

            val newIdentifier = UUID.randomUUID().toString()
            try {
                tokenStorage.saveSessionIdentifier(newIdentifier)
            } catch (exception: Exception) {
                Log.w(TAG, "Unable to persist session identifier", exception)
            }
            return newIdentifier
        }

        private fun readStoredAccessToken(): StoredTokenRead =
            try {
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
            const val TAG = "SessionManager"
            const val LEGACY_PLACEHOLDER_ACCESS_TOKEN = "placeholder-access-token"
        }
    }
