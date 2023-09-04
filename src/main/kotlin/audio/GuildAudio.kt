package audio

import GlobalData
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class GuildAudio(client: GatewayDiscordClient, guildId: Snowflake, messageChannel: MessageChannel) {

    val LEAVE_DELAY: Duration = Duration.ofMinutes(1)

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    val leavingTask: AtomicReference<Disposable> = AtomicReference()
    val client: GatewayDiscordClient = client
    val guildId: Snowflake = guildId
    val scheduler: AudioTrackScheduler

    init {
        this.scheduler = AudioTrackScheduler(player, guildId, messageChannel)
        player.addListener(scheduler)
    }

    fun scheduleLeave() {
        leavingTask.set(
            Mono.delay(LEAVE_DELAY, Schedulers.boundedElastic())
                .filter { isLeavingScheduled() }
                .map { client.voiceConnectionRegistry }
                .flatMap { it.getVoiceConnection(guildId) }
                .flatMap { it.disconnect() }
                .subscribe()
        )
    }

    fun cancelLeave() {
        if (!isLeavingScheduled()) return
        this.leavingTask.get().dispose()
    }

    fun isLeavingScheduled(): Boolean {
        return leavingTask.get() != null && !leavingTask.get().isDisposed
    }

    fun destroy() {
        cancelLeave()
        scheduler.destroy()
    }

}