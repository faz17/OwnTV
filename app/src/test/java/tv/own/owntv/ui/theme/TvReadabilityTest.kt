package tv.own.owntv.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvReadabilityTest {
    @Test fun comfortIsDefaultWhenNoPreferenceExists() {
        assertEquals(TvReadabilityProfile.COMFORT, TvReadabilityProfile.fromStored(null))
    }
    @Test fun storedValuesRoundTripAndUnknownValuesFallBackSafely() {
        TvReadabilityProfile.entries.forEach {
            assertEquals(it, TvReadabilityProfile.fromStored(it.name))
        }
        assertEquals(TvReadabilityProfile.COMFORT, TvReadabilityProfile.fromStored("old-version"))
    }
    @Test fun typographyAndControlsIncreaseWithProfile() {
        assertTrue(TvReadabilityProfile.STANDARD.textScale == 1f)
        assertTrue(TvReadabilityProfile.COMFORT.textScale > TvReadabilityProfile.STANDARD.textScale)
        assertTrue(TvReadabilityProfile.LARGE.textScale > TvReadabilityProfile.COMFORT.textScale)
        assertTrue(TvReadabilityProfile.LARGE.controlDp > TvReadabilityProfile.COMFORT.controlDp)
    }
}
