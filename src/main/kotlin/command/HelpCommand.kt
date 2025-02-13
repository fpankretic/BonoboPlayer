package command

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.bold
import util.defaultEmbedBuilder

class HelpCommand(
    private val longCommands: MutableMap<String, Command>,
    private val shortCommands: MutableMap<String, Command>
) : Command {

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        val messages = longCommands
            .map { "${entryName(it)} - ${it.value.help()}" }
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

    private fun entryName(entry: Map.Entry<String, Command>): String {
        val shortname = shortCommands.entries.find { it.value == entry.value }?.key ?: ""
        return bold(if (shortname.isNotEmpty()) "${entry.key} ($shortname)" else entry.key)
    }

}