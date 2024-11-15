package audio

import GlobalData
import audio.load.DefaultAudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import command.JoinCommand
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateSpec
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import util.EmbedUtils
import util.EmbedUtils.Companion.simpleMessageEmbed
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GuildAudio(private val client: GatewayDiscordClient, private val guildId: Snowflake) {

    private val logger = KotlinLogging.logger {}
    private val leaveDelay = Duration.ofMinutes(5)
    private val menuDelay = Duration.ofMinutes(1)
    private val removeDelay = Duration.ofSeconds(3)

    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer()
    private var destroyed: Boolean = false
    private val scheduler: AudioTrackScheduler = AudioTrackScheduler(player, guildId)
    private var messageChannelId: AtomicLong = AtomicLong()
    private val leavingTask: AtomicReference<Disposable> = AtomicReference()
    private val menusTasks: HashMap<String, AtomicReference<Disposable>> = hashMapOf()
    private val loadResultHandlers: ConcurrentHashMap<AudioLoadResultHandler, Future<Void>> = ConcurrentHashMap()
    private val equalizer: EqualizerFactory = EqualizerFactory()

    init {
        player.addListener(scheduler)
        player.setFilterFactory(equalizer)
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
                .then(mono { sendMessage(leaveMessage()) })
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

    fun sendMessageWithComponentAndTimeout(embedCreateSpec: EmbedCreateSpec, actionRow: ActionRow, customId: String) {
        sendMessageWithComponent(embedCreateSpec, actionRow)
        setMenuComponentTimeout(customId)
    }

    private fun sendMessageWithComponent(embedCreateSpec: EmbedCreateSpec, actionRow: ActionRow) {
        getMessageChannel().flatMap { channel ->
            channel.createMessage(
                MessageCreateSpec.builder()
                    .addEmbed(embedCreateSpec)
                    .addComponent(actionRow)
                    .build()
            )
        }.subscribe()
    }

    private fun setMenuComponentTimeout(customId: String) {
        val task = client.on(SelectMenuInteractionEvent::class.java) {
            if (it.customId.equals(customId)) {
                val value = it.values.toString().replace("[", "").replace("]", "")
                val author = it.interaction.user

                return@on JoinCommand().executeManual(it, guildId)
                    .timeout(removeDelay).then(it.message.get().delete())
                    .then(mono {
                        val track = "ytsearch: $value"
                        addHandler(DefaultAudioLoadResultHandler(guildId, author), track)
                    })
            }

            return@on mono { null }
        }.timeout(menuDelay)
            .onErrorResume(TimeoutException::class.java) { mono { logger.info { "Menu item timed out." } } }
            .map {
                if (menusTasks[customId] != null)
                    menusTasks[customId]?.get()?.dispose()
            }

        menusTasks[customId] = AtomicReference(task.subscribe())
    }

    fun play(track: AudioTrack) {
        scheduler.play(track.makeClone())
    }

    fun isSongLoaded(): Boolean {
        return scheduler.currentSong().isPresent
    }

    fun getQueue(): List<AudioTrack> {
        return scheduler.getQueue()
    }

    fun currentSong(): Optional<AudioTrack> {
        return scheduler.currentSong()
    }

    fun clearQueue() {
        scheduler.clearQueue()
    }

    fun skipTo(position: Int): Boolean {
        if (position < 1 || position > scheduler.getQueue().size) {
            return false
        }

        return scheduler.skipTo(position)
    }

    fun skipInQueue(position: Int): Boolean {
        if (position == 0) {
            val skipped = skip()
            if (!skipped) {
                GuildManager.getAudio(guildId).sendMessage(simpleMessageEmbed("Queue is empty.").build())
            }
            return skipped
        } else if (position < 0 || position > scheduler.getQueue().size) {
            return false
        }

        return scheduler.skipInQueue(position)
    }

    fun addHandler(loadResultHandler: AudioLoadResultHandler, query: String) {
        logger.info { "GuildId: ${guildId.asLong()} Adding audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers[loadResultHandler] =
            GlobalData.PLAYER_MANAGER.loadItemOrdered(guildId.asLong(), query, loadResultHandler)
    }

    fun removeHandler(loadResultHandler: AudioLoadResultHandler) {
        logger.info { "GuildId: ${guildId.asLong()} Removing audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers.remove(loadResultHandler)
    }

    fun destroy() {
        cancelLeave()
        loadResultHandlers.forEach { it.value.cancel(true) }
        loadResultHandlers.clear()
        scheduler.destroy()
        destroyed = true
    }

    private fun skip(): Boolean {
        return scheduler.skip()
    }

    private fun getMessageChannelId(): Snowflake {
        return Snowflake.of(messageChannelId.get())
    }

    private fun getMessageChannel(): Mono<MessageChannel> {
        return client.getChannelById(getMessageChannelId())
            .cast(MessageChannel::class.java)
    }

    private fun leaveMessage(): EmbedCreateSpec {
        return EmbedUtils.defaultEmbed()
            .description("Left the voice channel due to inactivity.")
            .build()
    }

}