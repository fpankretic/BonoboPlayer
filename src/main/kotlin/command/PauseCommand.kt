package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent

class PauseCommand : Command {
    override fun execute(event: MessageCreateEvent) {
        if (event.guildId.isEmpty) return
        val player = GuildAudioManager.of(event.guildId.get()).player

        if (player.playingTrack != null) {
            if (player.isPaused) {
                ResumeCommand().execute(event)
            } else {
                player.isPaused = true
            }
        }
    }
}