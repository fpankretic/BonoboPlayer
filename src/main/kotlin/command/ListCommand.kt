package command

import audio.GuildAudio
import audio.load.DefaultAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging

object ListCommand : PlayCommandBase() {
    private val logger = KotlinLogging.logger {}

    override fun play(event: MessageCreateEvent, guildAudio: GuildAudio, guildId: Snowflake) {
        val songList = event.message.content.substringAfter(" ").trim().split(";")

        songList.forEach {
            val track = loadTrack(it.trim())
            logger.debug { "Parsed query \"$songList\"." }

            guildAudio.addHandler(
                DefaultAudioLoadResultHandler(guildId, event.message.author.get(), track),
                track
            )
        }
    }

    override fun help(): String {
        return "Adds a list of songs separated by \";\"."
    }
}