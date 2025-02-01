package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.sendSwitchMessage
import util.simpleMessageEmbed

class ShuffleCommand : Command() {

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return GuildManager.audioMono(guildId)
            .switchIfEmpty(sendSwitchMessage(event, Message.QUEUE_EMPTY))
            .map { shuffle(it) }
            .then()
    }

    override fun help(): String {
        return "Shuffles the queue."
    }

    private fun shuffle(guildAudio: GuildAudio) {
        val shuffled = guildAudio.shuffleQueue()
        if (shuffled) {
            guildAudio.sendMessage(simpleMessageEmbed(Message.QUEUE_SHUFFLED.message))
        }

    }

}