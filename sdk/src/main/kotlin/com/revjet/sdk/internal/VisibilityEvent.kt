package com.revjet.sdk.internal

/** A tracking event the ad server defines pixels for. */
internal enum class VisibilityEvent(
    val value: String,
) {
    AD_LOAD_START("ad_load_start"),
    AD_LOADED("ad_loaded"),
    AD_CLICKED("ad_clicked"),
    HEATMAP_PIXEL("heatmap_pixel"),
    AD_VIEWABLE_SHOWN("ad_viewable_shown"),
    AD_VIEWABLE("ad_viewable"),
    AD_INVISIBLE("ad_invisible"),
    FIRST_QUARTILE("ad_viewable_first_quartile"),
    MIDPOINT("ad_viewable_midpoint"),
    THIRD_QUARTILE("ad_viewable_third_quartile"),
    COMPLETE("ad_viewable_complete"),
    ;

    companion object {
        fun from(value: String): VisibilityEvent? = entries.firstOrNull { it.value == value }
    }
}
