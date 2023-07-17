package command

import discord4j.core.event.domain.message.MessageCreateEvent

class LeaveCommand : Command {
    override fun execute(event: MessageCreateEvent) {
        val member = event.member.orElse(null) ?: return
        val voiceState = member.voiceState.block() ?: return
        event.client.voiceConnectionRegistry
            .getVoiceConnection(voiceState.guildId).block()
            ?.disconnect()?.block()
    }
}