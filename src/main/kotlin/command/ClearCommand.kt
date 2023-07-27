package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class ClearCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .map { GuildAudioManager.of(it).scheduler.clear() }
            .then()
    }
}