package handler

import discord4j.core.event.domain.Event
import mu.KotlinLogging
import reactor.core.publisher.Mono

class AllEventHandler {

    private val logger = KotlinLogging.logger {}

    fun handle(event: Event): Mono<Void> {
        return Mono.justOrEmpty(event)
            .map { logger.info { it.javaClass.name } }
            .then()
    }

}