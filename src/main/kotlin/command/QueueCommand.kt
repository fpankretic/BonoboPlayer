package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
import util.trackAsHyperLink

class QueueCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        return event.message.channel
            .flatMap { it.createMessage(createList(guildId)) }
            .then()
    }

    override fun help(): String {
        return "Shows the current queue."
    }

    private fun createList(guildId: Snowflake): EmbedCreateSpec {
        if (GuildManager.audioExists(guildId).not() || GuildManager.getAudio(guildId).getQueue().isEmpty()) {
            return simpleMessageEmbed("Queue is empty.")
        }

        val queue = GuildManager.getAudio(guildId).getQueue()

        var index = 0
        val fields = queue.take(10)
            .map {
                EmbedCreateFields.Field.of("", "${++index}. ${trackAsHyperLink(it)}", false)
            }
            .toTypedArray()

        return defaultEmbedBuilder()
            .title("Queue")
            .addFields(*fields)
            .build()
    }

}