package app.nanotv.tv.data.remote

object DurationFormatter {
    const val MIN_NON_SHORT_SECONDS = 76L

    private val isoDurationRegex =
        Regex("""^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$""")
    private val videoIdParamRegex = Regex("""[?&]v=([A-Za-z0-9_-]{11})""")
    private val playlistIdParamRegex = Regex("""[?&]list=([A-Za-z0-9_-]{10,})""")
    private val bareVideoIdRegex = Regex("""^[A-Za-z0-9_-]{11}$""")

    fun parseIsoDurationSeconds(isoDuration: String?): Long {
        if (isoDuration.isNullOrBlank()) return 0L
        val match = isoDurationRegex.matchEntire(isoDuration.trim()) ?: return 0L
        val days = match.groupValues[1].toLongOrNull() ?: 0L
        val hours = match.groupValues[2].toLongOrNull() ?: 0L
        val minutes = match.groupValues[3].toLongOrNull() ?: 0L
        val seconds = match.groupValues[4].toLongOrNull() ?: 0L
        return days * 86_400L + hours * 3_600L + minutes * 60L + seconds
    }

    fun isShortVideo(durationSeconds: Long, title: String, description: String = ""): Boolean {
        if (durationSeconds in 1 until MIN_NON_SHORT_SECONDS) return true
        val combined = "$title $description"
        return combined.contains("#shorts", ignoreCase = true) ||
            combined.contains("#short", ignoreCase = true)
    }

    fun formatSeconds(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return ""
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun parseYouTubeInput(rawInput: String): ParsedYouTubeInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ParsedYouTubeInput.Invalid("Please enter a YouTube link or @channel handle.")

        if (trimmed.contains("/shorts/", ignoreCase = true)) {
            return ParsedYouTubeInput.RejectedShort
        }

        if (trimmed.contains("youtu.be/")) {
            val after = trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("/").trim()
            if (after.length == 11) return ParsedYouTubeInput.Video(after)
        }

        val vidMatch = videoIdParamRegex.find(trimmed)?.groupValues?.getOrNull(1)
        val listMatch = playlistIdParamRegex.find(trimmed)?.groupValues?.getOrNull(1)

        if (trimmed.contains("/playlist") && !listMatch.isNullOrBlank()) {
            return ParsedYouTubeInput.Playlist(listMatch)
        }
        if (!vidMatch.isNullOrBlank()) {
            return ParsedYouTubeInput.Video(vidMatch)
        }
        if (!listMatch.isNullOrBlank()) {
            return ParsedYouTubeInput.Playlist(listMatch)
        }

        for (marker in listOf("/live/", "/embed/")) {
            if (trimmed.contains(marker)) {
                val id = trimmed.substringAfter(marker).substringBefore("?").substringBefore("/").trim()
                if (id.length == 11) return ParsedYouTubeInput.Video(id)
            }
        }

        if ((trimmed.startsWith("PL") || trimmed.startsWith("UU") || trimmed.startsWith("OLAK")) && trimmed.length >= 14 && !trimmed.contains("/")) {
            return ParsedYouTubeInput.Playlist(trimmed)
        }

        if (trimmed.startsWith("UC") && trimmed.length >= 20 && !trimmed.contains("/")) {
            return ParsedYouTubeInput.ChannelId(trimmed)
        }

        val withoutScheme = trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("m.")
            .removePrefix("youtube.com/")

        if (withoutScheme.startsWith("channel/")) {
            val cid = withoutScheme.removePrefix("channel/").substringBefore("/").substringBefore("?")
            if (cid.isNotBlank()) return ParsedYouTubeInput.ChannelId(cid)
        }
        if (withoutScheme.startsWith("@")) {
            val handle = withoutScheme.substringBefore("/").substringBefore("?")
            return ParsedYouTubeInput.ChannelHandle(handle)
        }
        if (trimmed.startsWith("@")) {
            return ParsedYouTubeInput.ChannelHandle(trimmed.substringBefore("/").substringBefore("?"))
        }

        if (bareVideoIdRegex.matches(trimmed)) {
            return ParsedYouTubeInput.Video(trimmed)
        }

        if (!trimmed.contains(" ") && !trimmed.contains(".")) {
            return ParsedYouTubeInput.ChannelHandle("@$trimmed")
        }

        return ParsedYouTubeInput.Invalid("Unrecognized format. Paste a YouTube video link, playlist link, channel ID (UC...), or @handle.")
    }
}

sealed interface ParsedYouTubeInput {
    data class Video(val videoId: String) : ParsedYouTubeInput
    data class Playlist(val playlistId: String) : ParsedYouTubeInput
    data class ChannelId(val channelId: String) : ParsedYouTubeInput
    data class ChannelHandle(val handle: String) : ParsedYouTubeInput
    data object RejectedShort : ParsedYouTubeInput
    data class Invalid(val reason: String) : ParsedYouTubeInput
}
