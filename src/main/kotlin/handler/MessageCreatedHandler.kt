package handler

import command.*
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class MessageCreatedHandler {

    companion object {
        private val commands: MutableMap<String, Command> = mutableMapOf()

        init {
            commands["join"] = JoinCommand()
            commands["play"] = PlayCommand()
            commands["skip"] = SkipCommand()
            commands["pause"] = PauseCommand()
            commands["resume"] = ResumeCommand()
            commands["clear"] = ClearCommand()
            commands["queue"] = QueueCommand()
            commands["q"] = QueueCommand()
            commands["leave"] = LeaveCommand()
            commands["np"] = NowPlayingCommand()
        }
    }

    fun handle(event: MessageCreateEvent): Mono<Void> {
        val content = event.message.content
        if (content.length < 2) return Mono.empty()

        val first = content.split(" ")[0]
        val prefix = first[0]
        val command = first.substring(1).toLowerCase()

        if (prefix == '&' && commands.containsKey(command))
            return commands[command]!!.execute(event)
        return Mono.empty()
    }

}