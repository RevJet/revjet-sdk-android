package com.revjet.sdk

/** How a creative is scaled to its container. */
public enum class ScaleMode(
    internal val value: String,
) {
    /** Fits entirely within the container, keeping its aspect ratio. */
    BEST_FIT("best-fit"),

    /** Fits the container's width, with the height following. */
    FIT_TO_WIDTH("fit-to-width"),

    /** Fits the container's height, with the width following. */
    FIT_TO_HEIGHT("fit-to-height"),
}
