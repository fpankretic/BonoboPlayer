package command

import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono
import util.EmbedUtils.Companion.bold
import util.EmbedUtils.Companion.defaultEmbed

class HelpCommand(private val commands: MutableMap<String, Command>) : Command {
    val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val author = event.member.get()

        val messages = commands.filter { isShortCommand(it.key).not() }
            .map { "${bold(it.key)} - ${it.value.help()}" }
            .toMutableList()

        val message = (1..messages.size).joinToString("\n") { "${it}. ${messages[it - 1]}" }
        val embed = defaultEmbed()
            .title("All commands")
            .description(message)
            .build()

        return event.message.channel.flatMap { it.createMessage(embed) }.then()
    }

    override fun help(): String {
        return "List all available commands."
    }

    private fun isShortCommand(command: String): Boolean {
        return command.length <= 2
    }
}