package app.nanotv.tv

import app.nanotv.tv.data.local.PinHasher
import app.nanotv.tv.data.remote.DurationFormatter
import app.nanotv.tv.data.remote.ParsedYouTubeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoTvPolicyAndParserTest {

    @Test
    fun pinHashingIsDeterministicAndSalted() {
        val h1 = PinHasher.hashPin("2486")
        val h2 = PinHasher.hashPin("2486")
        val h3 = PinHasher.hashPin("1234")
        assertEquals(64, h1.length)
        assertEquals(h1, h2)
        assertNotEquals(h1, h3)
        assertNotEquals("2486", h1)
    }

    @Test
    fun blocksShortsAccurately() {
        assertTrue(DurationFormatter.isShortVideo(45L, "Experiment"))
        assertTrue(DurationFormatter.isShortVideo(0L, "Quick Clip #Shorts"))
        assertFalse(DurationFormatter.isShortVideo(360L, "How Volcanoes Work"))
        assertEquals(
            ParsedYouTubeInput.RejectedShort,
            DurationFormatter.parseYouTubeInput("https://www.youtube.com/shorts/abc12345678")
        )
    }

    @Test
    fun parsesVideosPlaylistsAndHandles() {
        assertEquals(
            ParsedYouTubeInput.Video("rL0qEmTRI-I"),
            DurationFormatter.parseYouTubeInput("https://youtu.be/rL0qEmTRI-I")
        )
        assertEquals(
            ParsedYouTubeInput.Playlist("PL1234567890ABCD"),
            DurationFormatter.parseYouTubeInput("https://www.youtube.com/playlist?list=PL1234567890ABCD")
        )
        assertEquals(
            ParsedYouTubeInput.ChannelHandle("@SciShowKids"),
            DurationFormatter.parseYouTubeInput("@SciShowKids")
        )
    }
}
