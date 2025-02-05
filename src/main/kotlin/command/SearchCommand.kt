package command

import audio.GuildManager
import audio.load.SearchAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Mono

class SearchCommand : Command() {
    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        search(event, guildId)
        return Mono.empty()
    }

    override fun help(): String {
        return "Searches for a song."
    }

    private fun search(event: MessageCreateEvent, guildId: Snowflake) {
        val query = "ytsearch: ${event.message.content.substringAfter(" ").trim()}"
        logger.debug { "Parsed query \"$query\"." }

        JoinCommand().joinVoiceChannel(event.message.channel, event.member, event.guildId.get()).block()
        GuildManager.audio(guildId).addHandler(SearchAudioLoadResultHandler(event.guildId.get()), query)
    }

}