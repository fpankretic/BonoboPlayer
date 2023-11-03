package command

import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.ReactorUtil.Companion.monoOptional

class ResumeCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .map { pause(GuildManager.getAudio(it).player) }
            .then()
    }

    override fun help(): String {
        return "Resumes the current song."
    }

    private fun pause(player: AudioPlayer) {
        if (player.playingTrack != null && player.isPaused) {
            player.isPaused = false
        }
    }

}