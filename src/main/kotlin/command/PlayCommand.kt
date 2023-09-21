package command

import GlobalData
import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import mu.KotlinLogging
import reactor.core.publisher.Mono
import util.EmbedUtils
import kotlin.math.log

class PlayCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return JoinCommand().execute(event)
            .then(Mono.justOrEmpty(event.guildId))
            .zipWith(event.message.channel)
            .map { GuildManager.getAudio(event.client, it.t1, it.t2.id) }
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
        GlobalData.PLAYER_MANAGER.loadItem(track, defaultAudioLoadResultHandler(guildAudio, event))
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

    private fun defaultAudioLoadResultHandler(
        guildAudio: GuildAudio,
        event: MessageCreateEvent
    ): AudioLoadResultHandler {
        return object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                logger.info { "Started loading track ${track.info.title}." }
                guildAudio.sendMessage(getTrackLoadedMessage(track))
                guildAudio.play(track)
                logger.info { "Finished loading track ${track.info.title}." }
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                logger.info { "Started loading playlist ${playlist.name}." }
                guildAudio.sendMessage(getPlaylistLoadedMessage(playlist))
                playlist.tracks.forEach { guildAudio.play(it) }
                logger.info { "Finished loading playlist ${playlist.name}." }
            }

            override fun noMatches() {
                logger.info { "Found no matches." }
            }

            override fun loadFailed(exception: FriendlyException?) {
                logger.info { "Load failed." }
            }
        }
    }

    private fun getTrackLoadedMessage(track: AudioTrack): EmbedCreateSpec {
        return EmbedUtils.getSimpleMessageEmbed(
            "Added to queue: ${EmbedUtils.getTextAsHyperLink(track.info.title, track.info.uri)}"
        )
    }

    private fun getPlaylistLoadedMessage(playlist: AudioPlaylist): EmbedCreateSpec {
        return EmbedUtils.getSimpleMessageEmbed(
            "Added playlist ${EmbedUtils.getTrackAsHyperLink(playlist)} with ${playlist.tracks.size} tracks"
        )

    }

}