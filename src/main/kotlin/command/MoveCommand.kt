package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.monoOptional
import util.sendSwitchMessage
import util.simpleMessageEmbed

object MoveCommand : Command {
    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .switchIfEmpty(sendSwitchMessage(event, Message.NOT_IN_VOICE_CHANNEL))
            .map { moveSong(event, guildId) }
            .then()
    }

    override fun help(): String {
        return "Moves the song position in the queue."
    }

    private fun moveSong(event: MessageCreateEvent, guildId: Snowflake) {
        val guildAudio = GuildManager.audio(guildId)

        val split = event.message.content.split(" ")
        if (split.size < 3) {
            guildAudio.sendMessage(simpleMessageEmbed(Message.INVALID_ARGUMENTS.message))
        }

        val from = event.message.content.split(" ")[1].toInt()
        val to = event.message.content.split(" ")[2].toInt()

        if (guildAudio.moveSong(from, to)) {
            guildAudio.sendMessage(simpleMessageEmbed(Message.SONG_MOVED.message))
        }
    }
}