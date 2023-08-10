package audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent

class DefaultAudioLoadResultHandler(
    private val audioManager: GuildAudioManager,
    private val event: MessageCreateEvent
) : AudioLoadResultHandler {

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