package audio

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class GuildManager private constructor() {

    companion object {

        private val logger = KotlinLogging.logger {}
        private val MANAGERS: MutableMap<Snowflake, GuildAudio> = ConcurrentHashMap()

        fun createAudio(client: GatewayDiscordClient, id: Snowflake, messageChannelId: Snowflake): GuildAudio {
            val guildAudio = MANAGERS.computeIfAbsent(id) { GuildAudio(client, id) }
            guildAudio.setMessageChannelId(messageChannelId)
            return guildAudio
        }

        fun getAudio(id: Snowflake): GuildAudio {
            return MANAGERS[id] ?: throw IllegalStateException("Audio does not exist.")
        }

        fun audioExists(id: Snowflake): Boolean {
            return MANAGERS.containsKey(id)
        }

        fun destroyAudio(id: Snowflake) {
            MANAGERS[id] ?: return
            MANAGERS[id]?.destroy()
            MANAGERS.remove(id)
            logger.info { "Audio destroyed." }
        }

    }

}