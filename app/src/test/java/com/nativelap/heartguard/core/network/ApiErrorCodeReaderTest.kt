package com.nativelap.heartguard.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorCodeReaderTest {
    @Test
    fun readsErrorCodeFromCommonErrorEnvelope() {
        val errorBody = """
            {"success":false,"data":null,"error":{"code":"INVALID_CREDENTIALS","message":"비밀번호 불일치"},"meta":{"requestId":"req_01"}}
        """.trimIndent()

        assertEquals("INVALID_CREDENTIALS", ApiErrorCodeReader.read(errorBody))
    }

    @Test
    fun returnsNullForNonJsonBody() {
        assertNull(ApiErrorCodeReader.read("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun returnsNullForBlankBody() {
        assertNull(ApiErrorCodeReader.read(""))
    }

    @Test
    fun returnsNullWhenErrorFieldIsMissing() {
        assertNull(ApiErrorCodeReader.read("""{"success":false}"""))
    }
}
