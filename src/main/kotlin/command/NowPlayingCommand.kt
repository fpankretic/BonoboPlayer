package command

import audio.GuildManager
import audio.RequestedBy
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.bold
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
import util.trackAsHyperLink
import java.util.*

class NowPlayingCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        return event.message.channel.flatMap {
            it.createMessage(responseMessage(getAudioTrack(guildId), getRequestedBy(guildId)))
        }.then()
    }

    override fun help(): String {
        return "Shows the currently playing song."
    }

    private fun getAudioTrack(guildId: Snowflake): Optional<AudioTrack> {
        return if (GuildManager.audioExists(guildId)) {
            GuildManager.getAudio(guildId).currentSong()
        } else {
            Optional.empty()
        }
    }

    private fun getRequestedBy(guildId: Snowflake): RequestedBy? {
        return if (GuildManager.audioExists(guildId)) GuildManager.getAudio(guildId).requestedBy() else null
    }

    private fun responseMessage(song: Optional<AudioTrack>, requestedBy: RequestedBy?): EmbedCreateSpec {
        return song.map {
            nowPlayingMessage(it, requestedBy!!)
        }.orElse(
            simpleMessageEmbed("No songs currently playing")
        )
    }

    private fun nowPlayingMessage(track: AudioTrack, requestedBy: RequestedBy): EmbedCreateSpec {
        logger.debug { "Current track state for ${track.info.title} is ${track.state}" }
        val position = track.position / 1000
        val duration = track.duration / 1000

        val positionMinutes = position / 60
        val positionSeconds = position % 60
        val durationMinutes = duration / 60
        val durationSeconds = duration % 60

        return defaultEmbedBuilder().title("Now playing ♪").thumbnail(track.info.artworkUrl)
            .addField("Playing", bold(trackAsHyperLink(track)), true)
            .addField("Author", track.info.author, true)
            .addField("Progress", progressBar(duration, position), false)
            .addField("Position", String.format("%02d:%02d", positionMinutes, positionSeconds), true)
            .addField("Length", String.format("%02d:%02d", durationMinutes, durationSeconds), true)
            .footer("Requested by ${requestedBy.user}", requestedBy.avatarUrl)
            .timestamp(requestedBy.time).build()
    }

    private fun progressBar(duration: Long, position: Long): String {
        val index = ((position / duration.toDouble()) * 100 / 6.25).toInt()
        return "`" + "▬".repeat(index) + "\uD83D\uDD18" + "▬".repeat(15 - index) + "`"
    }

}