package com.nativelap.heartguard.data.checklist.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistPathEncodingTest {
    @Test
    fun `encodes colon in round path segment`() {
        assertEquals("08%3A00", "08:00".encodeChecklistPathSegment())
    }

    @Test
    fun `encodes slash to keep identifiers in one path segment`() {
        assertEquals("item%2F01", "item/01".encodeChecklistPathSegment())
    }
}
