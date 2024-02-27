package audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import discord4j.common.util.Snowflake
import discord4j.core.spec.EmbedCreateSpec
import mu.KotlinLogging
import util.EmbedUtils.Companion.bold
import util.EmbedUtils.Companion.defaultEmbed
import util.EmbedUtils.Companion.simpleMessageEmbed
import util.EmbedUtils.Companion.trackAsHyperLink
import java.util.*

class AudioTrackScheduler private constructor() : AudioEventAdapter() {

    private val logger = KotlinLogging.logger {}

    private val queue: MutableList<AudioTrack> = Collections.synchronizedList(mutableListOf())
    private lateinit var player: AudioPlayer
    private lateinit var guildId: Snowflake

    constructor(player: AudioPlayer, guildId: Snowflake) : this() {
        this.player = player
        this.guildId = guildId;
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        logger.info { "Now playing ${track!!.info.title} from ${track.info.uri}." }
        GuildManager.getAudio(guildId).sendMessage(onTrackStartMessage(track!!))
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        logger.info { "OnTrackEndCalled with endReason $endReason." }
        if (endReason != null && endReason.mayStartNext) {
            if (skip().not()) {
                GuildManager.getAudio(guildId).scheduleLeave()
            }
        }
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException?) {
        logger.info { "Track exception for ${track!!.info.title}." }
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        logger.info { "Track ${track!!.info.title} got stuck, skipping." }
    }

    fun play(track: AudioTrack): Boolean {
        return play(track, false)
    }

    private fun play(track: AudioTrack, force: Boolean): Boolean {
        val started = player.startTrack(track.makeClone(), !force)
        if (!started) {
            queue.add(track)
        }
        player.playingTrack.info

        if (started) {
            GuildManager.getAudio(guildId).cancelLeave()
        }
        return started
    }

    fun getQueue(): List<AudioTrack> {
        return Collections.unmodifiableList(queue)
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

        val track = queue.removeAt(position - 1)
        logger.info { "Removed ${track.info.title} from queue." }

        GuildManager.getAudio(guildId).sendMessage(trackSkippedMessage(track))
        return true
    }

    fun clearQueue() {
        queue.clear()
        player.playTrack(null)
    }

    fun clear() {
        queue.clear()
        player.playTrack(null)
    }

    fun currentSong(): Optional<AudioTrack> {
        return Optional.ofNullable(player.playingTrack)
    }

    fun destroy() {
        player.destroy()
        clear()
    }

    private fun isPlaying(): Boolean {
        return player.playingTrack != null
    }

    private fun onTrackStartMessage(track: AudioTrack): EmbedCreateSpec {
        return simpleMessageEmbed("Now playing: ${trackAsHyperLink(track)}").build()
    }

    private fun trackSkippedMessage(track: AudioTrack): EmbedCreateSpec {
        return defaultEmbed()
            .title("Removed from the queue")
            .description(bold(trackAsHyperLink(track)))
            .thumbnail(track.info.artworkUrl)
            .build()
    }

}
