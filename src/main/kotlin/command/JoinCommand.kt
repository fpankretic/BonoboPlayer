package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.VoiceChannelJoinSpec
import audio.LavaPlayerAudioProvider
import discord4j.voice.VoiceConnection

class JoinCommand : Command {
    override fun execute(event: MessageCreateEvent): VoiceConnection?  {
        val member = event.member.orElse(null)
        if (member != null) {
            val voiceState = member.voiceState.block()
            if (voiceState != null) {
                val channel = voiceState.channel.block()
                if (channel != null) {
                    val provider = GuildAudioManager.of(channel.guildId).provider
                    val spec = VoiceChannelJoinSpec.builder()
                        .provider(provider)
                        .build();
                    return channel.join(spec).block()
                }
            }
        }
        return null
    }
}