package com.example.pebtip.pebble

import io.rebble.libpebblecommon.connection.TokenProvider
import io.rebble.libpebblecommon.connection.WebServices
import io.rebble.libpebblecommon.voice.TranscriptionProvider
import io.rebble.libpebblecommon.services.WatchInfo
import io.rebble.libpebblecommon.connection.FirmwareUpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi

// Stubs implemented temporarily so LibPebble3 compiles and run
object NoOpWebServices : WebServices {
    override suspend fun fetchLocker() = null

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun removeFromLocker(id: Uuid): Boolean = false

    override suspend fun checkForFirmwareUpdate(watch: WatchInfo) =
        io.rebble.libpebblecommon.connection.FirmwareUpdateCheckResult.FoundNoUpdate

    override fun uploadMemfaultChunk(chunk: ByteArray, watchInfo: WatchInfo) {}

    override fun uploadAnalyticsHeartbeat(itemData: ByteArray, watchInfo: WatchInfo) {}
}

object NoOpTokenProvider : TokenProvider {
    override suspend fun getDevToken(): String? = null
}

object NoOpTranscriptionProvider : TranscriptionProvider {
    override suspend fun transcribe(
        encoderInfo: io.rebble.libpebblecommon.voice.VoiceEncoderInfo,
        audioFrames: Flow<UByteArray>,
        isNotificationReply: Boolean
    ): io.rebble.libpebblecommon.voice.TranscriptionResult {
        return io.rebble.libpebblecommon.voice.TranscriptionResult.Disabled
    }
    override suspend fun canServeSession() = false
}