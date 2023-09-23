package command

import GlobalData
import audio.DefaultAudioLoadResultHandler
import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono

class PlayCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return Mono.empty()
        }
        val guildId = event.guildId.get()

        return executeJoinCommand(event)
            .then(event.message.channel)
            .map { GuildManager.getAudio(event.client, guildId, it.id) }
            .map { play(it, event) }
            .doOnError { logger.error { it.message } }
            .retry(2)
            .onErrorComplete()
            .then()
    }

    private fun play(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ").trim()
        val track = getTrack(query)
        logger.info { "Found track url: $track" }
        guildAudio.addHandler(DefaultAudioLoadResultHandler(event.guildId.get(), track))
    }

    private fun getTrack(query: String): String {
        logger.info { "Fetching URL for query: $query." }
        if (query.startsWith("http") || query.startsWith("www") || query.startsWith("youtube")) {
            return query
        }

        return try {
            GlobalData.SEARCH_CLIENT.getTracksForSearch(query).get(0).url
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch URL for $query.")
        }
    }

    private fun executeJoinCommand(event: MessageCreateEvent): Mono<Void> {
        return JoinCommand().execute(event)
    }

}