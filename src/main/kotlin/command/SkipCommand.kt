package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.simpleMessageEmbed

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
            .filter { skipSong(it) }
            .map { it.scheduleLeave() }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips the current song in the queue."
    }

    private fun skipSong(guildAudio: GuildAudio): Boolean {
        return guildAudio.skipInQueue(0).not() && guildAudio.isLeavingScheduled().not()
    }

    private fun sendQueueEmptyMessage(event: MessageCreateEvent): Mono<Boolean> {
        return event.message.channel
            .flatMap { it.createMessage(simpleMessageEmbed("Queue is empty.")) }
            .mapNotNull { null }
    }

}