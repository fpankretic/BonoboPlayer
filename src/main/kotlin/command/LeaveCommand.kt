package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.monoOptional

class LeaveCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { event.client.voiceConnectionRegistry.getVoiceConnection(it.guildId) }
            .flatMap { it.disconnect() }
            .then(Mono.fromCallable { GuildManager.destroyAudio(event.guildId.get()) })
            .then()
    }

    override fun help(): String {
        return "Leaves the voice channel."
    }

}