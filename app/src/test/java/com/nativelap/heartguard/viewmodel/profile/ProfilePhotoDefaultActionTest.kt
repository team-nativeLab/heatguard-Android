package com.nativelap.heartguard.viewmodel.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfilePhotoDefaultActionTest {
    @Test
    fun `server photo exists asks for deletion confirmation`() {
        val action =
            resolveProfilePhotoDefaultAction(
                hasServerPhoto = true,
                hasSelectedPhoto = false,
            )

        assertEquals(ProfileDefaultPhotoAction.ConfirmDeletion, action)
    }

    @Test
    fun `server photo takes priority over unsaved selection`() {
        val action =
            resolveProfilePhotoDefaultAction(
                hasServerPhoto = true,
                hasSelectedPhoto = true,
            )

        assertEquals(ProfileDefaultPhotoAction.ConfirmDeletion, action)
    }

    @Test
    fun `only unsaved selection exists clears the selection without a request`() {
        val action =
            resolveProfilePhotoDefaultAction(
                hasServerPhoto = false,
                hasSelectedPhoto = true,
            )

        assertEquals(ProfileDefaultPhotoAction.ClearSelection, action)
    }

    @Test
    fun `no photo exists only closes the sheet`() {
        val action =
            resolveProfilePhotoDefaultAction(
                hasServerPhoto = false,
                hasSelectedPhoto = false,
            )

        assertEquals(ProfileDefaultPhotoAction.None, action)
    }
}
