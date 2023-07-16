package audio

import GlobalData
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.common.util.Snowflake
import java.util.concurrent.ConcurrentHashMap

class GuildAudioManager private constructor() {
    companion object {
        private val MANAGERS: MutableMap<Snowflake, GuildAudioManager> = ConcurrentHashMap()

        @JvmStatic
        fun of(id: Snowflake): GuildAudioManager {
            return MANAGERS.computeIfAbsent(id) { GuildAudioManager() }
        }
    }

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    val scheduler: AudioTrackScheduler = AudioTrackScheduler(player)
    val provider: LavaPlayerAudioProvider = LavaPlayerAudioProvider(player)

    init {
        player.addListener(scheduler)
    }

}