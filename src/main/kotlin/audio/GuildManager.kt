package audio

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import java.util.concurrent.ConcurrentHashMap

class GuildManager private constructor() {

    companion object {
        private val MANAGERS: MutableMap<Snowflake, GuildAudio> = ConcurrentHashMap()

        @JvmStatic
        fun getAudio(id: Snowflake, messageChannel: MessageChannel): GuildAudio {
            val guildAudio = MANAGERS.computeIfAbsent(id) { GuildAudio(messageChannel.client, id, messageChannel) }
            guildAudio.scheduler.messageChannel = messageChannel
            return guildAudio
        }

        @JvmStatic
        fun getAudio(id: Snowflake): GuildAudio {
            return MANAGERS[id]!!
        }

        @JvmStatic
        fun destroyAudio(id: Snowflake) {
            MANAGERS[id]!!.destroy()
            MANAGERS.remove(id)
        }
    }

}