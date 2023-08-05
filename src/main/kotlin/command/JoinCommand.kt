package command

import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.VoiceChannelJoinSpec
import audio.LavaPlayerAudioProvider
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.voice.VoiceConnection
import discord4j.voice.VoiceStateUpdateTask
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

class JoinCommand : Command {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .flatMap { joinVoiceChannel(it) }
            .then()
    }

    private fun joinVoiceChannel(channel: VoiceChannel): Mono<Void> {
        val provider = GuildAudioManager.of(channel.guildId).provider
        val spec = VoiceChannelJoinSpec.builder().provider(provider).build();
        return channel.join(spec).then()
    }

}