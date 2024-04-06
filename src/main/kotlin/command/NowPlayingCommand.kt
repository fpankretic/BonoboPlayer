package command

import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono
import util.EmbedUtils
import util.EmbedUtils.Companion.bold
import util.EmbedUtils.Companion.simpleMessageEmbed
import util.EmbedUtils.Companion.trackAsHyperLink
import java.time.Instant
import java.util.*

class NowPlayingCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        return event.message.channel.flatMap { it.createMessage(responseMessage(song(guildId), event)) }.then()
    }

    override fun help(): String {
        return "Shows the currently playing song."
    }

    private fun song(guildId: Snowflake): Optional<AudioTrack> {
        return if (GuildManager.audioExists(guildId)) {
            GuildManager.getAudio(guildId).currentSong()
        } else {
            Optional.empty()
        }
    }

    private fun responseMessage(song: Optional<AudioTrack>, event: MessageCreateEvent): EmbedCreateSpec {
        return song.map {
            nowPlayingMessage(it, event)
        }.orElse(
            simpleMessageEmbed("No songs currently playing").build()
        )
    }

    private fun nowPlayingMessage(track: AudioTrack, event: MessageCreateEvent): EmbedCreateSpec {
        logger.info { "Current track state for ${track.info.title} is ${track.state}" }
        val position = track.position / 1000
        val duration = track.duration / 1000

        val positionMinutes = position / 60
        val positionSeconds = position % 60
        val durationMinutes = duration / 60
        val durationSeconds = duration % 60

        val author = event.message.author.get()

        return EmbedUtils.defaultEmbed().title("Now playing ♪").thumbnail(track.info.artworkUrl)
            .addField("Playing", bold(trackAsHyperLink(track)), false)
            .addField("Progress", progressBar(duration, position), true)
            .addField("Position", String.format("%02d:%02d", positionMinutes, positionSeconds), true)
            .addField("Length", String.format("%02d:%02d", durationMinutes, durationSeconds), true)
            .footer("Requested by ${author.globalName.orElse(author.username)}", author.avatarUrl)
            .timestamp(Instant.now()).build()
    }

    private fun progressBar(duration: Long, position: Long): String {
        val index = ((position / duration.toDouble()) * 100 / 6.25).toInt()
        return "`" + "▬".repeat(index) + "\uD83D\uDD18" + "▬".repeat(15 - index) + "`"
    }

}