package command

import audio.GuildAudio
import audio.load.DefaultAudioLoadResultHandler
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging

class ListCommand : PlayCommand() {

    private val logger = KotlinLogging.logger {}

    override fun help(): String {
        return "Adds a list of songs separated by \";\"."
    }

    override fun play(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val songList = event.message.content.substringAfter(" ").trim().split(";")
        logger.info { "Parsed query: $songList." }

        songList.forEach {
            val track = loadTrack(it.trim())
            guildAudio.addHandler(
                DefaultAudioLoadResultHandler(event.guildId.get(), event.message.author.get(), track),
                track
            )
        }
    }

}