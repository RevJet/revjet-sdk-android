package com.revjet.sdk

/** How a creative's width or height is determined. */
public enum class ResponsiveDimension(
    internal val value: String,
) {
    /** Predefined, and independent of the container. */
    FIXED("fixed"),

    /** Follows the creative's content. */
    DYNAMIC("dynamic"),

    /** Follows the container. */
    FIT("fit"),
}
