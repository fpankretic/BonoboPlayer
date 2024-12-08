package command

import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Mono
import util.bold
import util.defaultEmbedBuilder

class HelpCommand(private val commands: MutableMap<String, Command>) : Command {
    val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val messages = commands
            .map { "${bold(it.key)} - ${it.value.help()}" }
            .toMutableList()

        val message = (1..messages.size).joinToString("\n") { "${it}. ${messages[it - 1]}" }
        val embed = defaultEmbedBuilder()
            .title("All commands")
            .description(message)
            .build()

        return event.message.channel.flatMap { it.createMessage(embed) }.then()
    }

    override fun help(): String {
        return "List all available commands."
    }

}