import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import handler.MessageCreatedHandler
import handler.VoiceStateUpdatedHandler
import env.EnvironmentValue
import env.EnvironmentManager
import env.EnvironmentValue.MAINTENANCE
import mu.KotlinLogging

fun main() {
    val logger = KotlinLogging.logger {}

    if (EnvironmentManager.get(MAINTENANCE).toBoolean()) {
        logger.info { "Bot is in maintenance mode" }
    }

    val client = DiscordClient.create(EnvironmentManager.get(EnvironmentValue.DISCORD_API_TOKEN))
    val gateway = client.login().block() ?: return

    val messageCreatedHandler = MessageCreatedHandler()
    val voiceStateUpdatedHandler = VoiceStateUpdatedHandler()

    gateway.on(MessageCreateEvent::class.java) { messageCreatedHandler.handle(it) }.subscribe()
    gateway.on(VoiceStateUpdateEvent::class.java) { voiceStateUpdatedHandler.handle(it) }.subscribe()

    gateway.onDisconnect().block()
}