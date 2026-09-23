package com.revjet.sdk

import com.revjet.sdk.internal.JsLiteral
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Options reach a URL and a JavaScript object, and each wants a different rendering of the same
 * value, so both renderings are pinned here.
 */
class OptionEncodingTest {
    @Test
    fun `option values are sent unquoted in a URL`() {
        assertEquals("gold", OptionValue.of("gold").stringValue)
        assertEquals("7", OptionValue.of(7).stringValue)
        assertEquals("1.5", OptionValue.of(1.5).stringValue)
        assertEquals("true", OptionValue.of(true).stringValue)
    }

    @Test
    fun `only strings are quoted in a JavaScript literal`() {
        assertEquals("'gold'", OptionValue.of("gold").jsLiteral)
        assertEquals("7", OptionValue.of(7).jsLiteral)
        assertEquals("1.5", OptionValue.of(1.5).jsLiteral)
        assertEquals("true", OptionValue.of(true).jsLiteral)
    }

    @Test
    fun `a quote inside a custom value is escaped for JavaScript`() {
        assertEquals(JsLiteral.string("it's"), OptionValue.of("it's").jsLiteral)
    }

    @Test
    fun `options carry the parameter names the ad server expects`() {
        assertEquals(
            listOf("_imp_banner_size" to "300x250"),
            Option.ImpBannerSize(300, 250).queryItems.map { it.first to it.second.stringValue },
        )
        assertEquals(
            listOf("autoscale" to "true", "autoscale_mode" to "best-fit"),
            Option.Autoscale(true, ScaleMode.BEST_FIT).queryItems.map { it.first to it.second.stringValue },
        )
        assertEquals(
            listOf("delivery_method" to "banner"),
            Option.Delivery(DeliveryMethod.BANNER).queryItems.map { it.first to it.second.stringValue },
        )
        assertEquals(
            listOf("responsive_height" to "dynamic"),
            Option.ResponsiveHeight(ResponsiveDimension.DYNAMIC).queryItems.map { it.first to it.second.stringValue },
        )
        assertEquals(
            listOf("adaptive_sizes" to "300x250,320x50"),
            Option
                .AdaptiveSizes(listOf("300x250", "320x50"))
                .queryItems
                .map { it.first to it.second.stringValue },
        )
        assertEquals(
            listOf("no_session" to "false"),
            Option.NoSession(false).queryItems.map { it.first to it.second.stringValue },
        )
    }

    @Test
    fun `autoscale defaults to fitting the width`() {
        assertEquals(ScaleMode.FIT_TO_WIDTH, Option.Autoscale(true).mode)
    }

    @Test
    fun `custom options keep their own names and types`() {
        val items =
            Option
                .Custom(
                    mapOf("tier" to OptionValue.of("gold"), "score" to OptionValue.of(7)),
                ).queryItems
                .toMap()

        assertEquals("gold", items["tier"]?.stringValue)
        assertEquals("'gold'", items["tier"]?.jsLiteral)
        assertEquals("7", items["score"]?.stringValue)
        assertEquals("7", items["score"]?.jsLiteral)
    }

    @Test
    fun `the domain comes from the options, or falls back to the default`() {
        assertEquals(Constants.ADS_DOMAIN, emptyList<Option>().domain)
        assertEquals("ads.example.com", listOf(Option.CustomDomain("ads.example.com")).domain)
    }
}
