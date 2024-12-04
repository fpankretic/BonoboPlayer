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
import mu.KotlinLogging
import util.EmbedUtils
import util.EmbedUtils.Companion.bold
import util.EmbedUtils.Companion.defaultEmbed
import util.EmbedUtils.Companion.trackAsHyperLink
import java.time.Instant

class DefaultAudioLoadResultHandler(
    private val guildId: Snowflake,
    private val author: User,
    private val track: String,
    private val retried: Boolean = false
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.getAudio(guildId)

    override fun trackLoaded(track: AudioTrack) {
        logger.info { "Started loading track ${track.info.title}." }

        if (guildAudio.getQueue().isNotEmpty() || guildAudio.isSongLoaded()) {
            guildAudio.sendMessage(getTrackLoadedMessage(track))
        }
        guildAudio.play(
            SongRequest(
                track,
                RequestedBy(author.globalName.orElse(author.username), author.avatarUrl, Instant.now())
            )
        )

        logger.info { "Loaded track ${track.info.title}." }
        guildAudio.removeHandler(this)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {
            trackLoaded(playlist.tracks[0])
            return
        }

        logger.info { "Started loading playlist ${playlist.name}." }

        guildAudio.sendMessage(getPlaylistLoadedMessage(playlist))
        playlist.tracks.forEach {
            guildAudio.play(
                SongRequest(
                    it,
                    RequestedBy(author.globalName.orElse(author.username), author.avatarUrl, Instant.now())
                )
            )
        }

        logger.info { "Finished loading playlist ${playlist.name}." }
        guildAudio.removeHandler(this)
    }

    override fun noMatches() {
        logger.info { "Found no matches for: $track." }
        guildAudio.sendMessage(getNoMatchesMessage())
        guildAudio.removeHandler(this)
    }

    override fun loadFailed(exception: FriendlyException?) {
        guildAudio.removeHandler(this)
        if (retried) {
            logger.error { "Failed to load track: $track." }
            if (exception != null) {
                logger.error { exception.stackTraceToString() }
            }
            guildAudio.sendMessage(getFailedToLoadMessage())
        } else {
            logger.info { "Retrying to load track: $track." }
            guildAudio.addHandler(DefaultAudioLoadResultHandler(guildId, author, track, true), track)
        }
    }

    private fun getTrackLoadedMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbed()
            .title("Added to the queue")
            .description(bold(trackAsHyperLink(track)))
            .thumbnail(track.info.artworkUrl)
            .build()
    }

    private fun getPlaylistLoadedMessage(playlist: AudioPlaylist): EmbedCreateSpec {
        return defaultEmbed()
            .title("Added playlist to the queue")
            .description(bold(trackAsHyperLink(playlist)))
            .thumbnail(playlist.tracks[0].info.artworkUrl)
            .addField("Songs in playlist: ${playlist.tracks.size}", "", true)
            .build()
    }

    private fun getNoMatchesMessage(): EmbedCreateSpec {
        return EmbedUtils.simpleMessageEmbed("Found no matches.").build()
    }

    private fun getFailedToLoadMessage(): EmbedCreateSpec {
        return EmbedUtils.simpleMessageEmbed("Failed to load track.").build()
    }
}