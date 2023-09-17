package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono


class SkipCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .map { GuildManager.getAudio(it) }
            .filter { isSkipped(it) }
            .filter { !it.isLeavingScheduled() }
            .map { it.scheduleLeave() }
            .then()
    }

    private fun isSkipped(guildAudio: GuildAudio): Boolean {
        return guildAudio.scheduler.skip().not()
    }

}