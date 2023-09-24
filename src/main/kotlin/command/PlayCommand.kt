package command

import audio.DefaultAudioLoadResultHandler
import audio.GuildAudio
import audio.GuildManager
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

        return executeJoinCommand(event)
            .then(event.message.channel)
            .map { GuildManager.getAudio(event.client, guildId, it.id) }
            .map { play(it, event) }
            .doOnError { logger.error { it.message } }
            .retry(2)
            .onErrorStop()
            .then()
    }

    private fun play(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ").trim()
        val track = loadTrack(query)
        logger.info { "Parsed query: $track." }
        guildAudio.addHandler(DefaultAudioLoadResultHandler(event.guildId.get(), track, event.message.author.get()))
    }

    private fun loadTrack(query: String): String {
        return try {
            URI(query).toString()
        } catch (exception: URISyntaxException) {
            "ytsearch: $query"
        }
    }

    private fun executeJoinCommand(event: MessageCreateEvent): Mono<Void> {
        return JoinCommand().execute(event)
    }

}