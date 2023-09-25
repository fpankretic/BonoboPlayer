package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.User
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.EmbedUtils
import util.EmbedUtils.Companion.defaultEmbed
import util.EmbedUtils.Companion.simpleMessageEmbed
import java.time.Instant

class QueueCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()
        val author = event.member.get()
        return event.message.channel
            .flatMap { it.createMessage(createList(guildId, author)) }
            .then()
    }

    private fun createList(guildId: Snowflake, author: User): EmbedCreateSpec {
        if (GuildManager.audioExists(guildId).not() || GuildManager.getAudio(guildId).getQueue().isEmpty()) {
            return simpleMessageEmbed("Queue is empty.").build()
        }

        val queue = GuildManager.getAudio(guildId).getQueue()

        var index = 0
        val fields = queue.take(10)
            .map {
                EmbedCreateFields.Field.of("", "${++index}. ${EmbedUtils.trackAsHyperLink(it)}", false)
            }
            .toTypedArray()

        return defaultEmbed()
            .title("Queue")
            .addFields(*fields)
            .footer("Requested by ${author.globalName.orElse(author.username)}", author.avatarUrl)
            .timestamp(Instant.now())
            .build()
    }

}