package com.nativelap.heartguard.data.site.local

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class WeatherCacheLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}

internal fun isOwnedByCurrentSession(
    storedOwner: String,
    currentSessionIdentifier: String?,
): Boolean =
    currentSessionIdentifier == storedOwner

internal suspend fun <T> runCacheOperationSafely(
    onFailure: (Exception) -> Unit,
    operation: suspend () -> T,
): T? =
    try {
        operation()
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (exception: Exception) {
        onFailure(exception)
        null
    }
