import com.github.topi314.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.YoutubeSourceOptions
import dev.lavalink.youtube.clients.*
import util.EnvironmentManager
import util.EnvironmentValue.*
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager as YoutubeAudioSourceManagerDeprecated

object GlobalData {
    val PLAYER_MANAGER: AudioPlayerManager = DefaultAudioPlayerManager()

    init {
        PLAYER_MANAGER.configuration.setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
        PLAYER_MANAGER.configuration.isFilterHotSwapEnabled = true

        // Setup Youtube source
        val clients = arrayOf(
            MusicWithThumbnail(),
            WebWithThumbnail(),
            WebEmbeddedWithThumbnail(),
            MWebWithThumbnail(),
            TvHtml5EmbeddedWithThumbnail(),
            AndroidMusicWithThumbnail(),
            AndroidVrWithThumbnail()
        )

        var remoteCipherUrl = "http://localhost:12000"
        val youtubeSourceOptions = YoutubeSourceOptions().setRemoteCipher(remoteCipherUrl, null, null)
        val youtubeSource = YoutubeAudioSourceManager(youtubeSourceOptions, *clients)

        // Set PoToken and VisitorData
        if (EnvironmentManager.valueOf(PO_TOKEN).isEmpty() || EnvironmentManager.valueOf(VISITOR_DATA).isEmpty()) {
            val poToken = EnvironmentManager.valueOf(PO_TOKEN)
            val visitorData = EnvironmentManager.valueOf(VISITOR_DATA)
            WebWithThumbnail.setPoTokenAndVisitorData(poToken, visitorData)
            WebEmbeddedWithThumbnail.setPoTokenAndVisitorData(poToken, visitorData)
            MWebWithThumbnail.setPoTokenAndVisitorData(poToken, visitorData)
        }

        // Set refresh token
        if (EnvironmentManager.valueOf(REFRESH_TOKEN).isNotEmpty()) {
            youtubeSource.useOauth2(EnvironmentManager.valueOf(REFRESH_TOKEN), true)
        } else {
            youtubeSource.useOauth2(null, false)
        }

        // Set Spotify source
        val spotifySource = SpotifySourceManager(
            null,
            EnvironmentManager.valueOf(SPOTIFY_CLIENT_ID),
            EnvironmentManager.valueOf(SPOTIFY_CLIENT_SECRET),
            "HR",
            PLAYER_MANAGER
        )

        // Register sources
        PLAYER_MANAGER.registerSourceManagers(youtubeSource)
        PLAYER_MANAGER.registerSourceManager(spotifySource)

        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER, YoutubeAudioSourceManagerDeprecated::class.java)
        // AudioSourceManagers.registerLocalSource(PLAYER_MANAGER) TODO: Implement local source

        // Setup IPv6 rotator
        if (EnvironmentManager.valueOf(IPV6_ENABLED).toBoolean()) {
            val ipv6Block = Ipv6Block(EnvironmentManager.valueOf(IPV6_CIDR))
            val routePlanner = NanoIpRoutePlanner(listOf(ipv6Block), true)
            val rotator = YoutubeIpRotatorSetup(routePlanner)

            rotator.forConfiguration(youtubeSource.httpInterfaceManager, false)
                .withMainDelegateFilter(null)
                .setup()
        }
    }
}
