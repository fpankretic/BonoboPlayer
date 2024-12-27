package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.simpleMessageEmbed

class ShuffleCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }

        val guildId = event.guildId.get()
        return mono { GuildManager.audioExists(guildId) }
            .filter { it }
            .switchIfEmpty(sendQueueEmptyMessage(event))
            .map { GuildManager.getAudio(guildId) }
            .filter { it.shuffleQueue() }
            .map { it.sendMessage(simpleMessageEmbed("Queue shuffled.")) }
            .then()
    }

    override fun help(): String {
        return "Shuffles the queue."
    }

    private fun sendQueueEmptyMessage(event: MessageCreateEvent): Mono<Boolean> {
        return event.message.channel
            .flatMap { it.createMessage(simpleMessageEmbed("Queue is empty.")) }
            .mapNotNull { null }
    }
}