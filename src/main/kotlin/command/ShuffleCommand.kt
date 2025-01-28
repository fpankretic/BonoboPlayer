package command

import audio.GuildAudio
import audio.GuildManager
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.monoOptional
import util.sendQueueEmptyMessage
import util.simpleMessageEmbed

class ShuffleCommand : Command {

    override fun execute(event: MessageCreateEvent): Mono<Void> {

        return monoOptional(event.guildId)
            .flatMap { GuildManager.audioMono(it) }
            .switchIfEmpty(sendQueueEmptyMessage(event))
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