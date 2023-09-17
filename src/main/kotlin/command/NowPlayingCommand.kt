package command

import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono
import util.EmbedUtils
import java.util.*

class NowPlayingCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val song = GuildManager.getAudio(event.guildId.get()).getCurrentSong()
        return event.message.channel.flatMap { it.createMessage(getMessage(song)) }.then()
    }

    private fun getMessage(song: Optional<AudioTrack>): EmbedCreateSpec {
        return song.map {
            EmbedUtils.getSimpleMessageEmbed("Now playing playing: ${EmbedUtils.getTrackAsHyperLink(it)}")
        }.orElse(
            EmbedUtils.getSimpleMessageEmbed("No songs currently playing")
        )
    }

}