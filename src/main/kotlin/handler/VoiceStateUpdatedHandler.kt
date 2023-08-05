package handler

import audio.GuildAudioManager
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.`object`.VoiceState
import reactor.core.publisher.Mono

class VoiceStateUpdatedHandler {

    /*fun handle(event: VoiceStateUpdateEvent): Mono<Void> {/*event.client.voiceConnectionRegistry.getVoiceConnection(event.current.guildId)
            .filter { event. }

        val connection = event.client.voiceConnectionRegistry.getVoiceConnection(event.current.guildId);

        // TODO: Cekiraj jel u tom channelu bot
        val voiceStateCounter = event.current.channel.flatMap { channel ->
            channel.voiceStates.count().map {
                println(it)
                it == 1L
            }
        }

        println("tu dodem")*/

        //return connection.filterWhen { voiceStateCounter }.flatMap { it.disconnect() }

        event.client.voiceConnectionRegistry.getVoiceConnection(event.)

        return when {
            event.isJoinEvent -> handleJoinEvent(event)
            event.isMoveEvent -> handleMoveEvent(event)
            event.isLeaveEvent -> handleLeaveEvent(event)
            else -> throw IllegalArgumentException("Invalid event")
        }

        /*val audioManager = GuildAudioManager.of(event.current.guildId)
        if (audioManager.isPlaying()) {
            TODO: Schedule leave
        }*/
    }

    private fun whenOnlyBotIsLeft(voiceState: VoiceState): Mono<Boolean> {
        return voiceState.channel
            .flatMap { channel ->
                channel.voiceStates
                    .count()
                    .map { it == 1L }
            }
    }

    private fun handleJoinEvent(event: VoiceStateUpdateEvent): Mono<Void> {
        return Mono.fromCallable { println("Skipping voice join event.") }.then()
    }

    private fun handleMoveEvent(event: VoiceStateUpdateEvent): Mono<Void> {
        val guildId = event.old.get().guildId
        event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .flatMap {
                it.
            }

    }

    private fun handleLeaveEvent(event: VoiceStateUpdateEvent): Mono<Void> {
        return Mono.empty()
    }*/

}