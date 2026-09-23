package com.revjet.sdk

/** Keeps test runs out of production statistics. Only for development. */
public enum class DebugMode(
    internal val value: String,
) {
    EMULATE("emulate"),
}
