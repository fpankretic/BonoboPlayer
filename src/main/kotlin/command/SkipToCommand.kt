package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.monoOptional
import util.sendQueueEmptyMessage

class SkipToCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .flatMap { GuildManager.audioMono(it) }
            .switchIfEmpty(sendQueueEmptyMessage(event))
            .map { skipSongs(it, event) }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips all songs in the queue to the wanted song."
    }

    private fun skipSongs(guildAudio: GuildAudio, event: MessageCreateEvent) {
        event.message.content.split(" ")[1].toInt().let { it != 0 && guildAudio.skipTo(it) }
    }

}