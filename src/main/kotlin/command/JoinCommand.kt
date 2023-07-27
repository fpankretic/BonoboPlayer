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

    private fun joinVoiceChannel(channel: VoiceChannel): Mono<Any> {
        val provider = GuildAudioManager.of(channel.guildId).provider
        val spec = VoiceChannelJoinSpec.builder().provider(provider).build();
        return channel.join(spec)
            .subscribeOn(Schedulers.boundedElastic())
            .map { autoLeave(it, channel).subscribe() }
    }

    private fun autoLeave(connection: VoiceConnection, channel: VoiceChannel): Mono<Void> {
        val voiceStateCounter = channel.voiceStates.count().map { it == 1L }

        val onDelay = Mono.delay(Duration.ofSeconds(60L))
            .map { println("onDelay") }
            .filterWhen { voiceStateCounter }
            .switchIfEmpty(Mono.never())
            .then()

        val onEvent = channel.client.eventDispatcher.on(VoiceStateUpdateEvent::class.java)
            .map {
                println("onEvent")
                it
            }
            .filter { event -> event.old.flatMap { it.channelId }.map { it.equals(channel.id) }.orElse(false) }
            .filterWhen { voiceStateCounter }
            .next()
            .then()

        return Mono.firstWithSignal(onDelay, onEvent).then(connection.disconnect())
    }
}