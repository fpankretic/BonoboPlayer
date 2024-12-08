package handler

import command.*
import discord4j.core.event.domain.message.MessageCreateEvent
import env.EnvironmentManager
import env.EnvironmentValue.PREFIX
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

class MessageCreatedHandler {

    private val logger = KotlinLogging.logger {}
    private val prefix = EnvironmentManager.get(PREFIX)

    companion object {
        private val displayHelpCommands: MutableMap<String, Command> = mutableMapOf()
        private val hiddenHelpCommands: MutableMap<String, Command> = mutableMapOf()
        private val commands: MutableMap<String, Command> = mutableMapOf()

        init {
            // Display help commands
            displayHelpCommands["play"] = PlayCommand()
            displayHelpCommands["yt"] = YoutubeCommand()
            displayHelpCommands["ytm"] = YoutubeMusicCommand()
            displayHelpCommands["search"] = SearchCommand()
            displayHelpCommands["list"] = ListCommand()
            displayHelpCommands["queue"] = QueueCommand()
            displayHelpCommands["clear"] = ClearCommand()
            displayHelpCommands["skip"] = SkipCommand()
            displayHelpCommands["skipto"] = SkipToCommand()
            displayHelpCommands["remove"] = RemoveCommand()
            displayHelpCommands["np"] = NowPlayingCommand()
            displayHelpCommands["pause"] = PauseCommand()
            displayHelpCommands["resume"] = ResumeCommand()
            displayHelpCommands["join"] = JoinCommand()
            displayHelpCommands["leave"] = LeaveCommand()
            displayHelpCommands["help"] = HelpCommand(displayHelpCommands)

            // Hidden help commands
            hiddenHelpCommands["p"] = PlayCommand()
            hiddenHelpCommands["l"] = ListCommand()
            hiddenHelpCommands["q"] = QueueCommand()
            hiddenHelpCommands["s"] = SkipCommand()
            hiddenHelpCommands["st"] = SkipToCommand()
            hiddenHelpCommands["r"] = RemoveCommand()
            hiddenHelpCommands["h"] = HelpCommand(displayHelpCommands)

            // All commands
            commands.putAll(displayHelpCommands)
            commands.putAll(hiddenHelpCommands)
        }
    }

    fun handle(event: MessageCreateEvent): Mono<Void> {
        val content = event.message.content
        if (content.length < 2) return Mono.empty()

        val first = content.split(" ")[0]
        val foundPrefix = first[0].toString()
        val commandName = first.substring(1).lowercase()

        if (isCommand(foundPrefix, commandName)) {
            logger.info { "Executing $commandName command." }
            return commands[commandName]?.execute(event) ?: mono { null }
        }

        return mono { null }
    }

    private fun isCommand(foundPrefix: String, commandName: String): Boolean {
        return foundPrefix == prefix && commands.containsKey(commandName)
    }

}