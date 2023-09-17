package audio

import GlobalData
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GuildAudio(
    val client: GatewayDiscordClient,
    private val guildId: Snowflake,
) {
    private val logger = KotlinLogging.logger {}
    private val LEAVE_DELAY = Duration.ofMinutes(3)

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    val scheduler: AudioTrackScheduler = AudioTrackScheduler(player, guildId)
    private var messageChannelId: AtomicLong = AtomicLong()
    private val leavingTask: AtomicReference<Disposable> = AtomicReference()

    init {
        player.addListener(scheduler)
    }

    fun scheduleLeave() {
        logger.info { "Scheduling bot leave." }
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
        logger.info { "Trying to cancel bot leave if leaving." }
        if (!isLeavingScheduled()) return
        logger.info { "Bot leave canceled." }
        this.leavingTask.get().dispose()
    }

    fun isLeavingScheduled(): Boolean {
        return leavingTask.get() != null && !leavingTask.get().isDisposed
    }

    fun destroy() {
        cancelLeave()
        scheduler.destroy()
    }

    fun getMessageChannelId(): Snowflake {
        return Snowflake.of(messageChannelId.get())
    }

    fun getMessageChannel(): Mono<MessageChannel> {
        return client.getChannelById(getMessageChannelId())
            .cast(MessageChannel::class.java)
    }

    fun setMessageChannelId(messageChannelId: Snowflake) {
        this.messageChannelId.set(messageChannelId.asLong())
    }

}