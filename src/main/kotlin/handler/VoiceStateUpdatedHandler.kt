package handler

import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.`object`.VoiceState
import discord4j.voice.VoiceConnection
import reactor.core.publisher.Mono

class VoiceStateUpdatedHandler {

    fun handle(event: VoiceStateUpdateEvent): Mono<Void> {
        val guildId = event.current.guildId
        val botConnectionMono = event.client.voiceConnectionRegistry.getVoiceConnection(guildId)

        // TODO: Scheduled leave
        return event.client.getSelfMember(guildId)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .flatMapMany { it.voiceStates }
            .flatMap { it.member }
            .filter { !it.isBot }
            .count()
            .filter { it == 0L }
            .flatMap { botConnectionMono }
            .flatMap { it.disconnect() }
    }

}