package com.nativelap.heartguard.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.CancellationException

class ApiExecutorTest {
    @Test
    fun cancellationIsNotConvertedToAnApiFailure() =
        runTest {
            val expectedCancellation = CancellationException("Request cancelled")
            val executor = ApiExecutor()
            val actualCancellation =
                try {
                    executor.execute<String> {
                        throw expectedCancellation
                    }
                    null
                } catch (cancellationException: CancellationException) {
                    cancellationException
                }

            assertSame(expectedCancellation, actualCancellation)
        }
}
