package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging
import reactor.core.publisher.Mono

class SkipCommand : Command {

    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .map { GuildManager.getAudio(it) }
            .filter { filterChain(it) }
            .map { it.scheduleLeave() }
            .then()
    }

    private fun filterChain(guildAudio: GuildAudio): Boolean {
        return guildAudio.skip().not() &&
                guildAudio.isLeavingScheduled().not()
    }

}