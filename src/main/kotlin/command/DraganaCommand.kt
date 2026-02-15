package command

import audio.GuildAudio
import audio.load.DefaultAudioLoadResultHandler
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent

object DraganaCommand : PlayCommandBase() {
    private val songs = listOf(
        "Dragana Mirković Luće moje",
        "Dragana Mirković Pečat na usnama",
        "Dragana Mirković Gromovi"
    )

    override fun help() = "Queues Dragana Mirković's greatest hits immediately."

    override fun play(event: MessageCreateEvent, guildAudio: GuildAudio, guildId: Snowflake) {
        songs.forEach { song ->
            val query = "ytmsearch: $song"
            guildAudio.addHandler(
                DefaultAudioLoadResultHandler(guildId, event.message.author.get(), query),
                query
            )
        }
    }
}
