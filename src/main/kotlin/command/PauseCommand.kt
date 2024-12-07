package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.monoOptional

class PauseCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .map { GuildManager.getAudio(it) }
            .flatMap { pauseOrResume(it, event) }
            .then()
    }

    override fun help(): String {
        return "Pauses the current song."
    }

    private fun pauseOrResume(guildAudio: GuildAudio, event: MessageCreateEvent): Mono<Void> {
        val player = guildAudio.player
        player.playingTrack ?: return mono { null }

        return if (player.isPaused) {
            guildAudio.cancelLeave()
            ResumeCommand().execute(event)
        } else {
            player.isPaused = true
            guildAudio.scheduleLeave()
            mono { null }
        }
    }

}