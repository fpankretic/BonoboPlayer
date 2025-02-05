package audio.load

import audio.GuildAudio
import audio.GuildManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import io.github.oshai.kotlinlogging.KotlinLogging
import util.bold
import util.chooseSongButtons
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
import kotlin.math.min

class SearchAudioLoadResultHandler(
    private val guildId: Snowflake
) : AudioLoadResultHandler {

    private val logger = KotlinLogging.logger {}
    private val guildAudio: GuildAudio = GuildManager.audio(guildId)

    override fun trackLoaded(track: AudioTrack?) {
        guildAudio.removeHandler(this)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        guildAudio.removeHandler(this)

        if (playlist.isSearchResult.not()) {
            logger.error { "Error while searching!" }
            return
        }

        val filteredTracks = playlist.tracks.distinctBy { it.info.title }
        val numberOfElements = min(5, filteredTracks.size)

        val resultString = filteredTracks
            .mapIndexed { index, audioTrack -> "${bold((index + 1).toString())}. ${audioTrack.info.title}" }
            .take(numberOfElements)
            .joinToString("\n")

        val customId = guildId.toString()
        guildAudio.sendMessageWithComponentAndTimeout(
            defaultEmbedBuilder()
                .title(playlist.name)
                .description(resultString)
                .build(),
            chooseSongButtons(filteredTracks.subList(0, numberOfElements), customId),
            customId
        )
    }

    override fun noMatches() {
        guildAudio.removeHandler(this)
        logger.info { "Found no matches." }
        guildAudio.sendMessage(simpleMessageEmbed("Found no matches."))
    }

    override fun loadFailed(exception: FriendlyException?) {
        guildAudio.removeHandler(this)
        logger.error { "Error while searching!" }
        guildAudio.sendMessage(simpleMessageEmbed("Error while searching!"))
    }
}