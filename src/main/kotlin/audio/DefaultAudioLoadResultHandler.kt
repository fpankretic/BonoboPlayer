package audio

import GlobalData
import com.github.kittinunf.fuel.httpGet
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent

class DefaultAudioLoadResultHandler(
    private val audioManager: GuildAudioManager,
    private val song: String,
    private val event: MessageCreateEvent
) : AudioLoadResultHandler {

    private var notMatched = false


    override fun trackLoaded(track: AudioTrack) {
        println("trackLoaded")
        event.message.channel
            .flatMap { it.createMessage("Track added: ${track.info.title}") }
            .block()
        audioManager.scheduler.play(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        println("playlistLoaded")
        playlist.tracks.forEach { audioManager.scheduler.play(it) }
    }

    override fun noMatches() {
        println("no matches")
        if (notMatched) {
            return
        }
        notMatched = true
        GlobalData.PLAYER_MANAGER.loadItem(getVideoLink(song), this)
    }

    override fun loadFailed(exception: FriendlyException?) {
        println("load failed")
    }

    private fun getVideoLink(query: String): String {
        val (_, _, result) = "http://localhost:8000/api/search".httpGet(listOf("q" to query)).responseString()
        return result.get().substringAfter("\"url\":\"").substringBefore("\"")
    }


}