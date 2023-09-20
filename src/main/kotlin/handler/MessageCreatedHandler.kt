package handler

import command.*
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono

class MessageCreatedHandler {

    private val logger = KotlinLogging.logger {}

    companion object {
        private val commands: MutableMap<String, Command> = mutableMapOf()

        init {
            commands["join"] = JoinCommand()
            commands["play"] = PlayCommand()
            commands["p"] = PlayCommand()
            commands["skip"] = SkipCommand()
            commands["s"] = SkipCommand()
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
        val commandName = first.substring(1).toLowerCase()

        if (prefix == '&' && commands.containsKey(commandName)){
            logger.info { "Executing $commandName command." }
            return commands[commandName]!!.execute(event)
        }

        return Mono.empty()
    }

}