package util

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color

const val CANCEL_TEXT: String = "Cancel"

fun defaultEmbedBuilder(): EmbedCreateSpec.Builder {
    return EmbedCreateSpec.builder()
        .color(Color.PINK)
}

fun simpleMessageEmbed(text: String): EmbedCreateSpec {
    return defaultEmbedBuilder()
        .description(text)
        .build()
}

fun trackAsHyperLink(playlist: AudioPlaylist): String {
    return textAsHyperLink(playlist.name, playlist.tracks[0].info.uri)
}

fun trackAsHyperLink(track: AudioTrack): String {
    return textAsHyperLink(track.info.title, track.info.uri)
}

fun bold(string: String): String {
    return "**$string**"
}

fun chooseSongButtons(tracks: List<AudioTrack>, customId: String): Array<ActionRow> {
    val selectButtons = tracks.mapIndexed { index, audioTrack ->
        Button.primary("$customId-${audioTrack.info.uri}", "${index + 1}")
    }
    val cancelButton = Button.danger("$customId-$CANCEL_TEXT", "Cancel")
    return arrayOf(ActionRow.of(selectButtons), ActionRow.of(cancelButton))
}

private fun textAsHyperLink(text: String, url: String): String {
    return "[$text]($url)"
}