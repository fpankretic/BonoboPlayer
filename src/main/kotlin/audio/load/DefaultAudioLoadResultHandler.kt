package audio.load

import audio.GuildAudio
import audio.GuildManager
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
    guildId: Snowflake,
    private val author: User
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.getAudio(guildId)

    override fun trackLoaded(track: AudioTrack) {
        logger.info { "Started loading track ${track.info.title}." }

        if (guildAudio.getQueue().isNotEmpty() || guildAudio.isSongLoaded()) {
            guildAudio.sendMessage(getTrackLoadedMessage(track))
        }
        guildAudio.play(track)

        logger.info { "Finished loading track ${track.info.title}." }
        guildAudio.removeHandler(this)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {
            trackLoaded(playlist.tracks[0])
            return
        }

        logger.info { "Started loading playlist ${playlist.name}." }

        guildAudio.sendMessage(getPlaylistLoadedMessage(playlist))
        playlist.tracks.forEach { guildAudio.play(it) }

        logger.info { "Finished loading playlist ${playlist.name}." }
        guildAudio.removeHandler(this)
    }

    override fun noMatches() {
        logger.info { "Found no matches." }
        guildAudio.sendMessage(getNoMatchesMessage())
        guildAudio.removeHandler(this)
    }

    override fun loadFailed(exception: FriendlyException?) {
        guildAudio.removeHandler(this)
        logger.info { "Load failed." }
    }

    private fun getTrackLoadedMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbed()
            .title("Added to the queue")
            .description(bold(trackAsHyperLink(track)))
            .thumbnail(track.info.artworkUrl)
            .footer("Requested by ${author.globalName.orElse(author.username)}", author.avatarUrl)
            .timestamp(Instant.now())
            .build()
    }

    private fun getPlaylistLoadedMessage(playlist: AudioPlaylist): EmbedCreateSpec {
        return defaultEmbed()
            .title("Added playlist to the queue")
            .description(bold(trackAsHyperLink(playlist)))
            .thumbnail(playlist.tracks[0].info.artworkUrl)
            .addField("Songs in playlist: ${playlist.tracks.size}", "", true)
            .footer("Requested by ${author.globalName.orElse(author.username)}", author.avatarUrl)
            .timestamp(Instant.now())
            .build()
    }

    private fun getNoMatchesMessage(): EmbedCreateSpec {
        return EmbedUtils.simpleMessageEmbed("Found no matches.").build()
    }
}