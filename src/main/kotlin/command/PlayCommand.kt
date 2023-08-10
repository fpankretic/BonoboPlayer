package command

import GlobalData
import audio.DefaultAudioLoadResultHandler
import audio.GuildAudioManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class PlayCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return JoinCommand().execute(event)
            .then(Mono.justOrEmpty(event.guildId))
            .map { GuildAudioManager.of(it) }
            .map { play(it, event) }
            .then()
    }

    private fun play(audioManager: GuildAudioManager, event: MessageCreateEvent) {
        val query = event.message.content.substringAfter(" ")
        val track = getTrack(query)
        GlobalData.PLAYER_MANAGER.loadItem(track, DefaultAudioLoadResultHandler(audioManager, event))
    }

    private fun getTrack(query: String): String {
        if (query.startsWith("http") || query.startsWith("www") || query.startsWith("youtube")) {
            return query
        }
        return GlobalData.SEARCH_CLIENT.getTracksForSearch(query)[0].url
    }

}