package audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import java.util.*

class AudioTrackScheduler private constructor() : AudioEventAdapter() {

    private val queue: MutableList<AudioTrack> = Collections.synchronizedList(mutableListOf())
    private lateinit var player: AudioPlayer
    private lateinit var guildId: Snowflake

    lateinit var messageChannel: MessageChannel

    constructor(player: AudioPlayer, guildId: Snowflake, messageChannel: MessageChannel) : this() {
        this.player = player
        this.guildId = guildId;
        this.messageChannel = messageChannel
    }

    fun getQueue(): List<AudioTrack> {
        return Collections.unmodifiableList(queue)
    }

    fun play(track: AudioTrack): Boolean {
        return play(track, false)
    }

    private fun play(track: AudioTrack, force: Boolean): Boolean {
        val playing = player.startTrack(track, !force)
        if (playing) {
            println("Now playing ${track.info.title} from ${track.info.uri}")
            messageChannel.createMessage("Now playing: ${track.info.title}").block()
        } else {
            queue.add(track)
        }
        return playing
    }

    fun skip(): Boolean {
        if (queue.isEmpty() && isPlaying()) {
            player.playTrack(null)
            return false
        }

        return queue.isNotEmpty() && play(queue.removeAt(0), true)
    }

    fun clear() {
        queue.clear()
        player.playTrack(null)
    }

    fun isPlaying(): Boolean {
        return player.playingTrack != null
    }

    fun currentSong(): Optional<AudioTrack> {
        return Optional.ofNullable(player.playingTrack)
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        println("onTrackEndCalled with endReason $endReason")
        /*if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            player!!.startTrack(track, true)
        } else*/ if (endReason != null && endReason.mayStartNext) {
            if (queue.isEmpty()) {
                println("leaving...")
                messageChannel.client.voiceConnectionRegistry.getVoiceConnection(guildId)
                    .flatMap { it.disconnect() }
                    .block()
                GuildAudioManager.destroy(guildId)
            } else {
                skip()
            }
        }
    }
}
