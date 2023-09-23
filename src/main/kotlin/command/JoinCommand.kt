package command

import audio.GuildManager
import audio.LavaPlayerAudioProvider
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import discord4j.voice.VoiceConnection
import mu.KotlinLogging
import reactor.core.publisher.Mono

class JoinCommand : Command {

    val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return Mono.empty()
        }
        val guildId = event.guildId.get()


        return event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .switchIfEmpty(destroyAudioAndJoin(event, guildId))
            .then(joinVoiceChannel(event, guildId))
            .then()
    }

    private fun destroyAudioAndJoin(event: MessageCreateEvent, guildId: Snowflake): Mono<VoiceConnection> {
        return Mono.fromCallable {
            logger.info { "Function destroyAudioAndJoin called." }
            GuildManager.destroyAudio(guildId)
        }.then(joinVoiceChannel(event, guildId))
    }

    private fun joinVoiceChannel(event: MessageCreateEvent, guildId: Snowflake): Mono<VoiceConnection> {
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