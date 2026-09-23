package com.revjet.sdk.internal

import com.revjet.sdk.DebugMode
import com.revjet.sdk.DeliveryMethod
import com.revjet.sdk.Option
import com.revjet.sdk.OptionValue
import com.revjet.sdk.RevJetError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class AdRequestTest {
    private val identity =
        Identity(
            sdkName = "RevJetSDK-Android",
            appId = "com.example.app",
            advertisingId = "abcd-1234",
            isAdTrackingLimited = false,
        )

    private fun url(
        plcId: String? = null,
        debugMode: DebugMode? = null,
        options: List<Option> = emptyList(),
        customParameters: Map<String, String> = emptyMap(),
        identity: Identity = this.identity,
    ) = AdRequest.nativeUrl(
        tag = "tag355022",
        key = "b37",
        plcId = plcId,
        debugMode = debugMode,
        options = options,
        customParameters = customParameters,
        identity = identity,
    )

    private fun query(url: String): Map<String, String> =
        url
            .substringAfter("?")
            .split("&")
            .associate { parameter ->
                val name = parameter.substringBefore("=")
                val value = parameter.substringAfter("=", "")
                URLDecoder.decode(name, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }

    @Test
    fun `the tag is the path of the ad server's URL`() {
        assertTrue(url().startsWith("https://ads.revjet.com/tag355022?"))
    }

    @Test
    fun `a custom domain replaces the ad server`() {
        val url = url(options = listOf(Option.CustomDomain("ads.example.com")))

        assertTrue(url.startsWith("https://ads.example.com/tag355022?"))
        assertFalse("the domain is not also a query parameter", query(url).containsKey("custom_domain"))
    }

    @Test
    fun `the request identifies the tag, the SDK and the app`() {
        val parameters = query(url(plcId = "266512461", debugMode = DebugMode.EMULATE))

        assertEquals("266512461", parameters["_plc_id"])
        assertEquals("b37", parameters["adkey"])
        assertEquals("emulate", parameters["debug"])
        assertEquals("RevJetSDK-Android", parameters["_js_sdk"])
        assertEquals("com.example.app", parameters["_js_app_id"])
        assertEquals("abcd-1234", parameters["_js_ifa"])
    }

    @Test
    fun `an absent placement is sent as an empty value, and debug is left out`() {
        val parameters = query(url())

        assertEquals("", parameters["_plc_id"])
        assertFalse(parameters.containsKey("debug"))
    }

    @Test
    fun `no identifier is sent when there is none to send`() {
        val parameters = query(url(identity = identity.copy(advertisingId = "")))

        assertFalse(parameters.containsKey("_js_ifa"))
        assertEquals("com.example.app", parameters["_js_app_id"])
    }

    @Test
    fun `option values are sent unquoted`() {
        val parameters =
            query(
                url(
                    options =
                        listOf(
                            Option.Delivery(DeliveryMethod.BANNER),
                            Option.ImpBannerSize(300, 250),
                            Option.NoSession(true),
                            Option.Custom(mapOf("persona" to OptionValue.of("vip"))),
                        ),
                ),
            )

        assertEquals("banner", parameters["delivery_method"])
        assertEquals("300x250", parameters["_imp_banner_size"])
        assertEquals("true", parameters["no_session"])
        assertEquals("vip", parameters["persona"])
    }

    @Test
    fun `custom parameters are sent, in a stable order`() {
        val url = url(customParameters = mapOf("zeta" to "last", "_bp_connector_url" to "ted-baker-london"))

        assertEquals("ted-baker-london", query(url)["_bp_connector_url"])
        assertTrue(url.indexOf("_bp_connector_url") < url.indexOf("zeta"))
    }

    @Test
    fun `values that would break the query are encoded`() {
        val parameters =
            query(
                url(customParameters = mapOf("q" to "a b&c=d/e", "u" to "ünïcode")),
            )

        assertEquals("a b&c=d/e", parameters["q"])
        assertEquals("ünïcode", parameters["u"])
    }

    @Test
    fun `the tag is one segment of the path, whatever it contains`() {
        val url = AdRequest.nativeUrl("my tag/2", "b37", null, null, emptyList(), emptyMap(), identity)

        assertTrue(url, url.startsWith("https://ads.revjet.com/my%20tag%2F2?"))
    }

    @Test
    fun `a request without a tag or key cannot be made`() {
        assertInvalid(tag = "")
        assertInvalid(tag = "   ")
        assertInvalid(key = "")
    }

    @Test
    fun `a custom domain has to be a host name`() {
        assertInvalid(options = listOf(Option.CustomDomain("ads example.com")))
        assertInvalid(options = listOf(Option.CustomDomain("ads.example.com/path")))
        assertInvalid(options = listOf(Option.CustomDomain("")))

        // a port is part of a host
        AdRequest.validate("tag355022", "b37", listOf(Option.CustomDomain("ads.example.com:8443")))
    }

    private fun assertInvalid(
        tag: String = "tag355022",
        key: String = "b37",
        options: List<Option> = emptyList(),
    ) {
        try {
            AdRequest.validate(tag, key, options)
        } catch (error: RevJetError.InvalidConfiguration) {
            return
        }

        throw AssertionError("tag '$tag', key '$key' and $options should be refused")
    }
}
