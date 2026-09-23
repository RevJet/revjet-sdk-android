package com.revjet.sdk.internal

internal object JsLiteral {
    /**
     * The string as a single-quoted JavaScript literal, escaped for an inline `<script>`.
     *
     * The backslash has to be escaped first, or it would escape the escapes added after it.
     */
    fun string(value: String): String {
        val escaped =
            value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029")
                .replace("<", "\\x3C")

        return "'$escaped'"
    }
}
