import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import env.EnvironmentManager
import env.EnvironmentValue
import env.EnvironmentValue.IPV6_ENABLED
import handler.MessageCreatedHandler
import handler.VoiceStateUpdatedHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import server.startWebServer

fun main() {
    val logger = KotlinLogging.logger {}

    if (EnvironmentManager.get(IPV6_ENABLED).toBoolean()) {
        System.setProperty("java.net.preferIPv6Addresses", "true")
        System.setProperty("java.net.preferIPv4Stack", "false")
        logger.info { "Bot is in IPv6 mode." }
    }

    val client = DiscordClient.create(EnvironmentManager.get(EnvironmentValue.DISCORD_API_TOKEN))
    val gateway = client.login().block() ?: return

    // Start web server for updates
    startWebServer(gateway)

    // Initialize the player manager
    GlobalData.PLAYER_MANAGER

    val messageCreatedHandler = MessageCreatedHandler()
    val voiceStateUpdatedHandler = VoiceStateUpdatedHandler()

    gateway.on(MessageCreateEvent::class.java) { messageCreatedHandler.handle(it) }.subscribe()
    gateway.on(VoiceStateUpdateEvent::class.java) { voiceStateUpdatedHandler.handle(it) }.subscribe()

    gateway.onDisconnect().block()
}