package handler

import command.*
import discord4j.core.event.domain.message.MessageCreateEvent
import env.EnvironmentManager
import env.EnvironmentValue.PREFIX
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono
import util.EmbedUtils.Companion.defaultEmbed

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
            commands["h"] = HelpCommand(commands)
            commands["help"] = HelpCommand(commands)
            commands["search"] = SearchCommand()
            commands["remove"] = RemoveCommand()
            commands["r"] = RemoveCommand()
            commands["skipto"] = SkipToCommand()
            commands["st"] = SkipToCommand()
        }
    }

    fun handle(event: MessageCreateEvent): Mono<Void> {
        val content = event.message.content
        if (content.length < 2) return Mono.empty()

        val first = content.split(" ")[0]
        val prefix = first[0].toString()
        val commandName = first.substring(1).lowercase()

        if (isCommand(prefix, commandName)) {
            logger.info { "Executing $commandName command." }
            return commands[commandName]!!.execute(event)
        }

        return mono { null }
    }

    private fun isCommand(prefix: String, commandName: String): Boolean {
        return prefix == EnvironmentManager.get(PREFIX) &&
                (commands.containsKey(commandName) || commandName == "help" || commandName == "h")
    }

}