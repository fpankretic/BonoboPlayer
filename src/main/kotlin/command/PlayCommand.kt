package command

import GlobalData
import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono

class PlayCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return JoinCommand().execute(event)
            .then(Mono.justOrEmpty(event.guildId))
            .zipWith(event.message.channel)
            .map { GuildManager.getAudio(event.client, it.t1, it.t2.id) }
            .map { play(it, event) }
            .then()
    }

    private fun play(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ")
        val track = getTrack(query)
        logger.info { "Found track url: $track" }
        GlobalData.PLAYER_MANAGER.loadItem(track, defaultAudioLoadResultHandler(guildAudio, event))
    }

    private fun getTrack(query: String): String {
        if (query.startsWith("http") || query.startsWith("www") || query.startsWith("youtube")) {
            return query
        }
        for (i in 1..2) {
            try {
                return GlobalData.SEARCH_CLIENT.getTracksForSearch(query).get(0).url
            } catch (e: Exception) {
                logger.info { "Failed to fetch URL for $query. Retrying..." }
            }
        }
        logger.info { "Failed to fetch URL for $query. Will not retry." }
        throw RuntimeException()
    }

    private fun defaultAudioLoadResultHandler(
        guildAudio: GuildAudio,
        event: MessageCreateEvent
    ): AudioLoadResultHandler {
        return object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                logger.info { "Loading track." }
                event.message.channel
                    .flatMap { it.createMessage("Adding track to queue: ${track.info.title}") }
                    .block()
                guildAudio.scheduler.play(track.makeClone())
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                logger.info { "Loading playlist." }
                event.message.channel
                    .flatMap { it.createMessage("Adding playlist ${playlist.name} to queue with ${playlist.tracks.size} tracks") }
                    .block()
                playlist.tracks.forEach { guildAudio.scheduler.play(it.makeClone()) }
            }

            override fun noMatches() {
                logger.info { "Found no matches." }
            }

            override fun loadFailed(exception: FriendlyException?) {
                logger.info { "Load failed." }
            }
        }
    }

}