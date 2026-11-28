package tv.own.owntv.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * TV-only reading profile. Unlike UiZoom, this does not increase Android density.
 * Text grows while settings grids and the player reflow. Never changes the mobile UI.
 */
enum class TvReadabilityProfile(val textScale: Float, val controlDp: Int) {
    STANDARD(1.00f, 44),
    COMFORT(1.20f, 54),
    LARGE(1.36f, 62);

    companion object {
        fun fromStored(value: String?): TvReadabilityProfile =
            entries.firstOrNull { it.name == value } ?: COMFORT
    }
}

val LocalTvReadabilityProfile = staticCompositionLocalOf { TvReadabilityProfile.COMFORT }
val LocalTvReadabilitySetter = staticCompositionLocalOf<(TvReadabilityProfile) -> Unit> { {} }
