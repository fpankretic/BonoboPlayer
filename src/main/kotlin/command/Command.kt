package command

import discord4j.core.event.domain.message.MessageCreateEvent

fun interface Command {
    fun execute(event: MessageCreateEvent)
}