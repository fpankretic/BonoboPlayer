package command

import audio.GuildAudio
import audio.GuildManager
import audio.load.DefaultAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URISyntaxException

class PlayCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        cancelLeave(guildId)
        return executeJoinCommand(event, guildId)
            .then(event.message.channel)
            .map { GuildManager.createAudio(event.client, guildId, it.id) }
            .map { play(it, event) }
            .onErrorStop()
            .then()
    }

    override fun help(): String {
        return "Plays a song."
    }

    private fun play(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ").trim()
        val track = loadTrack(query)
        logger.info { "Parsed query: $track." }

        guildAudio.addHandler(
            DefaultAudioLoadResultHandler(event.guildId.get(), event.message.author.get(), track),
            track
        )
    }

    private fun loadTrack(query: String): String {
        return try {
            URI(query).toString()
        } catch (exception: URISyntaxException) {
            "ytmsearch: $query"
        }
    }

    private fun executeJoinCommand(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        if (GuildManager.audioExists(guildId).not()) {
            return JoinCommand().execute(event).onErrorStop()
        }
        logger.info { "Skipping join command!" }
        return mono { null }
    }

    private fun cancelLeave(guildId: Snowflake) {
        if (GuildManager.audioExists(guildId) && GuildManager.getAudio(guildId).isLeavingScheduled()) {
            GuildManager.getAudio(guildId).cancelLeave()
        }
    }

}