package com.revjet.sdk

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Checks [condition] until it holds or [timeoutMs] passes.
 *
 * For state the SDK changes asynchronously, where no callback marks the moment it is reached.
 */
internal fun awaitUntil(
    timeoutMs: Long = 20_000,
    intervalMs: Long = 100,
    condition: () -> Boolean,
): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs

    while (System.currentTimeMillis() < deadline) {
        if (condition()) return true
        Thread.sleep(intervalMs)
    }

    return condition()
}

/** Runs [block] on the main thread, which the SDK's state is confined to, and waits for it. */
internal fun onMain(block: () -> Unit) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
}
