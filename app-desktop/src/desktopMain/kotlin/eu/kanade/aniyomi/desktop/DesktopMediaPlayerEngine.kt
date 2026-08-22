package eu.kanade.aniyomi.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference
import eu.kanade.tachiyomi.source.SourceSubtitle
import eu.kanade.tachiyomi.source.SourceTrack
import eu.kanade.tachiyomi.source.VideoSource
import eu.kanade.tachiyomi.source.network.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.Canvas
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

interface MediaPlayerEngine : AutoCloseable {
    val state: StateFlow<MediaPlayerEngineState>
    fun attach(canvas: Canvas)
    fun load(video: VideoSource, startPositionMs: Long = 0)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun setVolume(volume: Float)
    fun setMuted(muted: Boolean)
    fun setSpeed(speed: Float)
    fun selectAudioTrack(id: String?)
    fun selectSubtitleTrack(id: String?)
    fun release()
    override fun close() = release()
}

enum class MediaPlayerStatus {
    Idle,
    Loading,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error,
    Released,
}

data class MediaPlayerEngineState(
    val status: MediaPlayerStatus = MediaPlayerStatus.Idle,
    val video: VideoSource? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPercent: Float = 0f,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val speed: Float = 1f,
    val audioTracks: List<PlayerTrack> = emptyList(),
    val subtitleTracks: List<PlayerTrack> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val error: MediaPlayerError? = null,
) {
    val playing: Boolean = status == MediaPlayerStatus.Playing
}

data class PlayerTrack(
    val id: String,
    val language: String? = null,
    val title: String,
    val selected: Boolean = false,
)

data class MediaPlayerError(
    val type: MediaPlayerErrorType,
    val message: String,
)

enum class MediaPlayerErrorType {
    NetworkError,
    Unauthorized,
    InvalidStream,
    UnsupportedCodec,
    SourceError,
    PlayerInitializationError,
    NativeLibraryMissing,
}

