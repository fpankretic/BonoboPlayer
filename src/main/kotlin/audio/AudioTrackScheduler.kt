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
enum class QueueType { NORMAL, REPEAT }

class AudioTrackScheduler(
    private var player: AudioPlayer,
    private var guildId: Snowflake
): AudioEventAdapter(), TrackScheduling {
    private val logger = KotlinLogging.logger {}

    private var currentSongRequest: SongRequest? = null

    private val queue = Collections.synchronizedList(mutableListOf<SongRequest>())
    private val queueType = AtomicReference(QueueType.NORMAL)

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        val guildName = GuildManager.audio(guildId).guildName
        logger.info { "Now playing ${track!!.info.title} from ${track.info.uri} in guild $guildName." }
        GuildManager.audio(guildId).sendMessage(onTrackStartMessage(track!!))
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        logger.debug { "OnTrackEndCalled with endReason $endReason." }
        if (endReason != null && endReason.mayStartNext) {
            val started = when (queueType.get()) {
                QueueType.NORMAL -> next()
                QueueType.REPEAT -> {
                    queue.addFirst(currentSongRequest!!)
                    next()
                }

                null -> throw IllegalStateException("QueueType is null.")
            }

            if (started.not()) {
                GuildManager.audio(guildId).scheduleLeave()
            }
        }
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException?) {
        logger.error { "Exception occurred while playing: ${track!!.info.title}." }
        logger.debug { exception?.stackTrace }
        GuildManager.audio(guildId).sendMessage(exceptionOccurredMessage(track!!))
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        logger.error { "Track ${track!!.info.title} got stuck, skipping." }
    }

    override fun play(songRequest: SongRequest) = play(songRequest, false)

    override fun getQueueCopy(): List<AudioTrack> = Collections.unmodifiableList(queue.map { it.audioTrack })

    override fun isQueueEmpty() = queue.isEmpty()

    override fun next(): Boolean {
        if (queue.isEmpty()) {
            clearQueueAndTrack()
            return false
        }

        return play(queue.removeAt(0), true)
    }

    override fun skipInQueue(position: Int): Boolean {
        if (position !in 1..queue.size) {
            return false
        }

        val track = queue.removeAt(position - 1).audioTrack
        logger.info { "Removed ${track.info.title} from queue." }

        GuildManager.audio(guildId).sendMessage(trackSkippedMessage(track))
        return true
    }

    override fun skipTo(position: Int): Boolean {
        if (position !in 1..queue.size) {
            return false
        }

        repeat(position - 1) {
            val track = queue.removeAt(0).audioTrack
            logger.info { "Removed ${track.info.title} from queue." }
        }
        next()

        GuildManager.audio(guildId).sendMessage(nextSongMessage(queue.first().audioTrack))

        return true
    }

    override fun clearQueue() = queue.clear()

    override fun currentSong() = Optional.ofNullable(player.playingTrack)

    override fun isSongLoaded() = player.playingTrack != null

    override fun requestedBy() = currentSongRequest?.requestedBy

    override fun destroy() {
        player.destroy()
        clearQueueAndTrack()
    }

    override fun changeQueueRepeatStatus() {
        if (queueType.get() == QueueType.NORMAL) {
            queueType.set(QueueType.REPEAT)
        } else {
            queueType.set(QueueType.NORMAL)
        }
    }

    override fun isRepeating() = queueType.get() == QueueType.REPEAT

    override fun shuffleQueue(): Boolean {
        if (queue.size < 2) {
            return false
        }

        queue.shuffle()
        return true
    }

    override fun moveSong(from: Int, to: Int): Boolean {
        if (from !in 1..queue.size || to !in 1..queue.size || from == to) {
            return false
        }

        val songRequest = queue.removeAt(from - 1)
        queue.add(to - 1, songRequest)
        return true
    }

    private fun clearQueueAndTrack() {
        queue.clear()
        player.playTrack(null)
    }

    private fun play(songRequest: SongRequest, force: Boolean): Boolean {
        val track = songRequest.audioTrack

        val oldSongRequest = currentSongRequest ?: songRequest
        currentSongRequest = songRequest

        val started = player.startTrack(track.makeClone(), !force)

        if (!started) {
            queue.add(songRequest)
            currentSongRequest = oldSongRequest
        } else {
            GuildManager.audio(guildId).cancelLeave()
        }

        return started
    }

    private fun onTrackStartMessage(track: AudioTrack): EmbedCreateSpec {
        val requestedBy = requestedBy()!!
        return defaultEmbedBuilder()
            .description("Now playing: ${trackAsHyperLink(track)}")
            .footer("Requested by ${requestedBy.user}", requestedBy.avatarUrl)
            .timestamp(requestedBy.time)
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

    private fun exceptionOccurredMessage(track: AudioTrack) =
        simpleMessageEmbed("An error occurred while playing ${track.info.title}.")
}
