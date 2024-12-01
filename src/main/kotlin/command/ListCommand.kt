package command

import audio.GuildAudio
import audio.GuildManager
import audio.load.DefaultAudioLoadResultHandler
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Mono

class ListCommand : Command {

    private val logger = KotlinLogging.logger {}
    private val playCommand = PlayCommand()

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        if (event.guildId.isEmpty) {
            return mono { null }
        }
        val guildId = event.guildId.get()

        playCommand.cancelLeave(guildId)
        return playCommand.executeJoinCommand(event, guildId)
            .then(event.message.channel)
            .map { GuildManager.createAudio(event.client, guildId, it.id) }
            .map { list(it, event) }
            .onErrorStop()
            .then()
    }

    override fun help(): String {
        return "Adds a list of songs separated by \";\"."
    }

    private fun list(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val songList = event.message.content.substringAfter(" ").trim().split(";")
        logger.info { "Parsed query: $songList." }

        songList.forEach {
            val track = playCommand.loadTrack(it.trim())
            guildAudio.addHandler(
                DefaultAudioLoadResultHandler(event.guildId.get(), event.message.author.get(), track),
                track
            )
        }
    }

}