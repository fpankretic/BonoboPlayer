package command

import GlobalData
import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono


class SkipCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .map { GuildAudioManager.of(it).scheduler.skip() }
            .then()
    }
}