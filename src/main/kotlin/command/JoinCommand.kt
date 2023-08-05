package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import reactor.core.publisher.Mono

class JoinCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .zipWith(event.message.channel)
            .flatMap { joinVoiceChannel(it.t1, it.t2) }
            .then()
    }

    private fun joinVoiceChannel(channel: VoiceChannel, messageChannel: MessageChannel): Mono<Void> {
        val provider = GuildAudioManager.of(channel.guildId, messageChannel).provider
        val spec = VoiceChannelJoinSpec.builder().provider(provider).build();
        return channel.join(spec).then()
    }

}