package command

import GlobalData
import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent


class SkipCommand : Command {
    override fun execute(event: MessageCreateEvent) {
        if (event.guildId.isEmpty) return
        GuildAudioManager.of(event.guildId.get()).scheduler.skip()
    }
}