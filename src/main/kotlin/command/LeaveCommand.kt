package command

import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class LeaveCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
            .flatMap { it.voiceState }
            .flatMap { event.client.voiceConnectionRegistry.getVoiceConnection(it.guildId) }
            .flatMap { it.disconnect() }
            .then()
    }
}