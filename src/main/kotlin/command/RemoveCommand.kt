package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.simpleMessageEmbed

class RemoveCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return Mono.empty()
        }
        val guildId = event.guildId.get()

        return mono { GuildManager.audioExists(guildId) }
            .filter { it }
            .switchIfEmpty(sendQueueEmptyMessage(event))
            .map { GuildManager.getAudio(guildId) }
            .filter { skipSong(it, event) }
            .filter { isNotPlaying(it) }
            .map { it.scheduleLeave() }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips any song in the queue."
    }

    private fun skipSong(guildAudio: GuildAudio, event: MessageCreateEvent): Boolean {
        val position = event.message.content.split(" ")[1].toInt()
        return position != 0 && guildAudio.skipInQueue(position)
    }

    private fun isNotPlaying(guildAudio: GuildAudio): Boolean {
        return guildAudio.isLeavingScheduled().not() &&
                guildAudio.getQueue().isNotEmpty() &&
                guildAudio.isSongLoaded().not()
    }

    private fun sendQueueEmptyMessage(event: MessageCreateEvent): Mono<Boolean> {
        return event.message.channel
            .flatMap { it.createMessage(simpleMessageEmbed("Queue is empty.")) }
            .mapNotNull { null }
    }

}