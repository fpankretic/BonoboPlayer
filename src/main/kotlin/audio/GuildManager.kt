package audio

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
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

        fun audio(id: Snowflake): GuildAudio {
            return MANAGERS[id] ?: throw IllegalStateException("Audio does not exist.")
        }

        fun audioMono(id: Snowflake): Mono<GuildAudio> {
            return mono { MANAGERS[id] }
        }

        fun audioExists(id: Snowflake): Boolean {
            return MANAGERS.containsKey(id)
        }

        fun destroyAudio(id: Snowflake) {
            MANAGERS[id] ?: return

            val guildName = MANAGERS[id]!!.guildName
            MANAGERS[id]!!.destroy()
            MANAGERS.remove(id)

            logger.info { "Audio destroyed for guild: ${guildName}." }
        }

    }

}