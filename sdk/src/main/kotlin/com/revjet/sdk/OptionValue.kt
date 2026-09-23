package com.revjet.sdk

import com.revjet.sdk.internal.JsLiteral

/**
 * A value of an [Option.Custom] entry.
 *
 * The type is kept, rather than being flattened to a string, because the ad request and the tag
 * script need it rendered differently: `true` is a string in a URL and a boolean in JavaScript.
 */
public sealed interface OptionValue {
    public data class Text(
        public val value: String,
    ) : OptionValue

    public data class Integer(
        public val value: Int,
    ) : OptionValue

    public data class Decimal(
        public val value: Double,
    ) : OptionValue

    public data class Flag(
        public val value: Boolean,
    ) : OptionValue

    public companion object {
        @JvmStatic
        public fun of(value: String): OptionValue = Text(value)

        @JvmStatic
        public fun of(value: Int): OptionValue = Integer(value)

        @JvmStatic
        public fun of(value: Double): OptionValue = Decimal(value)

        @JvmStatic
        public fun of(value: Boolean): OptionValue = Flag(value)
    }
}

/** The value as a plain string, for a URL query parameter. */
internal val OptionValue.stringValue: String
    get() =
        when (this) {
            is OptionValue.Text -> value
            is OptionValue.Integer -> value.toString()
            is OptionValue.Decimal -> value.toString()
            is OptionValue.Flag -> value.toString()
        }

/** The value as a JavaScript literal, for the tag script. */
internal val OptionValue.jsLiteral: String
    get() =
        when (this) {
            is OptionValue.Text -> JsLiteral.string(value)
            is OptionValue.Integer -> value.toString()
            is OptionValue.Decimal -> value.toString()
            is OptionValue.Flag -> value.toString()
        }
