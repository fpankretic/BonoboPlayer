package command

import audio.GuildAudio
import audio.GuildManager
import audio.load.DefaultAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.VoiceChannel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.monoOptional
import java.net.URI
import java.util.*

open class PlayCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        return isUserJoined(event.member).flatMap {
            cancelLeave(guildId)
                .then(executeJoinCommand(event, guildId))
                .then(event.message.channel)
                .map { GuildManager.createAudio(event.client, guildId, it.id) }
                .map { play(it, event) }
                .onErrorStop()
                .then()
        }
    }

    override fun help(): String {
        return "Plays a song."
    }

    protected open fun play(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ").trim()
        val track = loadTrack(query)
        logger.debug { "Parsed query \"$track\"." }

        guildAudio.addHandler(
            DefaultAudioLoadResultHandler(event.guildId.get(), event.message.author.get(), track),
            track
        )
    }

    protected fun loadTrack(query: String): String {
        return try {
            URI(query).toURL().toString()
        } catch (e: Exception) {
            "${searchProvider()}: $query"
        }
    }

    protected open fun searchProvider(): String {
        return "ytmsearch"
    }

    private fun executeJoinCommand(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        if (GuildManager.audioExists(guildId).not()) {
            return JoinCommand().execute(event).onErrorStop()
        }
        logger.debug { "Skipping join command!" }
        return mono { null }
    }

    private fun isUserJoined(member: Optional<Member>): Mono<VoiceChannel> {
        return monoOptional(member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
    }

    private fun cancelLeave(guildId: Snowflake): Mono<GuildAudio> {
        return mono { GuildManager.audioExists(guildId) }
            .filter { it }
            .map { GuildManager.getAudio(guildId) }
            .filter { it.isLeavingScheduled() }
    }

}
