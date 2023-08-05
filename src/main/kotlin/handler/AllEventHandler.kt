package handler

import discord4j.core.event.domain.Event
import reactor.core.publisher.Mono

class AllEventHandler {

    fun handle(event: Event): Mono<Void> {
        return Mono.justOrEmpty(event)
            .map { println(it.javaClass.name) }
            .then()
    }

}