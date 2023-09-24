package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.ReactorUtil.Companion.monoOptional

class ClearCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .map { GuildManager.getAudio(it).clearQueue() }
            .then()
    }

}