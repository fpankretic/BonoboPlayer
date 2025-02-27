import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.gateway.intent.IntentSet
import handler.MemberJoinHandler
import handler.MessageCreatedHandler
import handler.VoiceStateUpdatedHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import server.startWebServer
import util.EnvironmentManager
import util.EnvironmentValue
import util.EnvironmentValue.IPV6_ENABLED

fun main() {
    val logger = KotlinLogging.logger {}

    // Enable IPv6 if specified
    if (EnvironmentManager.valueOf(IPV6_ENABLED).toBoolean()) {
        System.setProperty("java.net.preferIPv6Addresses", "true")
        System.setProperty("java.net.preferIPv4Stack", "false")
        logger.info { "Bot is in IPv6 mode." }
    }

    val client = DiscordClient.create(EnvironmentManager.valueOf(EnvironmentValue.DISCORD_API_TOKEN))
    val gateway = client.gateway().setEnabledIntents(IntentSet.all()).login().block()!!

    // Start web server for updates
    startWebServer(gateway)

    // Initialize the player manager
    GlobalData.PLAYER_MANAGER

    gateway.on(MessageCreateEvent::class.java) { MessageCreatedHandler.handle(it) }.subscribe()
    gateway.on(VoiceStateUpdateEvent::class.java) { VoiceStateUpdatedHandler.handle(it) }.subscribe()
    gateway.on(MemberJoinEvent::class.java) { MemberJoinHandler.handle(it) }.subscribe()

    gateway.onDisconnect().block()
}