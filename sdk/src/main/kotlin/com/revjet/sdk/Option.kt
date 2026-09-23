package com.revjet.sdk

/**
 * Configures how the ad server and the creative behave.
 *
 * Options reach native and web-based tags alike: as query parameters of the ad request, and as
 * entries of the tag script's options object.
 */
public sealed class Option {
    /** Requested creative size, such as 300x250. */
    public data class ImpBannerSize(
        public val width: Int,
        public val height: Int,
    ) : Option()

    /** Hides the ad slot when no creative is served. */
    public data class Autohide(
        public val enabled: Boolean,
    ) : Option()

    /** Scales the creative to its container. */
    public data class Autoscale
        @JvmOverloads
        constructor(
            public val enabled: Boolean,
            public val mode: ScaleMode = ScaleMode.FIT_TO_WIDTH,
        ) : Option()

    /** Serves the tag from another domain instead of [Constants.ADS_DOMAIN]. */
    public data class CustomDomain(
        public val domain: String,
    ) : Option()

    /** The delivery format of the ad. */
    public data class Delivery(
        public val method: DeliveryMethod,
    ) : Option()

    /** Lets the creative size itself to the container. */
    public data class Responsive(
        public val enabled: Boolean,
    ) : Option()

    /** How the creative's height is determined. */
    public data class ResponsiveHeight(
        public val dimension: ResponsiveDimension,
    ) : Option()

    /** How the creative's width is determined. */
    public data class ResponsiveWidth(
        public val dimension: ResponsiveDimension,
    ) : Option()

    /** Sizes the server may choose from, such as `300x250`. */
    public data class AdaptiveSizes(
        public val sizes: List<String>,
    ) : Option()

    /** Identifies the hosting app to the ad server. */
    public data class InApp(
        public val value: String,
    ) : Option()

    /** Excludes the request from session tracking. */
    public data class NoSession(
        public val enabled: Boolean,
    ) : Option()

    /** Contextual content passed to the creative. */
    public data class Content(
        public val value: String,
    ) : Option()

    /** Any further options the ad server accepts. */
    public data class Custom(
        public val values: Map<String, OptionValue>,
    ) : Option()
}

/** The parameters the option contributes, in the order the ad server expects them. */
internal val Option.queryItems: List<Pair<String, OptionValue>>
    get() =
        when (this) {
            is Option.ImpBannerSize -> listOf("_imp_banner_size" to OptionValue.Text("${width}x$height"))
            is Option.Autohide -> listOf("autohide" to OptionValue.Flag(enabled))
            is Option.Autoscale ->
                listOf(
                    "autoscale" to OptionValue.Flag(enabled),
                    "autoscale_mode" to OptionValue.Text(mode.value),
                )
            is Option.CustomDomain -> listOf("custom_domain" to OptionValue.Text(domain))
            is Option.Delivery -> listOf("delivery_method" to OptionValue.Text(method.value))
            is Option.Responsive -> listOf("responsive" to OptionValue.Flag(enabled))
            is Option.ResponsiveHeight -> listOf("responsive_height" to OptionValue.Text(dimension.value))
            is Option.ResponsiveWidth -> listOf("responsive_width" to OptionValue.Text(dimension.value))
            is Option.AdaptiveSizes -> listOf("adaptive_sizes" to OptionValue.Text(sizes.joinToString(",")))
            is Option.InApp -> listOf("in_app" to OptionValue.Text(value))
            is Option.NoSession -> listOf("no_session" to OptionValue.Flag(enabled))
            is Option.Content -> listOf("content" to OptionValue.Text(value))
            is Option.Custom -> values.map { it.key to it.value }
        }

/** The domain the tag is served from. */
internal val List<Option>.domain: String
    get() = filterIsInstance<Option.CustomDomain>().firstOrNull()?.domain ?: Constants.ADS_DOMAIN

/** Whether a domain was chosen explicitly. */
internal val List<Option>.hasCustomDomain: Boolean
    get() = any { it is Option.CustomDomain }
