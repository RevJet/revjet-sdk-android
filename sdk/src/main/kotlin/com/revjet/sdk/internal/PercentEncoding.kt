package com.revjet.sdk.internal

private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

/**
 * The string percent-encoded for use as a URL query parameter.
 *
 * Everything outside the unreserved set is encoded, and a space becomes `%20` rather than `+`,
 * which is only valid in form bodies.
 */
internal fun String.percentEncoded(): String {
    val encoded = StringBuilder(length)

    for (byte in toByteArray(Charsets.UTF_8)) {
        val character = byte.toInt().toChar()
        if (byte >= 0 && UNRESERVED.indexOf(character) >= 0) {
            encoded.append(character)
        } else {
            encoded.append('%').append("%02X".format(byte.toInt() and 0xFF))
        }
    }

    return encoded.toString()
}