class DesktopMediaPlayerEngine : MediaPlayerEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val released = AtomicBoolean(false)
    private val _state = MutableStateFlow(MediaPlayerEngineState())
    private var handle: Pointer? = null
    private var eventLoopStarted = false
    private var attachedCanvas: Canvas? = null

    override val state: StateFlow<MediaPlayerEngineState> = _state

    override fun attach(canvas: Canvas) {
        attachedCanvas = canvas
        val handle = ensureHandle() ?: return
        val windowId = Native.getComponentPointer(canvas).let { Pointer.nativeValue(it) }
        setPropertyString(handle, "wid", windowId.toString())
    }

    override fun load(video: VideoSource, startPositionMs: Long) {
        val handle = ensureHandle() ?: return
        _state.value = MediaPlayerEngineState(
            status = MediaPlayerStatus.Loading,
            video = video,
            positionMs = startPositionMs,
            volume = _state.value.volume,
            muted = _state.value.muted,
            speed = _state.value.speed,
            audioTracks = video.audioTracks.map { it.toPlayerTrack(selected = false) },
            subtitleTracks = listOf(PlayerTrack("no", title = "Off", selected = true)) + video.subtitles.mapIndexed { index, subtitle -> subtitle.toPlayerTrack(index) },
            selectedSubtitleTrackId = "no",
        )
        applyHeaders(handle, video.headers)
        setPropertyDouble(handle, "volume", _state.value.volume * 100.0)
        setPropertyFlag(handle, "mute", _state.value.muted)
        setPropertyDouble(handle, "speed", _state.value.speed.toDouble())
        command(handle, "loadfile", video.url, "replace")
        video.subtitles.forEach { subtitle -> command(handle, "sub-add", subtitle.url, "auto", subtitle.label ?: subtitle.language ?: "Subtitle") }
        if (startPositionMs > 0) {
            setPropertyDouble(handle, "time-pos", startPositionMs / 1000.0)
        }
    }

    override fun play() {
        ensureHandle()?.let { setPropertyFlag(it, "pause", false) }
        _state.update { it.copy(status = MediaPlayerStatus.Playing) }
    }

    override fun pause() {
        ensureHandle()?.let { setPropertyFlag(it, "pause", true) }
        _state.update { it.copy(status = MediaPlayerStatus.Paused) }
    }

    override fun togglePlayPause() {
        if (_state.value.playing) pause() else play()
    }

    override fun seekTo(positionMs: Long) {
        ensureHandle()?.let { setPropertyDouble(it, "time-pos", positionMs.coerceAtLeast(0) / 1000.0) }
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0)) }
    }

    override fun seekBy(deltaMs: Long) {
        val target = (_state.value.positionMs + deltaMs).coerceAtLeast(0)
        ensureHandle()?.let { command(it, "seek", (deltaMs / 1000.0).toString(), "relative", "exact") }
        _state.update { it.copy(positionMs = target) }
    }

    override fun setVolume(volume: Float) {
        val coerced = volume.coerceIn(0f, 1f)
        ensureHandle()?.let { setPropertyDouble(it, "volume", coerced * 100.0) }
        _state.update { it.copy(volume = coerced) }
    }

    override fun setMuted(muted: Boolean) {
        ensureHandle()?.let { setPropertyFlag(it, "mute", muted) }
        _state.update { it.copy(muted = muted) }
    }

    override fun setSpeed(speed: Float) {
        val coerced = speed.coerceIn(0.25f, 4f)
        ensureHandle()?.let { setPropertyDouble(it, "speed", coerced.toDouble()) }
        _state.update { it.copy(speed = coerced) }
    }

    override fun selectAudioTrack(id: String?) {
        ensureHandle()?.let { setPropertyString(it, "aid", id ?: "no") }
        _state.update { state ->
            state.copy(
                audioTracks = state.audioTracks.map { it.copy(selected = it.id == id) },
                selectedAudioTrackId = id,
            )
        }
    }

    override fun selectSubtitleTrack(id: String?) {
        val selected = id ?: "no"
        ensureHandle()?.let { setPropertyString(it, "sid", selected) }
        _state.update { state ->
            state.copy(
                subtitleTracks = state.subtitleTracks.map { it.copy(selected = it.id == selected) },
                selectedSubtitleTrackId = selected,
            )
        }
    }

    override fun release() {
        if (!released.compareAndSet(false, true)) return
        val current = handle
        handle = null
        current?.let { Mpv.INSTANCE.mpv_terminate_destroy(it) }
        scope.cancel()
        _state.update { it.copy(status = MediaPlayerStatus.Released) }
    }

    private fun ensureHandle(): Pointer? {
        if (released.get()) return null
        handle?.let { return it }
        val created = runCatching {
            val pointer = Mpv.INSTANCE.mpv_create()
                ?: throw IllegalStateException("mpv_create returned null")
            setOptionString(pointer, "terminal", "no")
            setOptionString(pointer, "msg-level", "all=warn")
            setOptionString(pointer, "force-window", "yes")
            setOptionString(pointer, "osc", "no")
            if (Mpv.INSTANCE.mpv_initialize(pointer) < 0) {
                throw IllegalStateException("mpv_initialize failed")
            }
            observe(pointer, "time-pos", MPV_FORMAT_DOUBLE)
            observe(pointer, "duration", MPV_FORMAT_DOUBLE)
            observe(pointer, "pause", MPV_FORMAT_FLAG)
            observe(pointer, "eof-reached", MPV_FORMAT_FLAG)
            observe(pointer, "cache-buffering-state", MPV_FORMAT_DOUBLE)
            observe(pointer, "volume", MPV_FORMAT_DOUBLE)
            observe(pointer, "mute", MPV_FORMAT_FLAG)
            observe(pointer, "speed", MPV_FORMAT_DOUBLE)
            pointer
        }.onFailure { error ->
            _state.update {
                it.copy(
                    status = MediaPlayerStatus.Error,
                    error = MediaPlayerError(error.toPlayerErrorType(), error.message ?: error::class.simpleName.orEmpty()),
                )
            }
        }.getOrNull()
        handle = created
        if (created != null && !eventLoopStarted) {
            eventLoopStarted = true
            scope.launch { eventLoop(created) }
        }
        attachedCanvas?.let { attach(it) }
        return created
    }

    private fun eventLoop(handle: Pointer) {
        while (!released.get() && this.handle == handle) {
            val pointer = Mpv.INSTANCE.mpv_wait_event(handle, 0.25) ?: continue
            val event = MpvEvent(pointer)
            event.read()
            when (event.event_id) {
                MPV_EVENT_FILE_LOADED -> _state.update { if (it.status == MediaPlayerStatus.Loading) it.copy(status = MediaPlayerStatus.Playing) else it }
                MPV_EVENT_END_FILE -> _state.update { it.copy(status = MediaPlayerStatus.Ended) }
                MPV_EVENT_PLAYBACK_RESTART -> _state.update { if (it.status == MediaPlayerStatus.Buffering) it.copy(status = MediaPlayerStatus.Playing) else it }
                MPV_EVENT_PROPERTY_CHANGE -> handleProperty(event.data)
                MPV_EVENT_SHUTDOWN -> return
            }
        }
    }

    private fun handleProperty(data: Pointer?) {
        data ?: return
        val property = MpvEventProperty(data)
        property.read()
        when (property.name) {
            "time-pos" -> property.doubleValue()?.let { seconds -> _state.update { it.copy(positionMs = (seconds * 1000).roundToLong()) } }
            "duration" -> property.doubleValue()?.let { seconds -> _state.update { it.copy(durationMs = (seconds * 1000).roundToLong()) } }
            "pause" -> property.flagValue()?.let { paused -> _state.update { state -> state.copy(status = if (paused) MediaPlayerStatus.Paused else MediaPlayerStatus.Playing) } }
            "eof-reached" -> property.flagValue()?.takeIf { it }?.let { _state.update { state -> state.copy(status = MediaPlayerStatus.Ended) } }
            "cache-buffering-state" -> property.doubleValue()?.let { percent ->
                _state.update { state ->
                    state.copy(
                        bufferedPercent = (percent / 100.0).toFloat().coerceIn(0f, 1f),
                        status = if (percent < 100.0 && state.playing) MediaPlayerStatus.Buffering else state.status,
                    )
                }
            }
            "volume" -> property.doubleValue()?.let { value -> _state.update { it.copy(volume = (value / 100.0).toFloat().coerceIn(0f, 1f)) } }
            "mute" -> property.flagValue()?.let { muted -> _state.update { it.copy(muted = muted) } }
            "speed" -> property.doubleValue()?.let { speed -> _state.update { it.copy(speed = speed.toFloat()) } }
        }
    }

    private fun applyHeaders(handle: Pointer, headers: HttpHeaders) {
        val sanitized = headers.toList()
            .filterNot { (name, value) -> name.isBlank() || value.isBlank() }
            .map { (name, value) -> "${name.trim()}: ${value.trim()}" }
        setOptionString(handle, "http-header-fields", sanitized.joinToString(","))
        headers.get("User-Agent")?.let { setOptionString(handle, "user-agent", it) }
        headers.get("Referer")?.let { setOptionString(handle, "referrer", it) }
        headers.get("Cookie")?.let { setOptionString(handle, "cookies", "yes") }
    }

    private fun observe(handle: Pointer, name: String, format: Int) {
        Mpv.INSTANCE.mpv_observe_property(handle, 0, name, format)
    }

    private fun command(handle: Pointer, vararg args: String) {
        Mpv.INSTANCE.mpv_command_async(handle, 0, arrayOf(*args, null))
    }

    private fun setOptionString(handle: Pointer, name: String, value: String) {
        Mpv.INSTANCE.mpv_set_option_string(handle, name, value)
    }

    private fun setPropertyString(handle: Pointer, name: String, value: String) {
        Mpv.INSTANCE.mpv_set_property_string(handle, name, value)
    }

    private fun setPropertyFlag(handle: Pointer, name: String, value: Boolean) {
        Mpv.INSTANCE.mpv_set_property_string(handle, name, if (value) "yes" else "no")
    }

    private fun setPropertyDouble(handle: Pointer, name: String, value: Double) {
        Mpv.INSTANCE.mpv_set_property_string(handle, name, value.toString())
    }
}

