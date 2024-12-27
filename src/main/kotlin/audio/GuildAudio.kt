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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import util.defaultEmbedBuilder
import util.simpleMessageEmbed
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
    private val scheduler: AudioTrackScheduler = AudioTrackScheduler(player, guildId)
    private var destroyed: Boolean = false
    private var messageChannelId: AtomicLong = AtomicLong()
    private val leavingTask: AtomicReference<Disposable> = AtomicReference()
    private val menusTasks: HashMap<String, AtomicReference<Disposable>> = hashMapOf()
    private val loadResultHandlers: ConcurrentHashMap<AudioLoadResultHandler, Future<Void>> = ConcurrentHashMap()

    // TODO: Implement the equalizer
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

    fun play(songRequest: SongRequest) {
        scheduler.play(SongRequest(songRequest.audioTrack.makeClone(), songRequest.requestedBy))
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

    fun requestedBy(): RequestedBy? {
        return scheduler.requestedBy()
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
                GuildManager.getAudio(guildId).sendMessage(simpleMessageEmbed("Queue is empty."))
            }
            return skipped
        } else if (position < 0 || position > scheduler.getQueue().size) {
            return false
        }

        return scheduler.skipInQueue(position)
    }

    fun addHandler(loadResultHandler: AudioLoadResultHandler, query: String) {
        logger.debug { "GuildId: ${guildId.asLong()} Adding audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers[loadResultHandler] =
            GlobalData.PLAYER_MANAGER.loadItemOrdered(guildId.asLong(), query, loadResultHandler)
    }

    fun removeHandler(loadResultHandler: AudioLoadResultHandler) {
        logger.debug { "GuildId: ${guildId.asLong()} Removing audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers.remove(loadResultHandler)
    }

    fun destroy() {
        cancelLeave()
        loadResultHandlers.forEach { it.value.cancel(true) }
        loadResultHandlers.clear()
        scheduler.destroy()
        destroyed = true
    }

    fun flipRepeating() {
        scheduler.flipRepeating()
    }

    fun shuffleQueue(): Boolean {
        return scheduler.shuffleQueue()
    }

    private fun setMenuComponentTimeout(customId: String) {
        val task = client.on(SelectMenuInteractionEvent::class.java) {
            if (it.customId.equals(customId)) {
                val value = it.values.toString().replace("[", "").replace("]", "")
                val author = it.interaction.user
                val messageChannelMono = it.interaction.channel
                val member = it.interaction.member

                return@on JoinCommand().joinVoiceChannel(messageChannelMono, member, guildId)
                    .timeout(removeDelay).then(it.message.get().delete())
                    .then(mono {
                        logger.info { "Selected value while using search: $value" }
                        addHandler(DefaultAudioLoadResultHandler(guildId, author, value), value)
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

    private fun skip(): Boolean {
        return scheduler.skip()
    }

    private fun getMessageChannel(): Mono<MessageChannel> {
        return client.getChannelById(Snowflake.of(messageChannelId.get())).map { it as MessageChannel }
    }

    private fun leaveMessage(): EmbedCreateSpec {
        return defaultEmbedBuilder()
            .description("Left the voice channel due to inactivity.")
            .build()
    }

}