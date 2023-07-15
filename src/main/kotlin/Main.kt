import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import command.Command
import command.JoinCommand
import command.PingCommand
import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import audio.LavaPlayerAudioProvider
import audio.TrackScheduler
import command.PlayCommand

fun main(args: Array<String>) {
    println("Loaded token: ${args[0]}")

    // player configuration
    val playerManager = DefaultAudioPlayerManager()
    playerManager.configuration.setFrameBufferFactory{a,b,c -> NonAllocatingAudioFrameBuffer(a,b,c)}
    AudioSourceManagers.registerRemoteSources(playerManager)
    val player = playerManager.createPlayer()
    val scheduler = TrackScheduler(player)
    val provider = LavaPlayerAudioProvider(player)

    // command configuration
    val commands = mutableMapOf<String, Command>()
    commands["ping"] = PingCommand()
    commands["join"] = JoinCommand(provider)
    commands["play"] = PlayCommand(playerManager, scheduler)

    val client = DiscordClient.create(args[0]);
    val gateway = client.login().block() ?: return

    gateway.eventDispatcher.on(MessageCreateEvent::class.java).subscribe {
        val content = it.message.content
        for (command in commands) {
            if (content.startsWith("&${command.key}")) {
                command.value.execute(it)
            }
        }
    }

    gateway.onDisconnect().block()
}