package audio

import GlobalData
import audio.load.DefaultAudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import util.CANCEL_TEXT
import util.Message
import util.defaultEmbedBuilder
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GuildAudio(
    private val client: GatewayDiscordClient,
    private val guildId: Snowflake,
    val player: AudioPlayer = GlobalData.PLAYER_MANAGER.createPlayer(),
    private val scheduler: AudioTrackScheduler = AudioTrackScheduler(player, guildId)
) : TrackScheduling by scheduler {
    private val logger = KotlinLogging.logger {}

    private val leaveDelay = Duration.ofMinutes(5)
    private val menuDelay = Duration.ofMinutes(1)
    private val removeDelay = Duration.ofSeconds(1)

    val guildName = client.getGuildById(guildId).block()?.name ?: "Unknown"

    private var destroyed = false
    private val messageChannelId = AtomicLong()
    private val leavingTask = AtomicReference<Disposable>()
    private val menusTasks = hashMapOf<String, AtomicReference<Disposable>>()
    private val loadResultHandlers = ConcurrentHashMap<AudioLoadResultHandler, Future<Void>>()
    private val equalizer = EqualizerFactory()

    init {
        player.addListener(scheduler)
        player.setFilterFactory(equalizer)
    }

    fun scheduleLeave() {
        if (destroyed || isLeavingScheduled()) {
            return
        }

        leavingTask.set(
            Mono.delay(leaveDelay, Schedulers.boundedElastic())
                .filter { isLeavingScheduled() }
                .flatMap { client.voiceConnectionRegistry.getVoiceConnection(guildId) }
                .flatMap { it.disconnect() }
                .then(mono { sendMessage(leaveMessage()) })
                .then(mono { GuildManager.destroyAudio(guildId) })
                .subscribe()
        )
        logger.info { "Bot leave scheduled in guild: $guildName." }
    }

    fun cancelLeave() {
        if (!isLeavingScheduled()) return
        leavingTask.get().dispose()
        logger.info { "Bot leave canceled in guild: $guildName." }
    }

    fun isLeavingScheduled(): Boolean = leavingTask.get()?.isDisposed?.not() ?: false

    fun setMessageChannelId(messageChannelId: Snowflake) {
        this.messageChannelId.set(messageChannelId.asLong())
    }

    fun sendMessage(embedCreateSpec: EmbedCreateSpec) {
        getMessageChannel().flatMap { it.createMessage(embedCreateSpec) }.subscribe()
    }

    fun sendMessageWithComponentAndTimeout(
        embedCreateSpec: EmbedCreateSpec,
        actionRows: Array<ActionRow>,
        customId: String
    ) {
        sendMessageWithComponent(embedCreateSpec, actionRows)
        setButtonsComponentsTimeout(customId)
    }

    override fun play(songRequest: SongRequest) =
        scheduler.play(SongRequest(songRequest.audioTrack.makeClone(), songRequest.requestedBy))

    override fun skipInQueue(position: Int) = if (position == 0) next() else scheduler.skipInQueue(position)

    fun addHandler(loadResultHandler: AudioLoadResultHandler, query: String) {
        logger.debug { "GuildId: ${guildId.asLong()} Adding audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers[loadResultHandler] =
            GlobalData.PLAYER_MANAGER.loadItemOrdered(guildId.asLong(), query, loadResultHandler)
    }

    fun removeHandler(loadResultHandler: AudioLoadResultHandler) {
        logger.debug { "GuildId: ${guildId.asLong()} Removing audio load result handler: ${loadResultHandler.hashCode()}" }
        loadResultHandlers.remove(loadResultHandler)
    }

    override fun destroy() {
        cancelLeave()
        loadResultHandlers.forEach { it.value.cancel(true) }
        loadResultHandlers.clear()
        scheduler.destroy()
        destroyed = true
    }

    private fun setButtonsComponentsTimeout(customId: String) {
        val task = client.on(ButtonInteractionEvent::class.java) {
            if (it.customId.startsWith(customId)) {
                val value = it.customId.substringAfter("-")
                val author = it.interaction.user

                if (value == CANCEL_TEXT) {
                    return@on Mono.delay(removeDelay)
                        .then(it.message.get().delete())
                        .then(mono { menusTasks[customId]?.get()?.dispose() })
                }

                return@on mono {
                    logger.info { "Selected value while using search: $value in guild: $guildName." }
                    addHandler(DefaultAudioLoadResultHandler(guildId, author, value), value)
                }
                    .flatMap { Mono.delay(removeDelay) }
                    .then(it.message.get().delete())
                    .then(mono { menusTasks[customId]?.get()?.dispose() })
            }
            return@on mono { null }
        }.timeout(menuDelay)
            .onErrorResume(TimeoutException::class.java) { mono { logger.info { "Menu item timed out." } } }
            .then(mono { menusTasks[customId]?.get()?.dispose() })

        menusTasks[customId] = AtomicReference(task.subscribe())
    }

    private fun sendMessageWithComponent(embedCreateSpec: EmbedCreateSpec, actionRows: Array<ActionRow>) {
        val embedBuilder = MessageCreateSpec.builder().addEmbed(embedCreateSpec)
        actionRows.forEach { embedBuilder.addComponent(it) }
        getMessageChannel().flatMap { it.createMessage(embedBuilder.build()) }.subscribe()
    }

    private fun getMessageChannel() =
        client.getChannelById(Snowflake.of(messageChannelId.get())).map { it as MessageChannel }

    private fun leaveMessage() = defaultEmbedBuilder().description(Message.INACTIVITY.message).build()
}