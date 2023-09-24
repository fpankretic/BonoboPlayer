package command

import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.ReactorUtil.Companion.monoOptional

class PauseCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .map { GuildManager.getAudio(it).player }
            .flatMap { pauseOrResume(it, event) }
            .then()
    }

    private fun pauseOrResume(player: AudioPlayer, event: MessageCreateEvent): Mono<Void> {
        player.playingTrack ?: return mono { null }

        return if (player.isPaused) {
            ResumeCommand().execute(event)
        } else {
            player.isPaused = true
            mono { null }
        }
    }

}