package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono


class SkipCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .filter { GuildAudioManager.of(it).scheduler.skip().not() }
            // leave if queue is empty
            .flatMap { event.client.voiceConnectionRegistry.getVoiceConnection(it) }
            .flatMap { it.disconnect() }
    }
}