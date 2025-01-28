package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.monoOptional

class ClearCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .flatMap { GuildManager.audioMono(it) }
            .map { it.clearQueue() }
            .then()
    }

    override fun help(): String {
        return "Clears the queue."
    }

}