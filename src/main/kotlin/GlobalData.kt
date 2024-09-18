import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.*
import dev.lavalink.youtube.clients.skeleton.Client
import env.EnvironmentManager
import env.EnvironmentValue.IPV6_CIDR
import env.EnvironmentValue.IPV6_ENABLED
import mu.KotlinLogging
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager as YoutubeAudioSourceManagerDeprecated

class GlobalData {

    companion object {
        @JvmField
        val PLAYER_MANAGER: AudioPlayerManager

        private val logger = KotlinLogging.logger {}

        init {
            PLAYER_MANAGER = DefaultAudioPlayerManager()

            PLAYER_MANAGER.configuration.setFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
            }
            PLAYER_MANAGER.configuration.isFilterHotSwapEnabled = true

            // YoutubeAudioSourceManager is deprecated, use youtube-source instead
            val clients = arrayOf<Client>(
                MusicWithThumbnail(),
                WebWithThumbnail(),
                AndroidWithThumbnail(),
                TvHtml5EmbeddedWithThumbnail(),
                AndroidMusicWithThumbnail(),
                AndroidTestsuiteWithThumbnail(),
                AndroidLiteWithThumbnail(),
                IosWithThumbnail(),
                MediaConnectWithThumbnail()
            )
            val youtubeSource = YoutubeAudioSourceManager(*clients)
            PLAYER_MANAGER.registerSourceManagers(youtubeSource)

            AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER, YoutubeAudioSourceManagerDeprecated::class.java)
            AudioSourceManagers.registerLocalSource(PLAYER_MANAGER)

            // Setup IPv6 rotator
            if (EnvironmentManager.get(IPV6_ENABLED).equals("true", ignoreCase = true)) {
                logger.info { "Setting up IPv6 rotator" }

                val ipv6Block = Ipv6Block(EnvironmentManager.get(IPV6_CIDR))
                val routePlanner = NanoIpRoutePlanner(listOf(ipv6Block), true)
                val rotator = YoutubeIpRotatorSetup(routePlanner)

                rotator.forConfiguration(youtubeSource.httpInterfaceManager, false)
                    .withMainDelegateFilter(null)
                    .setup()
            }
        }
    }

}
