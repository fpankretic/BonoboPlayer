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
        val song = event.message.content.split(" ")[1]
        GlobalData.PLAYER_MANAGER.loadItem(song, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                println("trackLoaded")
                audioManager.scheduler.play(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                println("playlistLoaded")
                playlist.tracks.forEach { audioManager.scheduler.play(it) }
            }

            override fun noMatches() {
                println("no matches")
            }

            override fun loadFailed(exception: FriendlyException?) {
                println("load failed")
            }
        })
    }

}