import Command.Command
import Command.PingCommand
import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent

fun main(args: Array<String>) {
    println("Loaded token: ${args[0]}")

    val commands = mutableMapOf<String, Command>()
    commands["ping"] = PingCommand()

    val client = DiscordClient.create(args[0]);
    val gateway = client.login().block() ?: return

    gateway.eventDispatcher.on(MessageCreateEvent::class.java).subscribe {
        val content = it.message.content
        for (command in commands) {
            if (content.startsWith("!${command.key}")) {
                command.value.execute(it)
            }
        }
    }

    gateway.onDisconnect().block()
}