package command

import audio.GuildManager
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import util.Message
import util.monoOptional
import util.sendSwitchMessage

class LeaveCommand : Command() {

    override fun execute(event: MessageCreateEvent, guildId: Snowflake): Mono<Void> {
        return monoOptional(event.member)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .flatMap { it.voiceConnection }
            .filter { it.guildId == guildId }
            .switchIfEmpty(sendSwitchMessage(event, Message.WRONG_GUILD))
            .flatMap {
                GuildManager.destroyAudio(guildId)
                it.disconnect()
            }
            .then()
    }

    override fun help(): String {
        return "Leaves the voice channel."
    }

}