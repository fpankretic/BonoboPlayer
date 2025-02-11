package command

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

interface Command {
    fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void>
    fun help(): String

    fun executeForGuild(event: MessageCreateEvent): Mono<Void> {
        return execute(event, event.guildId.get())
    }
}