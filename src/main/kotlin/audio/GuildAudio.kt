package audio

import GlobalData
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import util.EmbedUtils
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GuildAudio(
    private val client: GatewayDiscordClient,
    private val guildId: Snowflake,
) {
    private val logger = KotlinLogging.logger {}
    private val leaveDelay = Duration.ofMinutes(3)

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    private val scheduler: AudioTrackScheduler = AudioTrackScheduler(player, guildId)
    private var messageChannelId: AtomicLong = AtomicLong()
    private val leavingTask: AtomicReference<Disposable> = AtomicReference()

    init {
        player.addListener(scheduler)
    }

    fun scheduleLeave() {
        logger.info { "Scheduling bot leave." }
        leavingTask.set(
            Mono.delay(leaveDelay, Schedulers.boundedElastic())
                .filter { isLeavingScheduled() }
                .map { client.voiceConnectionRegistry }
                .flatMap { it.getVoiceConnection(guildId) }
                .map {
                    sendMessage(getLeaveMessage())
                    it
                }
                .map {
                    scheduler.destroy()
                    it
                }
                .flatMap { it.disconnect() }
                .subscribe()
        )
    }

    fun cancelLeave() {
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

    fun setMessageChannelId(messageChannelId: Snowflake) {
        this.messageChannelId.set(messageChannelId.asLong())
    }

    fun sendMessage(embedCreateSpec: EmbedCreateSpec) {
        getMessageChannel().flatMap { it.createMessage(embedCreateSpec) }.subscribe()
    }

    fun getQueue(): List<AudioTrack> {
        return scheduler.queue
    }

    fun play(track: AudioTrack) {
        scheduler.play(track.makeClone())
    }

    fun getCurrentSong(): Optional<AudioTrack> {
        return scheduler.currentSong()
    }

    fun clearQueue() {
        scheduler.clear()
    }

    fun skip(): Boolean {
        return scheduler.skip()
    }

    private fun getMessageChannelId(): Snowflake {
        return Snowflake.of(messageChannelId.get())
    }

    private fun getMessageChannel(): Mono<MessageChannel> {
        return client.getChannelById(getMessageChannelId())
            .cast(MessageChannel::class.java)
    }

    private fun getLeaveMessage(): EmbedCreateSpec {
        return EmbedUtils.getDefaultEmbed()
            .description("Left the voice channel due to inactivity.")
            .build()
    }

}