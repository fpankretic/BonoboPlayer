package command

import audio.GuildAudioManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class PauseCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .map { GuildAudioManager.of(it).player }
            .flatMap { pauseOrResume(it, event) }
            .then()
    }

    private fun pauseOrResume(player: AudioPlayer, event: MessageCreateEvent) : Mono<Void> {
        player.playingTrack ?: return Mono.empty()

        return if (player.isPaused){
            ResumeCommand().execute(event)
        } else {
            player.isPaused = true
            Mono.empty()
        }
    }
}