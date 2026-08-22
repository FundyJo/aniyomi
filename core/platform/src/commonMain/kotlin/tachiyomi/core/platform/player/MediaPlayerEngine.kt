package tachiyomi.core.platform.player

import kotlinx.coroutines.flow.StateFlow

data class MediaTrack(
    val id: String,
    val label: String,
    val language: String? = null,
)

typealias AudioTrack = MediaTrack

typealias SubtitleTrack = MediaTrack

typealias VideoTrack = MediaTrack

data class MediaSubtitle(
    val url: String,
    val language: String? = null,
    val label: String? = null,
    val mimeType: String? = null,
)

data class PlayableMedia(
    val url: String,
    val title: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap(),
    val referer: String? = null,
    val userAgent: String? = null,
    val subtitles: List<MediaSubtitle> = emptyList(),
    val backupUrls: List<String> = emptyList(),
    val mimeType: String? = null,
)

enum class PlaybackState {
    Idle,
    Loading,
    Ready,
    Playing,
    Paused,
    Buffering,
    Ended,
    Error,
}

data class PlayerCapabilities(
    val externalSubtitles: Boolean,
    val playbackSpeed: Boolean,
    val pictureInPicture: Boolean,
    val customHeaders: Boolean,
    val multipleAudioTracks: Boolean,
)

data class PlayerState(
    val playbackState: PlaybackState,
    val positionMs: Long = 0,
    val durationMs: Long? = null,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val videoTracks: List<VideoTrack> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val error: String? = null,
)

interface MediaPlayerEngine {
    val state: StateFlow<PlayerState>

    val capabilities: PlayerCapabilities

    suspend fun load(media: PlayableMedia)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setVolume(volume: Float)

    fun setSpeed(speed: Float)

    fun selectAudioTrack(id: String?)

    fun selectSubtitleTrack(id: String?)

    fun stop()

    fun release()
}
