package tv.own.owntv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tv.own.owntv.core.dispatcharr.DispatcharrApi

class PlayerHudSourcesTest {
    private fun stream(
        id: Int,
        name: String,
        providerName: String? = null,
    ) = DispatcharrApi.AssignedStream(
        id = id,
        name = name,
        providerAccountId = null,
        streamStats = null,
        providerAccountName = providerName,
    )

    private fun sources(active: Int?, vararg streams: DispatcharrApi.AssignedStream) =
        DispatcharrApi.ChannelSources(
            channelUuid = "test-channel",
            channelId = 1,
            activeStreamId = active,
            streams = streams.toList(),
        )

    @Test
    fun displaysOnlyTheActiveUpstreamWithItsProvider() {
        assertEquals(
            "Fabio · Nexon",
            activeDispatcharrSourceName(
                sources(2, stream(1, "Other", "Elsewhere"), stream(2, "Nexon", "Fabio")),
            ),
        )
    }

    @Test
    fun fallsBackToStreamNameWhenProviderNameIsNotKnown() {
        assertEquals("Nexon", activeDispatcharrSourceName(sources(2, stream(2, "Nexon"))))
    }

    @Test
    fun doesNotInventAnActiveStreamWhenDispatcharrHasNotReportedOne() {
        assertNull(activeDispatcharrSourceName(sources(null, stream(2, "Nexon", "Fabio"))))
        assertNull(activeDispatcharrSourceName(sources(99, stream(2, "Nexon", "Fabio"))))
    }

    @Test
    fun avoidsRepeatingIdenticalProviderAndStreamNames() {
        assertEquals("Nexon", activeDispatcharrSourceName(sources(2, stream(2, "Nexon", "Nexon"))))
    }
}
