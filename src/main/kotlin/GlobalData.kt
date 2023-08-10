import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import io.sfrei.tracksearch.clients.MultiSearchClient
import io.sfrei.tracksearch.clients.setup.Client
import io.sfrei.tracksearch.clients.youtube.YouTubeClient

class GlobalData {
    companion object {
        @JvmField val PLAYER_MANAGER: AudioPlayerManager
        @JvmField val SEARCH_CLIENT: YouTubeClient

        init {
            PLAYER_MANAGER = DefaultAudioPlayerManager()
            SEARCH_CLIENT = YouTubeClient()

            PLAYER_MANAGER.configuration.setFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
            }

            AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER)
            AudioSourceManagers.registerLocalSource(PLAYER_MANAGER)
        }
    }
}