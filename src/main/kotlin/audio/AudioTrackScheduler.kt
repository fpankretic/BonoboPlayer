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

class AudioTrackScheduler private constructor() : AudioEventAdapter() {

    private val logger = KotlinLogging.logger {}

    private lateinit var player: AudioPlayer
    private lateinit var guildId: Snowflake

    private val queue: MutableList<SongRequest> = Collections.synchronizedList(mutableListOf())
    private val queueType: AtomicReference<QueueType> = AtomicReference(QueueType.NORMAL)

    private var currentSongRequest: SongRequest? = null

    constructor(player: AudioPlayer, guildId: Snowflake) : this() {
        this.player = player
        this.guildId = guildId;
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        val guildName = GuildManager.audio(guildId).getGuildName()
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

    fun play(songRequest: SongRequest): Boolean {
        return play(songRequest, false)
    }

    fun getQueueCopy(): List<AudioTrack> {
        return Collections.unmodifiableList(queue.map { it.audioTrack })
    }

    fun getQueueSize(): Int {
        return queue.size
    }

    fun isQueueEmpty(): Boolean {
        return queue.isEmpty()
    }

    fun next(): Boolean {
        if (queue.isEmpty()) {
            clearQueueAndTrack()
            return false
        }

        return play(queue.removeAt(0), true)
    }

    fun skipInQueue(position: Int): Boolean {
        if (queue.size < position) {
            return false
        }

        val track = queue.removeAt(position - 1).audioTrack
        logger.info { "Removed ${track.info.title} from queue." }

        GuildManager.audio(guildId).sendMessage(trackSkippedMessage(track))
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
        next()

        GuildManager.audio(guildId).sendMessage(nextSongMessage(queue.first().audioTrack))

        return true
    }

    fun clearQueue() {
        queue.clear()
    }

    fun currentSong(): Optional<AudioTrack> {
        return Optional.ofNullable(player.playingTrack)
    }

    fun requestedBy(): RequestedBy? {
        return currentSongRequest?.requestedBy
    }

    fun destroy() {
        player.destroy()
        clearQueueAndTrack()
    }

    fun changeQueueStatus() {
        if (queueType.get() == QueueType.NORMAL) {
            queueType.set(QueueType.REPEAT)
        } else {
            queueType.set(QueueType.NORMAL)
        }
    }

    fun repeating(): Boolean {
        return queueType.get() == QueueType.REPEAT
    }

    fun shuffleQueue(): Boolean {
        if (queue.size < 2) {
            return false
        }

        queue.shuffle()
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

    private fun exceptionOccurredMessage(track: AudioTrack): EmbedCreateSpec {
        return simpleMessageEmbed("An error occurred while playing ${track.info.title}.")
    }

}
