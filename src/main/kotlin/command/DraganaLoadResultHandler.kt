package command

import audio.GuildAudio
import audio.GuildManager
import audio.RequestedBy
import audio.SongRequest
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

class DraganaLoadResultHandler(
    private val guildId: Snowflake,
    private val authorName: String,
    private val avatarUrl: String,
    private val isFirst: Boolean
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.audio(guildId)

    override fun trackLoaded(loadedTrack: AudioTrack) {
        guildAudio.removeHandler(this)
        guildAudio.play(
            SongRequest(
                loadedTrack,
                RequestedBy(authorName, avatarUrl, Instant.now())
            )
        )
        logger.info { "Loaded Dragana track: ${loadedTrack.info.title}" }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        guildAudio.removeHandler(this)
        if (playlist.isSearchResult) {
            trackLoaded(playlist.tracks[0])
            return
        }
        playlist.tracks.firstOrNull()?.let { track ->
            guildAudio.play(
                SongRequest(track, RequestedBy(authorName, avatarUrl, Instant.now()))
            )
        }
        logger.info { "Loaded Dragana playlist: ${playlist.name}" }
    }

    override fun noMatches() {
        guildAudio.removeHandler(this)
        logger.warn { "No matches found for Dragana song." }
    }

    override fun loadFailed(exception: FriendlyException?) {
        guildAudio.removeHandler(this)
        logger.error { "Failed to load Dragana track." }
        exception?.let { logger.error { it.stackTraceToString() } }
    }
}
