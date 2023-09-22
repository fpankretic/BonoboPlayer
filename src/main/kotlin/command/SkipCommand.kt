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
            .filter { filterChain(it, event) }
            .map { it.scheduleLeave() }
            .onErrorComplete()
            .then()
    }

    private fun filterChain(guildAudio: GuildAudio, event: MessageCreateEvent): Boolean {
        val position = event.message.content.split(" ").getOrElse(1) { "0" }.toInt()
        return guildAudio.skipInQueue(position).not() &&
                guildAudio.isLeavingScheduled().not() &&
                position == 0
    }

}