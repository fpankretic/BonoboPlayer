package command

import audio.GuildManager
import audio.load.SearchAudioLoadResultHandler
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono

class SearchCommand : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }

        search(event)

        return mono { null }
    }

    override fun help(): String {
        return "Searches for a song."
    }

    private fun search(event: MessageCreateEvent) {
        val query = "ytsearch: ${event.message.content.substringAfter(" ").trim()}"
        logger.info { "Parsed query: $query." }

        val guildAudio = GuildManager.createAudio(event.client, event.guildId.get(), event.message.channelId)
        JoinCommand().joinVoiceChannel(event.message.channel, event.member, event.guildId.get()).subscribe()
        guildAudio.addHandler(SearchAudioLoadResultHandler(event.guildId.get()), query)
    }

}