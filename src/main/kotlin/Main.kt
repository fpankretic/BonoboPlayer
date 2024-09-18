import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import env.EnvironmentManager
import env.EnvironmentValue
import env.EnvironmentValue.IPV6_ENABLED
import env.EnvironmentValue.MAINTENANCE
import handler.MessageCreatedHandler
import handler.VoiceStateUpdatedHandler
import mu.KotlinLogging

fun main() {
    val logger = KotlinLogging.logger {}

    if (EnvironmentManager.get(MAINTENANCE).toBoolean()) {
        logger.info { "Bot is in maintenance mode." }
    }

    if (EnvironmentManager.get(IPV6_ENABLED).toBoolean()) {
        logger.info { "Bot is in IPv6 mode." }
    }

    val client = DiscordClient.create(EnvironmentManager.get(EnvironmentValue.DISCORD_API_TOKEN))
    val gateway = client.login().block() ?: return

    val messageCreatedHandler = MessageCreatedHandler()
    val voiceStateUpdatedHandler = VoiceStateUpdatedHandler()

    gateway.on(MessageCreateEvent::class.java) { messageCreatedHandler.handle(it) }.subscribe()
    gateway.on(VoiceStateUpdateEvent::class.java) { voiceStateUpdatedHandler.handle(it) }.subscribe()

    gateway.onDisconnect().block()
}