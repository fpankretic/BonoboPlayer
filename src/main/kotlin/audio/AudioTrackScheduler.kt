package audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.*

class AudioTrackScheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {

    private val queue: MutableList<AudioTrack> = Collections.synchronizedList(mutableListOf())

    fun play(track: AudioTrack): Boolean {
        return play(track, false)
    }

    fun play(track: AudioTrack, force: Boolean): Boolean {
        val playing = player.startTrack(track, !force)
        if (!playing) queue.add(track)
        return playing
    }

    fun skip(): Boolean {
        if (queue.isEmpty() && player.playingTrack != null) {
            player.playTrack(null)
            return false
        }

        return queue.isNotEmpty() && play(queue.removeAt(0), true)
    }

    fun clear() {
        queue.clear()
        player.playTrack(null)
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (endReason != null) {
            if (endReason.mayStartNext) {
                skip()
            }
        }
    }
}
