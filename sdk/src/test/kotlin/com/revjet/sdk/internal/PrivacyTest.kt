package com.revjet.sdk.internal

import com.revjet.sdk.RevJetSDK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the SDK reports about the device, and what COPPA withholds. */
class PrivacyTest {
    @After
    fun tearDown() {
        RevJetSDK.setCOPPACompliance(false)
    }

    @Test
    fun `the identifier read from the device is reported as it is`() {
        val identity = Identity.of("com.example.app", "abcd-1234", isLimitAdTrackingEnabled = false)

        assertEquals("abcd-1234", identity.advertisingId)
        assertFalse(identity.isAdTrackingLimited)
        assertEquals("RevJetSDK-Android", identity.sdkName)
    }

    @Test
    fun `an identifier of zeros is no identifier`() {
        val identity =
            Identity.of("com.example.app", "00000000-0000-0000-0000-000000000000", isLimitAdTrackingEnabled = false)

        assertEquals("", identity.advertisingId)
        assertTrue("there is no identifier to track with", identity.isAdTrackingLimited)
    }

    @Test
    fun `a device without an identifier limits tracking`() {
        assertTrue(Identity.of("com.example.app", "", isLimitAdTrackingEnabled = false).isAdTrackingLimited)
    }

    @Test
    fun `limited tracking on the device is reported`() {
        val identity = Identity.of("com.example.app", "abcd-1234", isLimitAdTrackingEnabled = true)

        assertTrue(identity.isAdTrackingLimited)
    }

    @Test
    fun `no identifier is reported under COPPA`() {
        RevJetSDK.setCOPPACompliance(true)

        val identity = Identity.of("com.example.app", "abcd-1234", isLimitAdTrackingEnabled = false)

        assertEquals("", identity.advertisingId)
        assertTrue("a child-directed app never tracks", identity.isAdTrackingLimited)
        assertEquals("the app is still identified", "com.example.app", identity.appId)
    }

    @Test
    fun `a request made under COPPA carries no identifier`() {
        RevJetSDK.setCOPPACompliance(true)
        val identity = Identity.of("com.example.app", "abcd-1234", isLimitAdTrackingEnabled = false)

        val url =
            AdRequest.nativeUrl(
                tag = "tag355022",
                key = "b37",
                plcId = null,
                debugMode = null,
                options = emptyList(),
                customParameters = emptyMap(),
                identity = identity,
            )

        assertFalse(url.contains("_js_ifa"))
        assertTrue(url.contains("_js_app_id=com.example.app"))
    }
}
