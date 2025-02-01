package util

import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import java.util.*

fun <T> monoOptional(element: Optional<T>): Mono<T> {
    return mono { element.orElse(null) }
}

fun <T> sendSwitchMessage(event: MessageCreateEvent, messageType: Message): Mono<T> {
    return event.message.channel
        .flatMap { it.createMessage(simpleMessageEmbed(messageType.message)) }
        .mapNotNull { null }
}