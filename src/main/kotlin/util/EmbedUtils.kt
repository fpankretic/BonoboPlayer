package util

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.SelectMenu
import discord4j.core.`object`.component.SelectMenu.Option
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color

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

        fun trackAsHyperLink(playlist: AudioPlaylist): String {
            return textAsHyperLink(playlist.tracks[0].info.title, playlist.tracks[0].info.uri)
        }

        fun trackAsHyperLink(track: AudioTrack): String {
            return textAsHyperLink(track.info.title, track.info.uri)
        }

        fun bold(string: String): String {
            return "**$string**"
        }

        fun chooseSongSelect(tracks: List<AudioTrack>, customId: String): ActionRow {
            val select = SelectMenu.of(
                customId,
                tracks.mapIndexed { index, audioTrack -> Option.of((index + 1).toString(), audioTrack.info.uri) }
            )
            return ActionRow.of(select)
        }

        private fun textAsHyperLink(text: String, url: String): String {
            return "[$text]($url)"
        }

    }

}