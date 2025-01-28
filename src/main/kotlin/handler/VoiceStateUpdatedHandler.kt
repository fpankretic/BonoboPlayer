package handler

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.`object`.VoiceState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

class VoiceStateUpdatedHandler {

    private val logger = KotlinLogging.logger {}

    fun handle(event: VoiceStateUpdateEvent): Mono<Void> {
        val guildId = event.current.guildId
        val userId = event.current.userId

        if (userId.equals(event.client.selfId)) {
            if (event.isLeaveEvent) {
                GuildManager.destroyAudio(guildId)
                return mono { event.client.voiceConnectionRegistry }
                    .flatMap { it.getVoiceConnection(guildId) }
                    .flatMap { it.disconnect() }
                    .onErrorComplete()
            }
            return mono { null }
        }

        return mono { GuildManager.audio(guildId) }
            .filter { isMovementEvent(event) }
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
            println("executing")
            guildAudio.player.isPaused = false
            if (guildAudio.currentSong().isPresent) {
                guildAudio.cancelLeave()
            }
        }
    }

    private fun isMovementEvent(event: VoiceStateUpdateEvent): Boolean {
        return event.isJoinEvent || event.isLeaveEvent || event.isMoveEvent
    }
}