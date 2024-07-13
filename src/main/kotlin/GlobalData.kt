import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.AndroidWithThumbnail
import dev.lavalink.youtube.clients.MusicWithThumbnail
import dev.lavalink.youtube.clients.TvHtml5EmbeddedWithThumbnail
import dev.lavalink.youtube.clients.WebWithThumbnail
import dev.lavalink.youtube.clients.skeleton.Client
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager as YoutubeAudioSourceManagerDeprecated

class GlobalData {

    companion object {
        @JvmField
        val PLAYER_MANAGER: AudioPlayerManager

        init {
            PLAYER_MANAGER = DefaultAudioPlayerManager()

            PLAYER_MANAGER.configuration.setFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
            }
            PLAYER_MANAGER.configuration.isFilterHotSwapEnabled = true

            // YoutubeAudioSourceManager is deprecated, use youtube-source instead
            val clients = arrayOf<Client>(
                MusicWithThumbnail(), WebWithThumbnail(), AndroidWithThumbnail(), TvHtml5EmbeddedWithThumbnail()
            )
            val youtubeSource = YoutubeAudioSourceManager(*clients)
            PLAYER_MANAGER.registerSourceManagers(youtubeSource)

            AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER, YoutubeAudioSourceManagerDeprecated::class.java)
            AudioSourceManagers.registerLocalSource(PLAYER_MANAGER)

        }
    }

}