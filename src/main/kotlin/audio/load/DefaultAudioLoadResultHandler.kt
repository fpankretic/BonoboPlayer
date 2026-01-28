package audio.load

import audio.GuildAudio
import audio.GuildManager
import audio.RequestedBy
import audio.SongRequest
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.spec.EmbedCreateSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import util.bold
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
import util.trackAsHyperLink
import java.time.Instant

class DefaultAudioLoadResultHandler(
    private val guildId: Snowflake,
    private val author: User,
    private val track: String,
    private val retried: Boolean = false,
    private val retriedSearch: Boolean = false,
    private val playlistMode: Boolean = false
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.audio(guildId)

    override fun trackLoaded(loadedTrack: AudioTrack) {
        guildAudio.removeHandler(this)

        if (guildAudio.isQueueEmpty().not() || guildAudio.isSongLoaded()) {
            guildAudio.sendMessage(getTrackLoadedMessage(loadedTrack))
        }

        guildAudio.play(
            SongRequest(
                loadedTrack,
                RequestedBy(author.globalName.orElse(author.username), author.avatarUrl, Instant.now())
            )
        )

        logger.info { "Loaded track ${loadedTrack.info.title}." }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        guildAudio.removeHandler(this)

        if (playlistMode.not() and playlist.tracks.isNotEmpty() or playlist.isSearchResult) {
            if (playlistMode.not()) {
                guildAudio.sendMessage(getUsePlaylistMessage())
            }
            trackLoaded(playlist.tracks[0])
            return
        }

        guildAudio.sendMessage(getPlaylistLoadedMessage(playlist))
        playlist.tracks.forEach {
            val authorName = author.globalName.orElse(author.username)
            guildAudio.play(SongRequest(it, RequestedBy(authorName, author.avatarUrl, Instant.now())))
        }

        logger.info { "Loaded playlist ${playlist.name}." }
    }

    override fun noMatches() {
        guildAudio.removeHandler(this)
        logger.debug { "Found no matches for: $track." }
        when {
            retriedSearch.not() and track.contains("spsearch") -> handleSearchFail()
            else -> handleNoMatchesFail()
        }
    }

    override fun loadFailed(exception: FriendlyException?) {
        guildAudio.removeHandler(this)
        when {
            retried -> handleSecondLoadFail(exception)
            track.contains("spsearch") -> handleSpotifyLoadFail()
            else -> handleGenericLoadFail()
        }
    }

    private fun handleSecondLoadFail(exception: FriendlyException?) {
        logger.error { "Failed to load track: $track." }
        if (exception != null) {
            logger.error { exception.stackTraceToString() }
        }
        guildAudio.sendMessage(simpleMessageEmbed("Failed to load track."))
    }

    private fun handleSpotifyLoadFail() {
        val newTrack = track.replace("spsearch", "ytsearch")
        logger.info { "Retrying to load track ${newTrack.replace("ytsearch: ", "")} with youtube." }
        guildAudio.addHandler(DefaultAudioLoadResultHandler(guildId, author, newTrack, true), newTrack)
    }

    private fun handleGenericLoadFail() {
        logger.info { "Retrying to load track: $track." }
        guildAudio.addHandler(DefaultAudioLoadResultHandler(guildId, author, track, true), track)
    }

    private fun handleNoMatchesFail() {
        val trackName = if (!track.contains("search:")) track else track.dropWhile { it != ' ' }.trim()
        guildAudio.sendMessage(simpleMessageEmbed("Found no matches for $trackName."))
    }

    private fun handleSearchFail() {
        val newTrack = track.replace("spsearch", "ytsearch")
        logger.info { "Searching again with $newTrack." }
        guildAudio.addHandler(DefaultAudioLoadResultHandler(guildId, author, newTrack, retried, true), newTrack)
    }

    private fun getTrackLoadedMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .title("Added to the queue")
            .description(bold(trackAsHyperLink(track)))
            .thumbnail(track.info.artworkUrl)
            .build()
    }

    private fun getPlaylistLoadedMessage(playlist: AudioPlaylist): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .title("Added playlist to the queue")
            .description(bold(trackAsHyperLink(playlist)))
            .thumbnail(playlist.tracks[0].info.artworkUrl)
            .addField("Songs in playlist: ${playlist.tracks.size}", "", true)
            .build()
    }

    private fun getUsePlaylistMessage(): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .title("Use Playlist Mode")
            .description("To add the whole playlist, use the playlist command.")
            .build()
    }
}