package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.simpleMessageEmbed

object ClearCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return GuildManager.audioMono(guildId)
            .map { clearQueue(it) }
            .then()
    }

    override fun help(): String {
        return "Clears the queue."
    }

    private fun clearQueue(guildAudio: GuildAudio) {
        if (guildAudio.clearQueue()) {
            guildAudio.sendMessage(simpleMessageEmbed(Message.QUEUE_CLEARED.message))
        }
    }

}