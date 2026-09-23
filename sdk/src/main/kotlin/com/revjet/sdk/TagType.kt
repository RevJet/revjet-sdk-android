package com.revjet.sdk

/** The kind of ad a [Tag] serves. */
public enum class TagType {
    /** Rendered by the application, from the response the SDK reports. */
    NATIVE,

    /** Rendered by the SDK in a web view. */
    WEB_BASED,
}
