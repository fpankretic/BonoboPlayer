package command

import audio.GuildAudioManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class ResumeCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .map { GuildAudioManager.of(event.guildId.get()).player }
            .map { pause(it) }
            .then()
    }

    private fun pause(player: AudioPlayer) {
        if (player.playingTrack != null && player.isPaused) {
            player.isPaused = false
        }
    }
}