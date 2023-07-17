package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent

class QueueCommand : Command {
    override fun execute(event: MessageCreateEvent) {
        if (event.guildId.isEmpty) return
        val scheduler = GuildAudioManager.of(event.guildId.get()).scheduler
        val message = scheduler.queue
            .take(10)
            .joinToString("\n") { it.info.title }
        event.message.channel.block()?.createMessage(message)?.block()
    }
}