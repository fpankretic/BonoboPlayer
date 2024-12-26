package command

import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

class RepeatCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }

        val guildId = event.guildId.get()
        if (GuildManager.audioExists(guildId).not()) {
            return mono { null }
        }

        GuildManager.getAudio(guildId).flipRepeating()
        return mono { null }
    }

    override fun help(): String {
        return "Repeats the current song."
    }
}