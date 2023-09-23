package command

import audio.GuildManager
import audio.LavaPlayerAudioProvider
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import discord4j.voice.VoiceConnection
import reactor.core.publisher.Mono

class JoinCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.guildId)
            .flatMap { event.client.getSelfMember(it) }
            .flatMap { it.voiceState }
            .filter { it.channelId.isEmpty } // bot is not connected
            .flatMap { joinVoiceChannel(it.guildId, event) }
            .onErrorComplete()
            .then()
    }

    private fun joinVoiceChannel(guildId: Snowflake, event: MessageCreateEvent): Mono<VoiceConnection> {
        GuildManager.destroyAudio(guildId)
        return Mono.justOrEmpty(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .zipWith(event.message.channel)
            .flatMap { createVoiceConnection(it.t1, it.t2, guildId) }
    }

    private fun createVoiceConnection(
        voiceChannel: VoiceChannel,
        messageChannel: MessageChannel,
        guildId: Snowflake
    ): Mono<VoiceConnection> {
        val player = GuildManager.getAudio(voiceChannel.client, guildId, messageChannel.id).player
        val spec = VoiceChannelJoinSpec.builder().provider(LavaPlayerAudioProvider(player)).build();
        return voiceChannel.join(spec)
    }

}