package command

import audio.GuildAudio
import audio.GuildManager
import audio.load.DefaultAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.EnvironmentManager
import util.EnvironmentValue.PREFIX
import util.Message
import util.monoOptional
import util.sendSwitchMessage
import java.net.URI

open class PlayCommand : Command() {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .switchIfEmpty(sendSwitchMessage(event, Message.NOT_IN_VOICE_CHANNEL))
            .flatMap {
                cancelLeave(guildId)
                    .then(executeJoinCommand(event, guildId))
                    .then(event.message.channel)
                    .map { GuildManager.createAudio(event.client, guildId, it.id) }
                    .map { play(event, it, guildId) }
                    .onErrorStop()
                    .then()
            }
    }

    override fun help(): String {
        return "Plays a song."
    }

    protected open fun play(event: MessageCreateEvent, guildAudio: GuildAudio, guildId: Snowflake) {
        val query = event.message.content.substringAfter(" ").trim()
        if (query.isEmpty() || query.startsWith("${EnvironmentManager.valueOf(PREFIX)}p")) {
            return
        }

        val track = loadTrack(query)
        logger.debug { "Parsed query \"$track\"." }

        guildAudio.addHandler(
            DefaultAudioLoadResultHandler(guildId, event.message.author.get(), track),
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
            return JoinCommand().execute(event, guildId).onErrorStop()
        }
        logger.debug { "Skipping join command!" }
        return mono { null }
    }

    private fun cancelLeave(guildId: Snowflake): Mono<GuildAudio> {
        return GuildManager.audioMono(guildId)
            .filter { it.isLeavingScheduled() }
    }

}
