package Command

import discord4j.core.event.domain.message.MessageCreateEvent

class PingCommand : Command {
    override fun execute(event: MessageCreateEvent) {
        event.message.channel.block().createMessage("Pong!").block()
    }
}