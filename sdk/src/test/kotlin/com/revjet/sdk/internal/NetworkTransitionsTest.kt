package com.revjet.sdk.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTransitionsTest {
    @Test
    fun `the network coming back is a restoration`() {
        assertTrue(NetworkTransitions(isAvailable = false).update(isAvailable = true))
    }

    @Test
    fun `a network that was there all along is not`() {
        // what registering with a network up reports first
        assertFalse(NetworkTransitions(isAvailable = true).update(isAvailable = true))
    }

    @Test
    fun `switching networks without losing one is not`() {
        val transitions = NetworkTransitions(isAvailable = true)

        assertFalse("Wi-Fi to mobile data", transitions.update(isAvailable = true))
    }

    @Test
    fun `losing the network and getting it back is, each time`() {
        val transitions = NetworkTransitions(isAvailable = true)

        assertFalse(transitions.update(isAvailable = false))
        assertTrue(transitions.update(isAvailable = true))
        assertFalse(transitions.update(isAvailable = false))
        assertTrue(transitions.update(isAvailable = true))
    }
}
