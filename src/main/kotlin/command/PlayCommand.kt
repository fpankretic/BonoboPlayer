package command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.core.event.domain.message.MessageCreateEvent

class PlayCommand(
    private val playerManager: AudioPlayerManager,
    private val scheduler: AudioLoadResultHandler
) : Command {
    override fun execute(event: MessageCreateEvent) {
        val content = event.message.content
        val command = content.split(" ")
        playerManager.loadItem(command[1], scheduler)
    }
}