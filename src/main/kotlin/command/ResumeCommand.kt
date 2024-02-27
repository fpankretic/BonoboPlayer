package command

import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.ReactorUtil.Companion.monoOptional

class ResumeCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .map { GuildManager.getAudio(it) }
            .map { resume(it) }
            .then()
    }

    override fun help(): String {
        return "Resumes the current song."
    }

    private fun resume(guildAudio: GuildAudio) {
        val player = guildAudio.player
        if (player.playingTrack != null && player.isPaused) {
            guildAudio.cancelLeave()
            player.isPaused = false
        }
    }

}