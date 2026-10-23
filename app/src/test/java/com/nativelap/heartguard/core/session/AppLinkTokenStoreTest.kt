package com.nativelap.heartguard.core.session

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLinkTokenStoreTest {
    @Test
    fun acceptsTokenFromSupportedAppLink() {
        val store = AppLinkTokenStore()

        store.accept("https://heatguard-temp.https.gsmsv.site/api/v1/app/links/link-token-123")

        assertEquals("link-token-123", store.pendingLink.value?.token)
    }

    @Test
    fun rejectsUnsupportedLinkComponents() {
        val invalidLinks =
            listOf(
                "http://heatguard-temp.https.gsmsv.site/api/v1/app/links/token",
                "https://example.com/api/v1/app/links/token",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/other/token",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/token/extra",
                "https://heatguard-temp.https.gsmsv.site:443/api/v1/app/links/token",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/token?source=email",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/token#claim",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/token%2Fextra",
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/token%ZZ",
            )

        invalidLinks.forEach { invalidLink ->
            val store = AppLinkTokenStore()

            store.accept(invalidLink)

            assertEquals(null, store.pendingLink.value)
        }
    }

    @Test
    fun decodesTokenAndConsumesOnlyMatchingValue() {
        val store = AppLinkTokenStore()

        store.accept("https://heatguard-temp.https.gsmsv.site/api/v1/app/links/link%2Dtoken")
        val acceptedLink = requireNotNull(store.pendingLink.value)
        store.consume(PendingAppLink("different-token", acceptedLink.linkId))

        assertEquals("link-token", store.pendingLink.value?.token)

        store.consume(acceptedLink)

        assertEquals(null, store.pendingLink.value)
    }

    @Test
    fun invalidLinkDoesNotReplacePendingToken() {
        val store = AppLinkTokenStore()
        store.accept("https://heatguard-temp.https.gsmsv.site/api/v1/app/links/valid-token")

        store.accept("https://example.com/api/v1/app/links/invalid-token")

        assertEquals("valid-token", store.pendingLink.value?.token)
    }

    @Test
    fun staleLinkCannotConsumeAReopenedLinkWithSameToken() {
        val store = AppLinkTokenStore()
        val link = "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/same-token"

        store.accept(link)
        val firstLink = requireNotNull(store.pendingLink.value)
        store.accept(link)
        val reopenedLink = requireNotNull(store.pendingLink.value)

        store.consume(firstLink)

        assertEquals(reopenedLink, store.pendingLink.value)
    }

    @Test
    fun pendingLinkStringDoesNotExposeToken() {
        val pendingLink = PendingAppLink("sensitive-token", 1L)

        assertEquals("PendingAppLink(linkId=1)", pendingLink.toString())
    }
}
