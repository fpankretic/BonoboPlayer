import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import env.EnvironmentManager
import env.EnvironmentValue
import env.EnvironmentValue.IPV6_ENABLED
import handler.MessageCreatedHandler
import handler.VoiceStateUpdatedHandler
import mu.KotlinLogging

fun main() {
    val logger = KotlinLogging.logger {}

    if (EnvironmentManager.get(IPV6_ENABLED).toBoolean()) {
        logger.info { "Bot is in IPv6 mode." }
    }

    val client = DiscordClient.create(EnvironmentManager.get(EnvironmentValue.DISCORD_API_TOKEN))
    val gateway = client.login().block() ?: return

    // Initialize the player manager
    GlobalData.PLAYER_MANAGER

    val messageCreatedHandler = MessageCreatedHandler()
    val voiceStateUpdatedHandler = VoiceStateUpdatedHandler()

    gateway.on(MessageCreateEvent::class.java) { messageCreatedHandler.handle(it) }.subscribe()
    gateway.on(VoiceStateUpdateEvent::class.java) { voiceStateUpdatedHandler.handle(it) }.subscribe()

    gateway.onDisconnect().block()
}