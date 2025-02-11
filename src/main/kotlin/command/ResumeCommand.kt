package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

object ResumeCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return GuildManager.audioMono(guildId)
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