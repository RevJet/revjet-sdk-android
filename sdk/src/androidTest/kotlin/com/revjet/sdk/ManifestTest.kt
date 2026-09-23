package com.revjet.sdk

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** What the SDK's manifest adds to an application that includes it. */
@RunWith(AndroidJUnit4::class)
class ManifestTest {
    private val permissions: List<String> by lazy {
        val context = InstrumentationRegistry.getInstrumentation().context
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        info.requestedPermissions?.toList().orEmpty()
    }

    @Test
    fun the_sdk_asks_for_what_it_needs() {
        assertTrue("requesting ads needs the network: $permissions", "android.permission.INTERNET" in permissions)
        assertTrue(
            "the advertising identifier needs its permission from Android 13: $permissions",
            "com.google.android.gms.permission.AD_ID" in permissions,
        )
        assertTrue(
            "`reloadsOnReachable` watches the network: $permissions",
            "android.permission.ACCESS_NETWORK_STATE" in permissions,
        )
    }

    @Test
    fun the_sdk_cannot_reach_the_local_network() {
        // Android 17 gates private and loopback addresses behind this permission. The SDK refuses
        // such destinations itself, and not asking for it means it cannot be granted either
        assertFalse(
            "the SDK must not let an app reach local addresses on its behalf: $permissions",
            "android.permission.ACCESS_LOCAL_NETWORK" in permissions,
        )
    }
}
