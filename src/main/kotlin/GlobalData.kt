import com.github.topi314.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.*
import env.EnvironmentManager
import env.EnvironmentValue.*
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
            val poToken = EnvironmentManager.get(PO_TOKEN)
            val visitorData = EnvironmentManager.get(VISITOR_DATA)
            WebWithThumbnail.setPoTokenAndVisitorData(poToken, visitorData)

            val clients = arrayOf(
                MusicWithThumbnail(),
                WebWithThumbnail(),
                WebEmbeddedWithThumbnail(),
                MWebWithThumbnail(),
                TvHtml5EmbeddedWithThumbnail(),
                AndroidVrWithThumbnail(),
                AndroidMusicWithThumbnail(),
            )

            val youtubeSource = YoutubeAudioSourceManager(*clients)
            youtubeSource.useOauth2(EnvironmentManager.get(REFRESH_TOKEN), true)
            PLAYER_MANAGER.registerSourceManagers(youtubeSource)

            val spotifySource = SpotifySourceManager(
                null,
                EnvironmentManager.get(SPOTIFY_CLIENT_ID),
                EnvironmentManager.get(SPOTIFY_CLIENT_SECRET),
                "HR",
                PLAYER_MANAGER
            )
            PLAYER_MANAGER.registerSourceManager(spotifySource)

            AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER, YoutubeAudioSourceManagerDeprecated::class.java)
            AudioSourceManagers.registerLocalSource(PLAYER_MANAGER)

            // Setup IPv6 rotator
            if (EnvironmentManager.get(IPV6_ENABLED).toBoolean()) {
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
