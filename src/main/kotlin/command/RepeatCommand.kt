package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.simpleMessageEmbed

object RepeatCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        if (GuildManager.audioExists(guildId).not()) {
            return mono { null }
        }

        val guildAudio = GuildManager.audio(guildId)
        guildAudio.changeQueueStatus()

        return event.message.channel
            .flatMap { it.createMessage(repeatMessage(guildAudio.isRepeating())) }
            .then()
    }

    override fun help(): String {
        return "Repeats the current song."
    }

    private fun repeatMessage(isRepeating: Boolean): EmbedCreateSpec {
        return if (isRepeating) {
            simpleMessageEmbed("Repeating ON.")
        } else {
            simpleMessageEmbed("Repeating OFF.")
        }
    }
}