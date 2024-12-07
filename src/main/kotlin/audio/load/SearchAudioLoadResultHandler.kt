package audio.load

import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import mu.KotlinLogging
import util.chooseSongSelect
import util.defaultEmbed
import util.simpleMessageEmbed
import java.util.*
import kotlin.math.min

class SearchAudioLoadResultHandler(
    guildId: Snowflake
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.getAudio(guildId)

    override fun trackLoaded(track: AudioTrack?) {
        logger.info { "Should never get to trackLoaded!" }
        guildAudio.removeHandler(this)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult.not()) {
            logger.info { "Error while searching!" }
            return
        }


        val filteredTracks = playlist.tracks.distinctBy { it.info.title }
        val numberOfElements = min(5, filteredTracks.size)

        val resultString = filteredTracks
            .mapIndexed { index, audioTrack -> "${index + 1}. ${audioTrack.info.title}" }
            .take(numberOfElements)
            .joinToString("\n")

        val customId = UUID.randomUUID().toString().lowercase()
        guildAudio.sendMessageWithComponentAndTimeout(
            defaultEmbed()
                .title(playlist.name)
                .description(resultString)
                .build(),
            chooseSongSelect(filteredTracks.subList(0, numberOfElements), customId),
            customId
        )
        guildAudio.removeHandler(this)
    }

    override fun noMatches() {
        logger.info { "Found no matches." }
        guildAudio.sendMessage(simpleMessageEmbed("Found no matches.").build())
        guildAudio.removeHandler(this)
    }

    override fun loadFailed(exception: FriendlyException?) {
        logger.error { "Error while searching!" }
        guildAudio.sendMessage(simpleMessageEmbed("Error while searching!").build())
        guildAudio.removeHandler(this)
    }
}