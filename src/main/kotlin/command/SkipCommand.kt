package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono


class SkipCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .filter { GuildManager.getAudio(it).scheduler.skip().not() }
            // leave if queue is empty
            .flatMap { event.client.voiceConnectionRegistry.getVoiceConnection(it) }
            .flatMap { it.disconnect() }
            .then(Mono.fromCallable { GuildManager.destroyAudio(event.guildId.get()) })
            .then()
    }
}