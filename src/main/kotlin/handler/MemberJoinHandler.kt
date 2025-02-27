package handler

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.guild.MemberJoinEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import util.EnvironmentManager
import util.EnvironmentValue.ADD_DEFAULT_ROLE_GUILDS
import util.EnvironmentValue.DEFAULT_ROLE_ID

object MemberJoinHandler {
    private val logger = KotlinLogging.logger {}

    private val enabledGuilds = EnvironmentManager.valueOf(ADD_DEFAULT_ROLE_GUILDS).split(",").toSet()
    private val defaultRoleId = EnvironmentManager.valueOf(DEFAULT_ROLE_ID)

    fun handle(event: MemberJoinEvent): Mono<Void> {
        logger.info { "Member joined: ${event.member.username}" }
        logger.info { "Guild ID: ${event.guildId.asString()}" }

        if (event.guildId.asString() !in enabledGuilds || defaultRoleId.isEmpty()) {
            return mono { null }
        }

        return event.member.addRole(Snowflake.of(defaultRoleId)).onErrorComplete()
    }
}