package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingPageTest {
    private val link = "https://ads.revjet.com/click/abc?x=\$\$CX\$\$&y=\$\$CY\$\$"

    @Test
    fun `the tap is filled in where the ad server asks for it`() {
        assertEquals(
            "https://ads.revjet.com/click/abc?x=120&y=45",
            LandingPage.url(link, tapLocation = 120.7f to 45.2f, parameters = emptyList()),
        )
    }

    @Test
    fun `without a tap the placeholders are left for the ad server`() {
        assertEquals(link, LandingPage.url(link, tapLocation = null, parameters = emptyList()))
    }

    @Test
    fun `parameters are appended to the query, encoded`() {
        assertEquals(
            "https://ads.revjet.com/click/abc?a=1&lp=https%3A%2F%2Fexample.com%2F%3Fq%3D1",
            LandingPage.url(
                "https://ads.revjet.com/click/abc?a=1",
                tapLocation = null,
                parameters = listOf("lp" to "https://example.com/?q=1"),
            ),
        )
        assertTrue(
            LandingPage
                .url("https://ads.revjet.com/click/abc", tapLocation = null, parameters = listOf("prm_tag" to "e1"))
                .endsWith("/click/abc?prm_tag=e1"),
        )
    }

    @Test(expected = RevJetError.InvalidLPFormat::class)
    fun `a link that is not a URL is refused`() {
        LandingPage.url("https://ads.revjet.com/click/a b", tapLocation = null, parameters = emptyList())
    }
}
