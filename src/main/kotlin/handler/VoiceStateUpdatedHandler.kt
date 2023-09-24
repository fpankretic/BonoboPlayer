package handler

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.`object`.VoiceState
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono

class VoiceStateUpdatedHandler {

    private val logger = KotlinLogging.logger {}

    fun handle(event: VoiceStateUpdateEvent): Mono<Void> {
        val guildId = event.current.guildId

        return mono { GuildManager.getAudio(guildId) }
            .flatMap { updateBot(it, guildId, event) }
            .onErrorComplete()
    }

    private fun updateBot(guildAudio: GuildAudio, guildId: Snowflake, event: VoiceStateUpdateEvent): Mono<Void> {
        return event.client.getSelfMember(guildId)
            .flatMap { it.voiceState }
            .switchIfEmpty(destroyBot(guildId, event))
            .flatMap { it.channel }
            .flatMapMany { it.voiceStates }
            .flatMap { it.member }
            .filter { !it.isBot }
            .count()
            .filter { (it == 0L) != guildAudio.isLeavingScheduled() } // Everyone left or somebody joined
            .map { updateScheduleStatus(it, guildAudio) }
            .doOnError { logger.error { "Error occurred with message ${it.message}" } }
            .onErrorComplete()
            .then()
    }

    private fun destroyBot(guildId: Snowflake, event: VoiceStateUpdateEvent): Mono<VoiceState> {
        return mono { event.isLeaveEvent }
            .filter { it }
            .map { GuildManager.destroyAudio(guildId) }
            .mapNotNull { null }
    }

    private fun updateScheduleStatus(memberCount: Long, guildAudio: GuildAudio) {
        if (memberCount == 0L && !guildAudio.isLeavingScheduled()) {
            guildAudio.player.isPaused = true
            guildAudio.scheduleLeave()
        } else if (memberCount != 0L && guildAudio.isLeavingScheduled()) {
            guildAudio.player.isPaused = false
            guildAudio.cancelLeave()
        }
    }

}