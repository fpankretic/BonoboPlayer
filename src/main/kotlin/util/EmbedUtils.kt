package util

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color

class EmbedUtils {

    companion object {

        fun getDefaultEmbed(): EmbedCreateSpec.Builder {
            return EmbedCreateSpec.builder()
                .color(Color.PINK)
        }

        fun getSimpleMessageEmbed(text: String): EmbedCreateSpec {
            return EmbedCreateSpec.builder()
                .color(Color.PINK)
                .description(text)
                .build()
        }

        fun getTrackAsHyperLink(playlist: AudioPlaylist): String {
            return getTextAsHyperLink(playlist.selectedTrack.info.title, playlist.selectedTrack.info.uri)
        }

        fun getTrackAsHyperLink(track: AudioTrack): String {
            return getTextAsHyperLink(track.info.title, track.info.uri)
        }

        fun getTextAsHyperLink(text: String, url: String): String {
            return "[$text]($url)"
        }

    }

}