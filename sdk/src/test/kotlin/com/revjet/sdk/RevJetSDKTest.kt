package com.revjet.sdk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevJetSDKTest {
    @After
    fun tearDown() {
        RevJetSDK.setCOPPACompliance(false)
        RevJetSDK.setDebugEnabled(false)
    }

    @Test
    fun `the SDK reports its version`() {
        assertEquals("2.1.0", RevJetSDK.version)
    }

    @Test
    fun `COPPA compliance is off until it is asked for`() {
        assertFalse(RevJetSDK.isCOPPACompliant)

        RevJetSDK.setCOPPACompliance(true)
        assertTrue(RevJetSDK.isCOPPACompliant)
    }
}
