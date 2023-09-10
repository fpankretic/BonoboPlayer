package command

import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import java.util.*

class NowPlayingCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val song = GuildManager.getAudio(event.guildId.get()).scheduler.currentSong()
        return event.message.channel.flatMap { it.createMessage(getMessage(song)) }.then()
    }

    private fun getMessage(song: Optional<AudioTrack>): String {
        return song.map { "Now playing playing: ${it.info.title}" }
            .orElse("No songs currently playing")
    }

}