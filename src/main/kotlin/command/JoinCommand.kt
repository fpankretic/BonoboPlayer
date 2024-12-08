package command

import audio.GuildManager
import audio.LavaPlayerAudioProvider
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import discord4j.voice.VoiceConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.monoOptional
import java.util.*

class JoinCommand : Command {

    val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }

        val guildId = event.guildId.get()
        val messageChannelMono = event.message.channel
        val member = event.member

        return event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .switchIfEmpty(destroyAudioAndJoin(event, guildId))
            .then(joinVoiceChannel(messageChannelMono, member, guildId))
            .onErrorStop()
            .then()
    }

    fun joinVoiceChannel(
        messageChannelMono: Mono<MessageChannel>,
        member: Optional<Member>,
        guildId: Snowflake
    ): Mono<VoiceConnection> {
        return monoOptional(member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .switchIfEmpty(mono {
                logger.debug { "User is not in a voice channel." }
                throw IllegalStateException("User is not in a voice channel.")
            })
            .onErrorStop()
            .zipWith(messageChannelMono)
            .flatMap { createVoiceConnection(it.t1, it.t2, guildId) }
    }

    override fun help(): String {
        return "Joins the voice channel the user is in."
    }

    private fun destroyAudioAndJoin(event: MessageCreateEvent, guildId: Snowflake): Mono<VoiceConnection> {
        return mono {
            logger.debug { "Function destroyAudioAndJoin called." }
            GuildManager.destroyAudio(guildId)
        }.then(joinVoiceChannel(event.message.channel, event.member, guildId))
    }

    private fun createVoiceConnection(
        voiceChannel: VoiceChannel,
        messageChannel: MessageChannel,
        guildId: Snowflake
    ): Mono<VoiceConnection> {
        val player = GuildManager.createAudio(voiceChannel.client, guildId, messageChannel.id).player
        val spec = VoiceChannelJoinSpec.builder().provider(LavaPlayerAudioProvider(player)).build();
        return voiceChannel.join(spec)
    }

}