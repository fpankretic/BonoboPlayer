package command

import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import mu.KotlinLogging
import reactor.core.publisher.Mono
import util.EmbedUtils
import java.util.*

class NowPlayingCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val song = GuildManager.getAudio(event.guildId.get()).getCurrentSong()
        return event.message.channel.flatMap { it.createMessage(getMessage(song)) }.then()
    }

    private fun getMessage(song: Optional<AudioTrack>): EmbedCreateSpec {
        return song.map {
            EmbedUtils.getSimpleMessageEmbed(nowPlayingMessage(it))
        }.orElse(
            EmbedUtils.getSimpleMessageEmbed("No songs currently playing")
        )
    }

    private fun nowPlayingMessage(track: AudioTrack): String {
        logger.info { "Current track state for ${track.info.title} is ${track.state}" }
        val position = track.position / 1000
        val duration = track.duration / 1000

        val positionMinutes = position / 60
        val positionSeconds = position % 60
        val durationMinutes = duration / 60
        val durationSeconds = duration % 60

        return """
            Now playing playing: ${EmbedUtils.getTrackAsHyperLink(track)}
            Timestamp: ${String.format("%02d:%02d / %02d:%02d", positionMinutes, positionSeconds, durationMinutes, durationSeconds)}
        """.trimIndent()
    }

}