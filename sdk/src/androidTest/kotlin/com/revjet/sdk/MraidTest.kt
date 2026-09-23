package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The MRAID surface the SDK exposes to a creative, driven against the live test tag. */
@RunWith(AndroidJUnit4::class)
class MraidTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var host: WebTagHost

    /** Records what an ad registering at document start observes. */
    private val recorder =
        """
        window.__revjetReadyState = 'not fired';
        (function poll() {
            if (window.mraid) {
                window.mraid.addEventListener('ready', function() {
                    window.__revjetReadyState = window.mraid.getState();
                });
            } else {
                setTimeout(poll, 0);
            }
        })();
        """.trimIndent()

    @Before
    fun setUp() {
        WebViewFeatures.assumeAdsCanBeHosted()
        RevJetSDK.setDebugEnabled(true)
        host =
            WebTagHost(
                Tag(
                    context = context,
                    type = TagType.WEB_BASED,
                    tag = "tag390451",
                    key = "7c2",
                    plcId = "266512461",
                    debugMode = DebugMode.EMULATE,
                ),
            )
        host.mountAndLoad(recorder)
        assertTrue("MRAID should initialize", host.awaitReady())
    }

    @After
    fun tearDown() {
        // The set-up is skipped where the WebView cannot host an ad at all
        if (::host.isInitialized) host.close()
        RevJetSDK.setDebugEnabled(false)
    }

    @Test
    fun ready_is_fired_once_the_tag_has_left_the_loading_state() {
        assertTrue(
            "an ad registering for ready before the host fires it must not see loading",
            host.evaluateUntil("window.__revjetReadyState", "default"),
        )
    }

    @Test
    fun mraid_provides_the_methods_our_ads_call() {
        val methods =
            listOf(
                "getVersion",
                "getState",
                "getPlacementType",
                "isViewable",
                "getCurrentAppOrientation",
                "open",
                "close",
                "useCustomClose",
                "setSupports",
                "setScreenSize",
                "addEventListener",
                "removeEventListener",
                "getExpandProperties",
                "setExpandProperties",
                "getOrientationProperties",
                "setOrientationProperties",
                "getScreenSize",
                "getMaxSize",
                "getCurrentPosition",
                "getDefaultPosition",
                "supports",
            )

        val missing =
            host.evaluate(
                "${WebTagHost.jsArray(
                    methods,
                )}.filter(function(m) { return typeof window.mraid[m] !== 'function' }).join(',')",
            )

        assertEquals("every method our ads call should be implemented", "", missing)
    }

    @Test
    fun the_placement_is_inline_and_the_version_is_three() {
        assertEquals("inline", host.evaluate("window.mraid.getPlacementType()"))
        assertEquals("3.0", host.evaluate("window.mraid.getVersion()"))
    }

    @Test
    fun the_environment_reports_the_sdk() {
        assertEquals("RevJetSDK-Android", host.evaluate("window.MRAID_ENV.sdk"))
        assertEquals(RevJetSDK.version, host.evaluate("window.MRAID_ENV.sdkVersion"))
        assertEquals(context.packageName, host.evaluate("window.MRAID_ENV.appId"))
    }

    @Test
    fun expand_properties_default_to_the_screen_size_and_are_stored() {
        assertTrue(
            "unset sizes report the screen size",
            host.evaluate("window.mraid.getExpandProperties().width").toInt() > 0,
        )
        assertEquals("true", host.evaluate("window.mraid.getExpandProperties().isModal"))

        val stored =
            host.evaluate(
                """
                (function() {
                  window.mraid.setExpandProperties({ width: 100, height: 200, useCustomClose: true, isModal: false });
                  var p = window.mraid.getExpandProperties();
                  return [p.width, p.height, p.useCustomClose, p.isModal].join(',');
                })()
                """.trimIndent(),
            )

        assertEquals("isModal stays read only", "100,200,true,true", stored)
    }

    @Test
    fun orientation_properties_are_stored() {
        val stored =
            host.evaluate(
                """
                (function() {
                  window.mraid.setOrientationProperties({ allowOrientationChange: false, forceOrientation: 'landscape' });
                  return JSON.stringify(window.mraid.getOrientationProperties());
                })()
                """.trimIndent(),
            )

        assertEquals("""{"allowOrientationChange":false,"forceOrientation":"landscape"}""", stored)
    }

    @Test
    fun supports_reports_what_the_container_implements() {
        val unsupported =
            host.evaluate(
                """
                ['calendar', 'storePicture', 'vpaid', 'location', 'notAFeature']
                  .filter(function(feature) { return window.mraid.supports(feature) })
                  .join(',')
                """.trimIndent(),
            )

        assertEquals("features this container does not implement report false", "", unsupported)
        assertEquals("true", host.evaluate("window.mraid.supports('inlineVideo')"))
    }

    @Test
    fun unsupported_methods_report_an_error_instead_of_being_absent() {
        val unsupported =
            listOf(
                "expand",
                "resize",
                "unload",
                "playVideo",
                "storePicture",
                "createCalendarEvent",
                "setResizeProperties",
                "getResizeProperties",
                "getLocation",
            )

        val reported =
            host.evaluate(
                """
                (function() {
                  var actions = [];
                  window.mraid.addEventListener('error', function(message, action) { actions.push(action) });
                  ${WebTagHost.jsArray(unsupported)}.forEach(function(name) { window.mraid[name]() });
                  return actions.join(',');
                })()
                """.trimIndent(),
            )

        assertEquals(
            "calling an unsupported method should raise an MRAID error, not a TypeError",
            unsupported.joinToString(","),
            reported,
        )
        assertEquals("-1", host.evaluate("window.mraid.getLocation()"))
    }

    @Test
    fun the_container_geometry_is_reported() {
        assertTrue(
            "the ad fills the host it was mounted in",
            host.evaluateUntil(
                "(function() { var p = window.mraid.getCurrentPosition(); return [p.x, p.y].join(',') })()",
                "0,0",
            ),
        )
        assertTrue(host.evaluate("window.mraid.getCurrentPosition().width").toInt() > 0)
        assertTrue(host.evaluate("window.mraid.getMaxSize().height").toInt() > 0)
        assertEquals("portrait", host.evaluate("window.mraid.getCurrentAppOrientation().orientation"))
    }
}
