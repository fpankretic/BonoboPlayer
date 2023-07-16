package handler

import command.*
import discord4j.core.event.domain.message.MessageCreateEvent
import java.util.function.Consumer

class MessageCreatedHandler : Consumer<MessageCreateEvent> {
    companion object {
        private val commands: MutableMap<String, Command> = mutableMapOf()

        init {
            commands["join"] = JoinCommand()
            commands["play"] = PlayCommand()
            commands["skip"] = SkipCommand()
            commands["pause"] = PauseCommand()
            commands["resume"] = ResumeCommand()
            commands["clear"] = ClearCommand()
        }
    }

    override fun accept(event: MessageCreateEvent) {
        val content = event.message.content
        if (content.length < 2) return
        println(content)

        val first = content.split(" ")[0]
        val prefix = first[0]
        val command = first.substring(1)

        if (prefix == '&' && commands.containsKey(command)) commands[command]?.execute(event)
    }
}