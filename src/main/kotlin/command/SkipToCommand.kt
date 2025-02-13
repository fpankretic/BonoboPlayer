package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.sendSwitchMessage

object SkipToCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return GuildManager.audioMono(guildId)
            .switchIfEmpty(sendSwitchMessage(event, Message.QUEUE_EMPTY))
            .map { skipSongs(it, event) }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips all songs in the queue to the wanted song."
    }

    private fun skipSongs(guildAudio: GuildAudio, event: MessageCreateEvent) {
        event.message.content.split(" ")[1].toInt().let { it != 0 && guildAudio.skipTo(it) }
    }
}