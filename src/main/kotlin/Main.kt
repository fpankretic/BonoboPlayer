import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import handler.MessageCreatedHandler

fun main(args: Array<String>) {
    val client = DiscordClient.create(args[0])
    val gateway = client.login().block() ?: return

    gateway.on(MessageCreateEvent::class.java).subscribe(MessageCreatedHandler())

    gateway.onDisconnect().block()
}