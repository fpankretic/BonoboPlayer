package handler

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.`object`.VoiceState
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
                    .switchIfEmpty(destroyBot(guildId, event))
                    .flatMap { it.channel }
                    .flatMapMany { it.voiceStates }
                    .flatMap { it.member }
                    .filter { !it.isBot }
                    .count()
                    .filter { (it == 0L) != guildAudio.isLeavingScheduled() } // Everyone left or somebody joined
                    .map {
                        val memberCount = it
                        if (memberCount == 0L && !guildAudio.isLeavingScheduled()) {
                            guildAudio.player.isPaused = true
                            guildAudio.scheduleLeave()
                        } else if (memberCount != 0L && guildAudio.isLeavingScheduled()) {
                            guildAudio.player.isPaused = false
                            guildAudio.cancelLeave()
                        }
                    }
                    .doOnError { logger.error { "Error occurred with message ${it.message}" } }
                    .onErrorComplete()
                    .then()
            }
            .onErrorComplete()
    }

    private fun destroyBot(guildId: Snowflake, event: VoiceStateUpdateEvent): Mono<VoiceState> {
        if (event.isLeaveEvent) {
            logger.info { "Destroy audio in VoiceStateUpdateEvent called." }
            GuildManager.destroyAudio(guildId)
        }
        return Mono.empty<VoiceState?>().mapNotNull { null }
    }

}