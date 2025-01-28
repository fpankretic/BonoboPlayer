package command

import audio.GuildManager
import audio.load.SearchAudioLoadResultHandler
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Mono
import util.monoOptional

class SearchCommand : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return monoOptional(event.guildId)
            .map { search(event) }
            .then()
    }

    override fun help(): String {
        return "Searches for a song."
    }

    private fun search(event: MessageCreateEvent) {
        val query = "ytsearch: ${event.message.content.substringAfter(" ").trim()}"
        logger.debug { "Parsed query \"$query\"." }

        val guildAudio = GuildManager.createAudio(event.client, event.guildId.get(), event.message.channelId)
        JoinCommand().joinVoiceChannel(event.message.channel, event.member, event.guildId.get()).subscribe()
        guildAudio.addHandler(SearchAudioLoadResultHandler(event.guildId.get()), query)
    }

}