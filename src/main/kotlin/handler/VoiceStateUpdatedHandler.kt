package handler

import audio.GuildManager
import discord4j.core.event.domain.VoiceStateUpdateEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono

class VoiceStateUpdatedHandler {

    private val logger = KotlinLogging.logger {}

    fun handle(event: VoiceStateUpdateEvent): Mono<Void> {
        val guildId = event.current.guildId

        return Mono.defer { Mono.justOrEmpty(GuildManager.getAudio(guildId)) }
            .flatMap { guildAudio ->
                event.client.getSelfMember(guildId)
                    .flatMap { it.voiceState }
                    .flatMap { it.channel }
                    .flatMapMany { it.voiceStates }
                    .flatMap { it.member }
                    .filter { !it.isBot }
                    .count()
                    // Everyone left or somebody joined
                    // TODO: Check
                    .filter { (it == 0L) != guildAudio.isLeavingScheduled() }
                    .map {
                        val memberCount = it
                        if (memberCount == 0L && !guildAudio.isLeavingScheduled()) {
                            guildAudio.player.isPaused = true
                            guildAudio.scheduleLeave()
                        } else if (memberCount != 0L && guildAudio.isLeavingScheduled()) {
                            guildAudio.player.isPaused = false
                            guildAudio.scheduleLeave()
                        }
                    }
                    .doOnError { logger.error { "Error occurred with message ${it.message}" } }
                    .onErrorComplete()
                    .then()
            }
    }

}