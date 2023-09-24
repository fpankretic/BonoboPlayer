package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.ReactorUtil.Companion.monoOptional

class LeaveCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { event.client.voiceConnectionRegistry.getVoiceConnection(it.guildId) }
            .flatMap { it.disconnect() }
            .then(mono { GuildManager.destroyAudio(event.guildId.get()) })
            .then()
    }

}