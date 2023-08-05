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
        val song = event.message.content.substringAfter(" ")
        GlobalData.PLAYER_MANAGER.loadItem(song, DefaultAudioLoadResultHandler(audioManager, song, event))
    }

}