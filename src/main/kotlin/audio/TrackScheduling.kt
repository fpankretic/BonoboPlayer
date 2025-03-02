package audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.*

interface TrackScheduling {
    // Lifecycle methods
    fun destroy()

    // Playing methods
    fun play(songRequest: SongRequest): Boolean
    fun next(): Boolean

    // Metadata methods
    fun currentSong(): Optional<AudioTrack>
    fun requestedBy(): RequestedBy?

    // Modification methods
    fun skipInQueue(position: Int): Boolean
    fun skipTo(position: Int): Boolean
    fun moveSong(from: Int, to: Int): Boolean

    // Queue methods
    fun getQueueCopy(): List<AudioTrack>
    fun clearQueue()
    fun changeQueueRepeatStatus()
    fun shuffleQueue(): Boolean

    // Status methods
    fun isQueueEmpty(): Boolean
    fun isSongLoaded(): Boolean
    fun isRepeating(): Boolean

}