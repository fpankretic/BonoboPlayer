package audio

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class GuildManager private constructor() {

    companion object {

        private val logger = KotlinLogging.logger {}
        private val MANAGERS: MutableMap<Snowflake, GuildAudio> = ConcurrentHashMap()

        @JvmStatic
        fun createAudio(client: GatewayDiscordClient, id: Snowflake, messageChannelId: Snowflake): GuildAudio {
            val guildAudio = MANAGERS.computeIfAbsent(id) { GuildAudio(client, id) }
            guildAudio.setMessageChannelId(messageChannelId)
            return guildAudio
        }

        @JvmStatic
        fun getAudio(id: Snowflake): GuildAudio {
            return MANAGERS[id]!!
        }

        @JvmStatic
        fun audioExists(id: Snowflake): Boolean {
            return MANAGERS[id] != null
        }

        @JvmStatic
        fun destroyAudio(id: Snowflake) {
            MANAGERS[id] ?: return

            logger.info { "Trying to destroy audio." }
            MANAGERS[id]?.destroy()
            MANAGERS.remove(id)
            logger.info { "Audio destroyed." }
        }

    }

}