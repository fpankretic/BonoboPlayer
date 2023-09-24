package util

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.`object`.entity.User
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import java.time.Instant

class EmbedUtils {

    companion object {

        fun defaultEmbed(): EmbedCreateSpec.Builder {
            return EmbedCreateSpec.builder()
                .color(Color.PINK)
        }

        fun simpleMessageEmbed(text: String): EmbedCreateSpec.Builder {
            return EmbedCreateSpec.builder()
                .color(Color.PINK)
                .description(text)
        }

        fun simpleMessageEmbedWithAuthor(text: String, author: User): EmbedCreateSpec.Builder {
            return EmbedCreateSpec.builder()
                .color(Color.PINK)
                .description(text)
                .footer("Requested by ${author.globalName.orElse(author.username)}", author.avatarUrl)
                .timestamp(Instant.now())
        }

        fun trackAsHyperLink(playlist: AudioPlaylist): String {
            return textAsHyperLink(playlist.tracks[0].info.title, playlist.tracks[0].info.uri)
        }

        fun trackAsHyperLink(track: AudioTrack): String {
            return textAsHyperLink(track.info.title, track.info.uri)
        }

        fun textAsHyperLink(text: String, url: String): String {
            return "[$text]($url)"
        }

        fun bold(string: String): String {
            return "**$string**"
        }

    }

}