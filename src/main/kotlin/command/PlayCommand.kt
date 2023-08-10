package command

import GlobalData
import audio.GuildAudioManager
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
            .map { GuildAudioManager.of(it) }
            .map { play(it, event) }
            .then()
    }

    private fun play(audioManager: GuildAudioManager, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ")
        val track = getTrack(query)
        GlobalData.PLAYER_MANAGER.loadItem(track, defaultAudioLoadResultHandler(audioManager, event))
    }

    private fun getTrack(query: String): String {
        if (query.startsWith("http") || query.startsWith("www") || query.startsWith("youtube")) {
            return query
        }
        return GlobalData.SEARCH_CLIENT.getTracksForSearch(query)[0].url
    }

    private fun defaultAudioLoadResultHandler(
        audioManager: GuildAudioManager,
        event: MessageCreateEvent
    ): AudioLoadResultHandler {
        return object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                println("trackLoaded")
                audioManager.scheduler.play(track)
                event.message.channel
                    .flatMap { it.createMessage("Track added: ${track.info.title}") }
                    .block()
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                println("playlistLoaded")
                playlist.tracks.forEach { audioManager.scheduler.play(it) }
                event.message.channel
                    .flatMap { it.createMessage("Playlist ${playlist.name} added with ${playlist.tracks.size} tracks") }
                    .block()
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