package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import env.EnvironmentManager
import env.EnvironmentValue.PREFIX
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.bold
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
import util.trackAsHyperLink
import kotlin.math.max

class QueueCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }

        val guildId = event.guildId.get()
        val pageNumber = pageNumberFromMessage(event.message.content)

        return event.message.channel.flatMap {
            it.createMessage(createList(guildId, pageNumber))
        }.then()
    }

    override fun help(): String {
        return "Shows the current queue."
    }

    private fun createList(guildId: Snowflake, pageNumber: Int): EmbedCreateSpec {
        val queue = GuildManager.getAudio(guildId).getQueue()
        if (GuildManager.audioExists(guildId).not() || queue.isEmpty()) {
            return simpleMessageEmbed("Queue is empty.")
        }

        val lastPage = max(1, (queue.size - 1) / 10 + 1)
        if (pageNumber < 1 || pageNumber > lastPage) {
            return simpleMessageEmbed("Invalid page number. Must be between 1 and $lastPage.")
        }

        val songList = queue.drop((pageNumber - 1) * 10)
            .take(10)
            .mapIndexed { index, track -> "${index + 1}. ${trackAsHyperLink(track)}" }
            .joinToString("\n")

        val prefix = EnvironmentManager.get(PREFIX)
        val pageNumberMessage = "${bold("Page")}: $pageNumber / $lastPage. Choose a page with `${prefix}queue <page>`."

        val songField = EmbedCreateFields.Field.of("", songList, false)
        val pageField = EmbedCreateFields.Field.of("", pageNumberMessage, false)

        return defaultEmbedBuilder()
            .title("Queue (${queue.size} track/s)")
            .addFields(songField, pageField)
            .build()
    }

    private fun pageNumberFromMessage(message: String): Int {
        return try {
            message.split(" ")[1].toInt()
        } catch (e: Exception) {
            when (e) {
                is IndexOutOfBoundsException, is NumberFormatException -> 1
                else -> throw e
            }
        }
    }

}