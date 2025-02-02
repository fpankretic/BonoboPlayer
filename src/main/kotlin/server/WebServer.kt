package server

import com.sun.net.httpserver.HttpServer
import discord4j.core.GatewayDiscordClient
import io.github.oshai.kotlinlogging.KotlinLogging
import util.EnvironmentManager
import util.EnvironmentValue.PORT
import util.simpleMessageEmbed
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

fun startWebServer(client: GatewayDiscordClient) {
    val port = EnvironmentManager.valueOf(PORT).toIntOrNull()
    if (port == null || port < 1 || port > 65535) {
        logger.error { "Invalid port number. Web server not started." }
        return
    }

    val webServer = HttpServer.create()
    webServer.bind(InetSocketAddress(port), 0)

    webServer.createContext("/send") { exchange ->
        if ("POST".equals(exchange.requestMethod, ignoreCase = true)) {
            val inputStream = exchange.requestBody
            val body = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
            sendMessageToAllServers(body, client)
            exchange.sendResponseHeaders(204, -1)
        } else {
            exchange.sendResponseHeaders(405, -1)
        }
        exchange.close()
    }

    webServer.start()
    logger.info { "Web server started." }
}

private fun sendMessageToAllServers(message: String, client: GatewayDiscordClient) {
    val messageEmbed = simpleMessageEmbed("@here $message")
    client.guilds
        .flatMap { it.systemChannel }
        .flatMap { it.createMessage(messageEmbed) }
        .subscribe()
}