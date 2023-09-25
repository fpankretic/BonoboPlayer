package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.EmbedUtils

class SkipCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        return mono { GuildManager.audioExists(guildId) }
            .filter { it }
            .switchIfEmpty(sendQueueEmptyMessage(event))
            .map { GuildManager.getAudio(guildId) }
            .filter { filterChain(it, event) }
            .map { it.scheduleLeave() }
            .onErrorComplete()
            .then()
    }

    private fun filterChain(guildAudio: GuildAudio, event: MessageCreateEvent): Boolean {
        val position = event.message.content.split(" ").getOrElse(1) { "0" }.toInt()
        return guildAudio.skipInQueue(position).not() &&
                guildAudio.isLeavingScheduled().not() &&
                position == 0
    }

    private fun sendQueueEmptyMessage(event: MessageCreateEvent): Mono<Boolean> {
        return event.message.channel
            .flatMap { it.createMessage(queueEmptyMessage()) }
            .mapNotNull { null }
    }

    private fun queueEmptyMessage(): EmbedCreateSpec {
        return EmbedUtils.simpleMessageEmbed("Queue is empty.").build()
    }

}