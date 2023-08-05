package command

import audio.GuildAudioManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class QueueCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) return Mono.empty()
        return event.message.channel
            .flatMap { it.createMessage(createList(event.guildId.get())) }
            .then()
    }

    private fun createList(guildId: Snowflake): String {
        return GuildAudioManager.of(guildId).scheduler.getQueue()
            .take(10)
            .joinToString("\n") { it.info.title }
    }

}