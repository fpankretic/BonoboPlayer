package command

import audio.GuildManager
import audio.load.SearchAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Mono
import util.Message
import util.monoOptional
import util.sendSwitchMessage

object SearchCommand : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .filter { it.guildId == guildId }
            .switchIfEmpty(sendSwitchMessage(event, Message.NOT_IN_VOICE_CHANNEL))
            .map { search(event, guildId) }
            .then()
    }

    override fun help(): String {
        return "Searches for a song."
    }

    private fun search(event: MessageCreateEvent, guildId: Snowflake) {
        val query = "ytsearch: ${event.message.content.substringAfter(" ").trim()}"
        logger.debug { "Parsed query \"$query\"." }

        JoinCommand.joinVoiceChannel(event.message.channel, event.member, event.guildId.get()).block()
        GuildManager.audio(guildId).addHandler(SearchAudioLoadResultHandler(event.guildId.get()), query)
    }
}