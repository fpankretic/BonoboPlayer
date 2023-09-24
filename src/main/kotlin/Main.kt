import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import handler.MessageCreatedHandler
import handler.VoiceStateUpdatedHandler
import secret.Credential
import secret.CredentialManager

fun main() {
    val client = DiscordClient.create(CredentialManager.get(Credential.DISCORD_API_TOKEN))
    val gateway = client.login().block() ?: return

    // handlers
    val messageCreatedHandler = MessageCreatedHandler()
    val voiceStateUpdatedHandler = VoiceStateUpdatedHandler()

    // subscribe
    gateway.on(MessageCreateEvent::class.java) { messageCreatedHandler.handle(it) }.subscribe()
    gateway.on(VoiceStateUpdateEvent::class.java) { voiceStateUpdatedHandler.handle(it) }.subscribe()

    gateway.onDisconnect().block()
}