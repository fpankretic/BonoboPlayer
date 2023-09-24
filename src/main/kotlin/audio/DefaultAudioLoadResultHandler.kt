package audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.spec.EmbedCreateSpec
import mu.KotlinLogging
import util.EmbedUtils

class DefaultAudioLoadResultHandler(
    guildId: Snowflake,
    val track: String
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.getAudio(guildId)

    override fun trackLoaded(track: AudioTrack) {
        logger.info { "Started loading track ${track.info.title}." }

        guildAudio.sendMessage(getTrackLoadedMessage(track))
        guildAudio.play(track)
        guildAudio.removeHandler(this)

        logger.info { "Finished loading track ${track.info.title}." }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        logger.info { "Started loading playlist ${playlist.name}." }

        if (playlist.isSearchResult) {
            trackLoaded(playlist.tracks[0])
            return
        }

        guildAudio.sendMessage(getPlaylistLoadedMessage(playlist))
        playlist.tracks.forEach { guildAudio.play(it) }
        guildAudio.removeHandler(this)

        logger.info { "Finished loading playlist ${playlist.name}." }
    }

    override fun noMatches() {
        guildAudio.removeHandler(this)
        logger.info { "Found no matches." }
    }

    override fun loadFailed(exception: FriendlyException?) {
        guildAudio.removeHandler(this)
        logger.info { "Load failed." }
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