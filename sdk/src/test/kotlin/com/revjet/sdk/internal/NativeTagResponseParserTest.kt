package com.revjet.sdk.internal

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeTagResponseParserTest {
    /** Shaped after a live response from `tag355022`. */
    private val payload =
        """
        {
          "version": "1.0.0",
          "data": { "layout": { "id": "l1" }, "personalization": [ { "id": "el1", "value": "Buy" } ] },
          "preview": false,
          "link": "https://ads.revjet.com/click/tag355022/855/1?vid=541",
          "width": "300px",
          "height": "250px",
          "context": { "crv": "crv240442", "eg": "eg71806", "tag": "tag355022", "adt": "8558528480160856302" },
          "tracking": [
            { "id": 999, "type": "ad_load_start", "pixels": ["https://pix.revjet.com/interaction/999"] },
            { "id": 1000, "type": "ad_clicked", "pixels": ["https://pix.revjet.com/interaction/1000"] },
            { "id": 1001, "type": "something_new", "pixels": ["https://pix.revjet.com/interaction/1001"] }
          ]
        }
        """.trimIndent()

    @Test
    fun `reads the fields the SDK and the application need`() {
        val response = NativeTagResponseParser.parse(payload)

        assertEquals("1.0.0", response.version)
        assertEquals("300px", response.width)
        assertEquals("250px", response.height)
        assertEquals("https://ads.revjet.com/click/tag355022/855/1?vid=541", response.linkValue)
        assertEquals("crv240442", response.context.crv)
        assertEquals("tag355022", response.context.tag)
    }

    @Test
    fun `hands the personalization payload over as JSON text`() {
        val response = NativeTagResponseParser.parse(payload)

        assertEquals(
            "Buy",
            JSONObject(response.data).getJSONArray("personalization").getJSONObject(0).getString("value"),
        )
    }

    @Test
    fun `keeps the tracking it understands and drops what it does not`() {
        val response = NativeTagResponseParser.parse(payload)

        assertEquals(
            listOf(VisibilityEvent.AD_LOAD_START, VisibilityEvent.AD_CLICKED),
            response.tracking.map { it.type },
        )
        assertEquals(999, response.tracking.first().id)
        assertEquals(listOf("https://pix.revjet.com/interaction/999"), response.tracking.first().pixels)
    }

    @Test(expected = org.json.JSONException::class)
    fun `a response missing a field the SDK needs is rejected`() {
        NativeTagResponseParser.parse("""{ "version": "1.0.0" }""")
    }

    @Test
    fun `every event the ad server sends is understood`() {
        val serverEvents =
            listOf(
                "ad_load_start",
                "ad_loaded",
                "ad_clicked",
                "heatmap_pixel",
                "ad_viewable_shown",
                "ad_viewable",
                "ad_viewable_first_quartile",
                "ad_viewable_midpoint",
                "ad_viewable_third_quartile",
                "ad_viewable_complete",
                "ad_invisible",
            )

        assertTrue(serverEvents.all { VisibilityEvent.from(it) != null })
    }
}
