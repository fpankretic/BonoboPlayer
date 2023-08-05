package command

import GlobalData
import audio.GuildAudioManager
import com.github.kittinunf.fuel.httpGet
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class PlayCommand : Command {

    private val URL = "http://localhost:8000/api/search"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return JoinCommand().execute(event)
            .then(Mono.justOrEmpty(event.guildId))
            .map { GuildAudioManager.of(it) }
            .map { play(it, event) }
            .then()
    }

    private fun play(audioManager: GuildAudioManager, event: MessageCreateEvent) {
        val song = event.message.content.substringAfter(" ")
        println(song)
        val audioLoadResultHandler = object : AudioLoadResultHandler {
            var notMatched = false

            override fun trackLoaded(track: AudioTrack) {
                println("trackLoaded")
                audioManager.scheduler.play(track)
                // TODO: Ovo nije dobro, dok se spoji spremi chanell i onda na song start poruku ispisi
                event.message.channel
                    .flatMap { it.createMessage("Started playing: ${track.info.title}") }
                    .block()
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
        }

        GlobalData.PLAYER_MANAGER.loadItem(song, audioLoadResultHandler)
    }

    fun getVideoLink(query: String): String {
        val (_, _, result) = URL.httpGet(listOf("q" to query)).responseString()
        return result.get().substringAfter("\"url\":\"").substringBefore("\"")
    }

}