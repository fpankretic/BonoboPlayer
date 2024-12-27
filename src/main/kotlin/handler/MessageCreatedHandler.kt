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
        private val longCommands: MutableMap<String, Command> = mutableMapOf()
        private val shortCommands: MutableMap<String, Command> = mutableMapOf()
        private val commands: MutableMap<String, Command> = mutableMapOf()

        init {
            // commands
            longCommands["play"] = PlayCommand()
            longCommands["yt"] = YoutubeCommand()
            longCommands["ytm"] = YoutubeMusicCommand()
            longCommands["sp"] = SpotifyCommand()
            longCommands["search"] = SearchCommand()
            longCommands["list"] = ListCommand()
            longCommands["queue"] = QueueCommand()
            longCommands["clear"] = ClearCommand()
            longCommands["skip"] = SkipCommand()
            longCommands["skipto"] = SkipToCommand()
            longCommands["remove"] = RemoveCommand()
            longCommands["np"] = NowPlayingCommand()
            longCommands["pause"] = PauseCommand()
            longCommands["resume"] = ResumeCommand()
            longCommands["repeat"] = RepeatCommand()
            longCommands["shuffle"] = ShuffleCommand()
            longCommands["join"] = JoinCommand()
            longCommands["leave"] = LeaveCommand()
            longCommands["help"] = HelpCommand(longCommands, shortCommands)

            // Hidden short commands
            shortCommands["p"] = longCommands["play"]!!
            shortCommands["l"] = longCommands["list"]!!
            shortCommands["q"] = longCommands["queue"]!!
            shortCommands["s"] = longCommands["skip"]!!
            shortCommands["st"] = longCommands["skipto"]!!
            shortCommands["r"] = longCommands["remove"]!!
            shortCommands["sh"] = longCommands["shuffle"]!!
            shortCommands["h"] = longCommands["help"]!!

            // All commands
            commands.putAll(longCommands)
            commands.putAll(shortCommands)
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