package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono
import util.EmbedUtils

class QueueCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) return Mono.empty()
        return event.message.channel
            .flatMap { it.createMessage(createList(event.guildId.get())) }
            .then()
    }

    private fun createList(guildId: Snowflake): EmbedCreateSpec {
        val queue = GuildManager.getAudio(guildId).getQueue()

        if (queue.isEmpty()) {
            return EmbedUtils.getSimpleMessageEmbed("Queue is empty.")
        }

        var index = 0
        val fields = queue.take(10)
            .map {
                EmbedCreateFields.Field.of("", "${++index}. ${EmbedUtils.getTrackAsHyperLink(it)}", false)
            }
            .toTypedArray()

        return EmbedUtils.getDefaultEmbed()
            .addFields(*fields)
            .build()
    }

}