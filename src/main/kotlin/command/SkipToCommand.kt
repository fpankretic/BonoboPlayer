package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.simpleMessageEmbed

class SkipToCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        return mono { GuildManager.audioExists(guildId) }
            .filter { it }
            .switchIfEmpty(sendQueueEmptyMessage(event))
            .map { GuildManager.getAudio(guildId) }
            .filter { skipSongs(it, event) }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips all songs in the queue to the wanted song."
    }

    private fun skipSongs(guildAudio: GuildAudio, event: MessageCreateEvent): Boolean {
        return event.message.content.split(" ")[1].toInt().let { it != 0 && guildAudio.skipTo(it) }
    }

    private fun sendQueueEmptyMessage(event: MessageCreateEvent): Mono<Boolean> {
        return event.message.channel
            .flatMap { it.createMessage(simpleMessageEmbed("Queue is empty.")) }
            .mapNotNull { null }
    }

}