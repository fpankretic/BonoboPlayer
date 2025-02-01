package command

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

abstract class Command {
    abstract fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void>
    abstract fun help(): String

    fun executeForGuild(event: MessageCreateEvent): Mono<Void> {
        return execute(event, event.guildId.get())
    }
}