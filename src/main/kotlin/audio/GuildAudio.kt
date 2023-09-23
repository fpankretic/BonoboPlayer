package audio

import GlobalData
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GuildAudio(
    private val client: GatewayDiscordClient,
    private val guildId: Snowflake,
) {
    private val logger = KotlinLogging.logger {}
    private val leaveDelay = Duration.ofMinutes(3)

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    private var destroyed: Boolean = false
    private val scheduler: AudioTrackScheduler = AudioTrackScheduler(player, guildId)
    private var messageChannelId: AtomicLong = AtomicLong()
    private val leavingTask: AtomicReference<Disposable> = AtomicReference()
    private val loadResultHandlers: ConcurrentHashMap<AudioLoadResultHandler, Future<Void>> = ConcurrentHashMap()

    init {
        player.addListener(scheduler)
    }

    fun scheduleLeave() {
        if (destroyed) {
            return
        }
        logger.info { "Scheduling bot leave." }
        leavingTask.set(
            Mono.delay(leaveDelay, Schedulers.boundedElastic())
                .filter { isLeavingScheduled() }
                .map { client.voiceConnectionRegistry }
                .flatMap { it.getVoiceConnection(guildId) }
                .flatMap { it.disconnect() }
                .then(Mono.fromCallable { sendMessage(getLeaveMessage()) })
                .map { GuildManager.destroyAudio(guildId) }
                .subscribe()
        )
    }

    fun cancelLeave() {
        if (!isLeavingScheduled()) return
        logger.info { "Bot leave canceled." }
        leavingTask.get().dispose()
    }

    fun isLeavingScheduled(): Boolean {
        return leavingTask.get()?.isDisposed?.not() ?: false
    }

    fun setMessageChannelId(messageChannelId: Snowflake) {
        this.messageChannelId.set(messageChannelId.asLong())
    }

    fun sendMessage(embedCreateSpec: EmbedCreateSpec) {
        getMessageChannel().flatMap { it.createMessage(embedCreateSpec) }.subscribe()
    }

    fun getQueue(): List<AudioTrack> {
        return scheduler.getQueue()
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

    fun skipInQueue(position: Int): Boolean {
        if (position == 0) {
            return skip()
        } else if (position < 0 || position > scheduler.getQueue().size) {
            return false
        }

        return scheduler.skipInQueue(position)
    }

    fun addHandler(loadResultHandler: DefaultAudioLoadResultHandler) {
        logger.info { "GuildId: ${guildId.asLong()} Adding audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers[loadResultHandler] =
            GlobalData.PLAYER_MANAGER.loadItemOrdered(guildId.asLong(), loadResultHandler.track, loadResultHandler)
    }

    fun removeHandler(loadResultHandler: DefaultAudioLoadResultHandler) {
        logger.info { "GuildId: ${guildId.asLong()} Removing audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers.remove(loadResultHandler)
    }

    fun logBotVoiceStatus() {
        client.getSelfMember(guildId)
            .flatMap { it.voiceState }
            .flatMap { it.channel }
            .flatMap { it.voiceConnection }
            .flatMapMany { it.stateEvents() }
            .doOnNext { logger.info { "Bot VoiceState is $it." } }
            .subscribe()
    }

    fun destroy() {
        destroyed = true
        cancelLeave()
        loadResultHandlers.forEach { it.value.cancel(true) }
        scheduler.destroy()
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