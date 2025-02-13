package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.sendSwitchMessage
import util.simpleMessageEmbed

object RemoveCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return GuildManager.audioMono(guildId)
            .filter { it.isQueueEmpty().not() }
            .switchIfEmpty(sendSwitchMessage(event, Message.QUEUE_EMPTY))
            .map { skipSong(it, event) }
            .onErrorComplete()
            .then()
    }

    override fun help(): String {
        return "Skips any song in the queue."
    }

    private fun skipSong(guildAudio: GuildAudio, event: MessageCreateEvent) {
        val position = event.message.content.split(" ")[1].toInt()
        val skipped = guildAudio.skipInQueue(position)
        if (skipped && guildAudio.isQueueEmpty() && guildAudio.isSongLoaded().not()) {
            guildAudio.sendMessage(simpleMessageEmbed(Message.QUEUE_EMPTY.message))
        }
    }
}