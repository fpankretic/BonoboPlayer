package audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import discord4j.common.util.Snowflake
import discord4j.core.spec.EmbedCreateSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import util.bold
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
import util.trackAsHyperLink
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference

data class RequestedBy(val user: String, val avatarUrl: String, val time: Instant)
data class SongRequest(val audioTrack: AudioTrack, val requestedBy: RequestedBy)

class AudioTrackScheduler private constructor() : AudioEventAdapter() {

    private val logger = KotlinLogging.logger {}

    private val queue: MutableList<SongRequest> = Collections.synchronizedList(mutableListOf())
    private var requestedBy: RequestedBy? = null
    private lateinit var player: AudioPlayer
    private lateinit var guildId: Snowflake
    private val repeating: AtomicReference<Boolean> = AtomicReference(false)

    constructor(player: AudioPlayer, guildId: Snowflake) : this() {
        this.player = player
        this.guildId = guildId;
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        logger.info { "Now playing ${track!!.info.title} from ${track.info.uri}." }
        GuildManager.getAudio(guildId).sendMessage(onTrackStartMessage(track!!))
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        logger.debug { "OnTrackEndCalled with endReason $endReason." }
        if (endReason != null && endReason.mayStartNext) {
            if (skip().not()) {
                GuildManager.getAudio(guildId).scheduleLeave()
            }
        }
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException?) {
        logger.error { "Exception occurred while playing: ${track!!.info.title}." }
        logger.debug { exception?.stackTrace }
        GuildManager.getAudio(guildId).sendMessage(exceptionOccurredMessage(track!!))
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        logger.error { "Track ${track!!.info.title} got stuck, skipping." }
    }

    fun play(songRequest: SongRequest): Boolean {
        return play(songRequest, false)
    }

    private fun play(songRequest: SongRequest, force: Boolean): Boolean {
        val track = songRequest.audioTrack

        val oldRequestedBy = requestedBy
        requestedBy = songRequest.requestedBy

        val started = player.startTrack(track.makeClone(), !force)

        if (!started) {
            queue.add(songRequest)
            requestedBy = oldRequestedBy
        } else {
            GuildManager.getAudio(guildId).cancelLeave()
        }

        return started
    }

    fun getQueue(): List<AudioTrack> {
        return Collections.unmodifiableList(queue.map { it.audioTrack })
    }

    fun skip(): Boolean {
        if (queue.isEmpty() && isPlaying()) {
            clear()
            return false
        }

        return queue.isNotEmpty() && play(queue.removeAt(0), true)
    }

    fun skipInQueue(position: Int): Boolean {
        if (queue.size < position) {
            return false
        }

        val track = queue.removeAt(position - 1).audioTrack
        logger.info { "Removed ${track.info.title} from queue." }

        GuildManager.getAudio(guildId).sendMessage(trackSkippedMessage(track))
        return true
    }

    fun skipTo(position: Int): Boolean {
        if (position < 1 || position > queue.size) {
            return false
        }

        for (i in 1 until position) {
            val track = queue.removeAt(0).audioTrack
            logger.info { "Removed ${track.info.title} from queue." }
        }
        skip()

        GuildManager.getAudio(guildId).sendMessage(nextSongMessage(queue.first().audioTrack))

        return true
    }

    fun clearQueue() {
        queue.clear()
    }

    private fun clear() {
        queue.clear()
        player.playTrack(null)
    }

    fun currentSong(): Optional<AudioTrack> {
        return Optional.ofNullable(player.playingTrack)
    }

    fun requestedBy(): RequestedBy? {
        return requestedBy
    }

    fun destroy() {
        player.destroy()
        clear()
    }

    private fun isPlaying(): Boolean {
        return player.playingTrack != null
    }

    private fun onTrackStartMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .description("Now playing: ${trackAsHyperLink(track)}")
            .footer("Requested by ${requestedBy!!.user}", requestedBy!!.avatarUrl)
            .timestamp(requestedBy!!.time)
            .build()
    }

    private fun trackSkippedMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .title("Removed from the queue")
            .description(bold(trackAsHyperLink(track)))
            .thumbnail(track.info.artworkUrl)
            .build()
    }

    private fun nextSongMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .title("Next in queue")
            .description(bold(trackAsHyperLink(track)))
            .thumbnail(track.info.artworkUrl)
            .build()
    }

    private fun exceptionOccurredMessage(track: AudioTrack): EmbedCreateSpec {
        return simpleMessageEmbed("An error occurred while playing ${track.info.title}.")
    }

}
