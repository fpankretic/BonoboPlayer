package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent

class ResumeCommand : Command {
    override fun execute(event: MessageCreateEvent) {
        if (event.guildId.isEmpty) return
        val player = GuildAudioManager.of(event.guildId.get()).player

        if (player.playingTrack != null && player.isPaused) {
            player.isPaused = false
        }
    }
}