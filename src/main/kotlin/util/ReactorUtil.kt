package util

import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import java.util.*

fun <T> monoOptional(element: Optional<T>): Mono<T> {
    return mono { element }
        .filter { it.isPresent }
        .map { it.get() }
}