private fun SourceTrack.toPlayerTrack(selected: Boolean): PlayerTrack {
    return PlayerTrack(id = id, language = language, title = label, selected = selected)
}

private fun SourceSubtitle.toPlayerTrack(index: Int): PlayerTrack {
    val title = label ?: language ?: url.substringAfterLast('/').ifBlank { "Subtitle ${index + 1}" }
    return PlayerTrack(id = (index + 1).toString(), language = language, title = title, selected = false)
}

private fun Throwable.toPlayerErrorType(): MediaPlayerErrorType {
    val text = listOfNotNull(message, cause?.message).joinToString(" ").lowercase(Locale.ROOT)
    return when {
        this is UnsatisfiedLinkError -> MediaPlayerErrorType.NativeLibraryMissing
        "401" in text || "unauthorized" in text || "forbidden" in text -> MediaPlayerErrorType.Unauthorized
        "network" in text || "resolve" in text || "timeout" in text || "connection" in text -> MediaPlayerErrorType.NetworkError
        "codec" in text || "decoder" in text -> MediaPlayerErrorType.UnsupportedCodec
        "url" in text || "stream" in text || "loadfile" in text -> MediaPlayerErrorType.InvalidStream
        else -> MediaPlayerErrorType.PlayerInitializationError
    }
}

private interface Mpv : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_command_async(ctx: Pointer, reply_userdata: Long, args: Array<String?>): Int
    fun mpv_set_option_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_set_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_get_property(ctx: Pointer, name: String, format: Int, data: PointerByReference): Int
    fun mpv_observe_property(ctx: Pointer, reply_userdata: Long, name: String, format: Int): Int
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer?
    fun mpv_terminate_destroy(ctx: Pointer)

    companion object {
        val INSTANCE: Mpv = Native.load("mpv-2", Mpv::class.java)
    }
}

@Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
private class MpvEvent(pointer: Pointer) : Structure(pointer) {
    @JvmField var event_id: Int = 0
    @JvmField var error: Int = 0
    @JvmField var reply_userdata: Long = 0
    @JvmField var data: Pointer? = null
}

@Structure.FieldOrder("name", "format", "data")
private class MpvEventProperty(pointer: Pointer) : Structure(pointer) {
    @JvmField var name: String? = null
    @JvmField var format: Int = 0
    @JvmField var data: Pointer? = null

    fun doubleValue(): Double? = if (format == MPV_FORMAT_DOUBLE) data?.getDouble(0) else null
    fun flagValue(): Boolean? = if (format == MPV_FORMAT_FLAG) data?.getInt(0)?.let { it != 0 } else null
}

private const val MPV_FORMAT_FLAG = 3
private const val MPV_FORMAT_DOUBLE = 5
private const val MPV_EVENT_SHUTDOWN = 1
private const val MPV_EVENT_END_FILE = 7
private const val MPV_EVENT_FILE_LOADED = 8
private const val MPV_EVENT_PLAYBACK_RESTART = 21
private const val MPV_EVENT_PROPERTY_CHANGE = 22
