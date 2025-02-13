package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

object PauseCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return GuildManager.audioMono(guildId)
            .flatMap { pauseOrResume(it, event, guildId) }
            .then()
    }

    override fun help(): String {
        return "Pauses the current song."
    }

    private fun pauseOrResume(guildAudio: GuildAudio, event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        val player = guildAudio.player
        player.playingTrack ?: return mono { null }

        return if (player.isPaused) {
            guildAudio.cancelLeave()
            ResumeCommand.execute(event, guildId)
        } else {
            player.isPaused = true
            mono { null }
        }
    }
}