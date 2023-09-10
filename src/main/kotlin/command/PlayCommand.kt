package command

import GlobalData
import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class PlayCommand : Command {

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
        println("Found track url: $track")
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
                println("Failed to fetch URL for $query. Retrying...")
            }
        }
        return GlobalData.SEARCH_CLIENT.getTracksForSearch(query).get(0).url
    }

    private fun defaultAudioLoadResultHandler(
        guildAudio: GuildAudio,
        event: MessageCreateEvent
    ): AudioLoadResultHandler {
        return object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                println("trackLoaded")
                event.message.channel
                    .flatMap { it.createMessage("Adding track to queue: ${track.info.title}") }
                    .block()
                guildAudio.scheduler.play(track.makeClone())
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                println("playlistLoaded")
                event.message.channel
                    .flatMap { it.createMessage("Adding playlist ${playlist.name} to queue with ${playlist.tracks.size} tracks") }
                    .block()
                playlist.tracks.forEach { guildAudio.scheduler.play(it.makeClone()) }
            }

            override fun noMatches() {
                println("no matches")
            }

            override fun loadFailed(exception: FriendlyException?) {
                println("load failed")
            }
        }
    }

}