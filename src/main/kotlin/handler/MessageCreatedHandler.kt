package handler

import command.*
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono
import util.EmbedUtils
import util.EmbedUtils.Companion.bold
import util.EmbedUtils.Companion.defaultEmbed
import java.time.Instant

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
        val commandName = first.substring(1).lowercase()

        if (prefix == '!' && (commands.containsKey(commandName) || commandName == "help" || commandName == "h")) {
            logger.info { "Executing $commandName command." }
            return if (commandName == "help" || commandName == "h")
                helpCommand(event)
            else
                commands[commandName]!!.execute(event)
        }

        return mono { null }
    }

    private fun helpCommand(event: MessageCreateEvent): Mono<Void> {
        val author = event.member.get()

        val messages = commands.map { "${bold(it.key)} - ${it.value.help()}" }.toList()
        val message = (1..commands.size).map { "${it}. ${messages[it - 1]}" }.joinToString("\n")

        val embed = defaultEmbed()
            .title("All commands")
            .description(message)
            .footer("Requested by ${author.globalName.orElse(author.username)}", author.avatarUrl)
            .timestamp(Instant.now())
            .build()

        return event.message.channel.flatMap { it.createMessage(embed) }.then()
    }

}