package audio

import GlobalData
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import java.util.concurrent.ConcurrentHashMap

class GuildAudioManager private constructor() {
    companion object {
        private val MANAGERS: MutableMap<Snowflake, GuildAudioManager> = ConcurrentHashMap()

        @JvmStatic
        fun of(id: Snowflake, messageChannel: MessageChannel): GuildAudioManager {
            val manager = MANAGERS.computeIfAbsent(id) { GuildAudioManager(id, messageChannel) }
            manager.scheduler.messageChannel = messageChannel
            return manager
        }

        @JvmStatic
        fun of(id: Snowflake): GuildAudioManager {
            return MANAGERS[id]!!
        }
    }

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    lateinit var scheduler: AudioTrackScheduler
    lateinit var provider: LavaPlayerAudioProvider

    private constructor(guildId: Snowflake, messageChannel: MessageChannel) : this() {
        this.scheduler = AudioTrackScheduler(player, guildId, messageChannel)
        this.provider = LavaPlayerAudioProvider(player)
        player.addListener(scheduler)
    }

}