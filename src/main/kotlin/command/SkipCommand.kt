package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.monoOptional
import util.sendQueueEmptyMessage
import util.simpleMessageEmbed

class SkipCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .flatMap { GuildManager.audioMono(it) }
            .switchIfEmpty(sendQueueEmptyMessage(event))
            .map { skipSong(it) }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips the current song in the queue."
    }

    private fun skipSong(guildAudio: GuildAudio) {
        val trackStarted = guildAudio.skipInQueue(0)
        if (trackStarted.not()) {
            guildAudio.sendMessage(simpleMessageEmbed(Message.QUEUE_EMPTY.message))
            guildAudio.scheduleLeave()
        }
    }

}