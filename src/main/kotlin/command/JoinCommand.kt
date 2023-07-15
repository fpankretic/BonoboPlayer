package command

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.VoiceChannelJoinSpec
import audio.LavaPlayerAudioProvider

class JoinCommand(private val provider: LavaPlayerAudioProvider) : Command {
    override fun execute(event: MessageCreateEvent) {
        val member = event.member.orElse(null)
        if (member != null) {
            val voiceState = member.voiceState.block()
            if (voiceState != null) {
                val channel = voiceState.channel.block()
                if (channel != null) {
                    val spec = VoiceChannelJoinSpec.builder()
                        .provider(provider)
                        .build();
                    channel.join(spec).block()
                }
            }
        }
    }
}