package command

import audio.GuildManager
import audio.LavaPlayerAudioProvider
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import reactor.core.publisher.Mono

class JoinCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
            // TODO: Check if already joined
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .zipWith(event.message.channel)
            .flatMap { joinVoiceChannel(it.t1, it.t2) }
            .then()
    }

    private fun joinVoiceChannel(channel: VoiceChannel, messageChannel: MessageChannel): Mono<Void> {
        val player = GuildManager.getAudio(channel.client, channel.guildId, messageChannel.id).player
        val spec = VoiceChannelJoinSpec.builder().provider(LavaPlayerAudioProvider(player)).build();
        return channel.join(spec).then()
    }

}