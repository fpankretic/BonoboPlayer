package command

import audio.GuildAudio
import audio.GuildManager
import audio.load.DefaultAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.Message
import util.monoOptional
import util.sendSwitchMessage
import util.simpleMessageEmbed

object DraganaCommand : Command {
    private val logger = KotlinLogging.logger {}

    private val songs = listOf(
        "ytmsearch: Dragana Mirković Luće moje",
        "ytmsearch: Dragana Mirković Pečat na usnama",
        "ytmsearch: Dragana Mirković Gromovi"
    )

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .switchIfEmpty(sendSwitchMessage(event, Message.NOT_IN_VOICE_CHANNEL))
            .flatMap { channel ->
                cancelLeave(guildId)
                    .then(executeJoinCommand(event, guildId))
                    .then(event.message.channel)
                    .map { GuildManager.createAudio(event.client, guildId, it.id) }
                    .flatMap { guildAudio ->
                        queueDraganaSongs(guildAudio, guildId, event)
                    }
                    .onErrorStop()
                    .then()
            }
    }

    private fun queueDraganaSongs(guildAudio: GuildAudio, guildId: Snowflake, event: MessageCreateEvent): Mono<Void> {
        return mono {
            guildAudio.clearQueue()
            guildAudio.player.playTrack(null)

            val author = event.member.get()
            val authorName = author.globalName.orElse(author.username)
            val avatarUrl = author.avatarUrl

            songs.forEachIndexed { index, query ->
                logger.info { "Loading Dragana song ${index + 1}/3: $query" }
                guildAudio.addHandler(
                    DraganaLoadResultHandler(guildId, authorName, avatarUrl, index == 0),
                    query
                )
            }

            guildAudio.sendMessage(
                simpleMessageEmbed("🎤 Queuing Dragana's greatest hits! Luće moje, Pečat na usnama, Gromovi - coming up!")
            )
        }
    }

    private fun executeJoinCommand(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        if (GuildManager.audioExists(guildId).not()) {
            return JoinCommand.execute(event, guildId).onErrorStop()
        }
        return mono { null }
    }

    private fun cancelLeave(guildId: Snowflake): Mono<GuildAudio> {
        return GuildManager.audioMono(guildId).filter { it.isLeavingScheduled() }
    }

    override fun help(): String = "Queues Dragana Mirković's greatest hits immediately."
}
