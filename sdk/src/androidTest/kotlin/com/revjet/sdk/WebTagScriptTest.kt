package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revjet.sdk.internal.WebBasedTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The document handed to the web view.
 *
 * An empty option set must leave no dangling comma, and custom option values must render as
 * JavaScript literals rather than through the language's own `toString`.
 */
@RunWith(AndroidJUnit4::class)
class WebTagScriptTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun script(
        options: List<Option> = emptyList(),
        customParameters: Map<String, String> = emptyMap(),
        debugMode: DebugMode? = null,
    ): String {
        val tag =
            Tag(
                context = context,
                type = TagType.WEB_BASED,
                tag = "tag390451",
                key = "7c2",
                plcId = "266512461",
                debugMode = debugMode,
                options = options,
                customParameters = customParameters,
            )

        return (tag.model as WebBasedTag).script()
    }

    @Test
    fun a_tag_without_options_produces_no_empty_entry() {
        val script = script()

        assertFalse(
            "an empty set of options must not leave a dangling comma: $script",
            Regex(",\\s*,").containsMatchIn(script),
        )
        assertTrue(script.contains("_tag: 'tag390451'"))
        assertTrue(script.contains("_plc_id: '266512461'"))
        assertFalse("debug is left out when it is not set", script.contains("debug:"))
    }

    @Test
    fun option_values_are_rendered_as_javascript() {
        val script =
            script(
                options =
                    listOf(
                        Option.Autoscale(true, ScaleMode.BEST_FIT),
                        Option.Custom(mapOf("persona" to OptionValue.of("it's me"), "score" to OptionValue.of(7))),
                    ),
                debugMode = DebugMode.EMULATE,
            )

        assertTrue("booleans stay unquoted", script.contains("'autoscale': true,"))
        assertTrue("strings are quoted", script.contains("'autoscale_mode': 'best-fit',"))
        assertTrue("numbers stay unquoted", script.contains("'score': 7,"))
        assertTrue("quotes inside a value are escaped", script.contains("""'persona': 'it\'s me',"""))
        assertTrue(script.contains("debug: 'emulate',"))
    }

    @Test
    fun custom_parameters_are_passed_to_the_tag() {
        val script = script(customParameters = mapOf("_bp_connector_url" to "ted-baker-london"))

        assertTrue(script.contains("'_bp_connector_url': 'ted-baker-london'"))
    }

    @Test
    fun a_custom_domain_serves_the_tag_and_is_reported_to_it() {
        val script = script(options = listOf(Option.CustomDomain("ads.example.com")))

        assertTrue(script.contains("https://ads.example.com/bg"))
        assertTrue(script.contains("custom_domain: 'ads.example.com',"))
    }
}
