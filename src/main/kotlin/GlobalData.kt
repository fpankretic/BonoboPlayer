import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer

class GlobalData {

    companion object {
        @JvmField val PLAYER_MANAGER: AudioPlayerManager

        init {
            PLAYER_MANAGER = DefaultAudioPlayerManager()

            PLAYER_MANAGER.configuration.setFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
            }
            PLAYER_MANAGER.configuration.isFilterHotSwapEnabled = true

            AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER)
            AudioSourceManagers.registerLocalSource(PLAYER_MANAGER)
        }
    }

}