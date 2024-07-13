package audio.handler

import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import mu.KotlinLogging
import util.EmbedUtils
import kotlin.math.min

class SearchAudioLoadResultHandler(
    guildId: Snowflake
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.getAudio(guildId)

    override fun trackLoaded(track: AudioTrack?) {
        logger.info { "Should never get to trackLoaded!" }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult.not()) {
            logger.info { "Error while searching!" }
            return
        }

        val results = playlist.tracks
            .mapIndexed { index, audioTrack -> "${index + 1}. ${audioTrack.info.title}" }
            .take(10)
            .joinToString("\n")

        val numberOfElements = min(10, playlist.tracks.size)
        guildAudio.sendMessageWithComponentAndTimeout(
            EmbedUtils.defaultEmbed()
                .title(playlist.name)
                .description(results)
                .build(),
            EmbedUtils.chooseSongSelect(playlist.tracks.subList(0, numberOfElements))
        )
    }

    override fun noMatches() {
        logger.info { "Found no matches." }
        guildAudio.sendMessage(EmbedUtils.simpleMessageEmbed("Found no matches.").build())
    }

    override fun loadFailed(exception: FriendlyException?) {
        logger.error { "Error while searching!" }
        guildAudio.sendMessage(EmbedUtils.simpleMessageEmbed("Error while searching!").build())
    }
}