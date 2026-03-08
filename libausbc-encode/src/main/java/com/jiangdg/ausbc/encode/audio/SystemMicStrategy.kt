/*
 * Copyright 2017-2023 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.encode.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import com.jiangdg.ausbc.core.contract.AudioConfig
import com.jiangdg.ausbc.core.contract.AudioData
import com.jiangdg.ausbc.core.contract.AudioStrategyType
import com.jiangdg.ausbc.core.contract.IAudioStrategy
import com.jiangdg.ausbc.core.common.result.AudioResult
import com.jiangdg.ausbc.core.common.error.CameraError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * System microphone audio strategy
 *
 * Implements IAudioStrategy for system microphone input.
 * This is a simplified placeholder implementation.
 *
 * @author Created for restructuring plan
 */
class SystemMicStrategy(
    private val context: Context
) : IAudioStrategy {

    // Calculate buffer size first, then create audioConfig
    // This avoids circular reference since audioConfig needs bufferSize
    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_IN_CONFIG,
        AUDIO_FORMAT_16BIT
    )

    override val audioConfig: AudioConfig = AudioConfig(
        sampleRate = SAMPLE_RATE,
        channelCount = CHANNEL_COUNT,
        bitDepth = BIT_DEPTH,
        bufferSize = bufferSize
    )

    private var audioRecord: AudioRecord? = null
    private val audioDataChannel = Channel<AudioData>(Channel.BUFFERED)
    private var isRecording = false
    private var audioJob: Job? = null

    override suspend fun initialize(): AudioResult {
        return try {
            if (audioRecord != null) {
                throw IllegalStateException("Audio strategy already initialized")
            }

            @Suppress("MissingPermission")
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            audioRecord = AudioRecord(
                AUDIO_RECORD_SOURCE,
                SAMPLE_RATE,
                CHANNEL_IN_CONFIG,
                AUDIO_FORMAT_16BIT,
                bufferSize
            )

            AudioResult.Initialized(
                sampleRate = SAMPLE_RATE,
                channelCount = CHANNEL_COUNT
            )
        } catch (e: Exception) {
            AudioResult.AudioFailed(
                CameraError.EncodeError(
                    reason = "Failed to initialize system microphone",
                    encoderType = CameraError.EncoderType.AAC,
                    cause = e
                )
            )
        }
    }

    override suspend fun startRecording(): AudioResult {
        return try {
            if (audioRecord == null) {
                throw IllegalStateException("Audio strategy not initialized")
            }

            audioRecord?.startRecording()
            isRecording = true

            // Start audio reading loop
            startAudioLoop()

            AudioResult.RecordingStarted(bufferSize = bufferSize)
        } catch (e: Exception) {
            AudioResult.AudioFailed(
                CameraError.EncodeError(
                    reason = "Failed to start recording",
                    encoderType = CameraError.EncoderType.AAC,
                    cause = e
                )
            )
        }
    }

    private fun startAudioLoop() {
        audioJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording && audioRecord != null) {
                try {
                    val readBytes = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (readBytes > 0) {
                        val audioData = AudioData(
                            data = buffer.copyOf(),
                            size = readBytes,
                            timestamp = System.nanoTime() / 1000,
                            sampleRate = SAMPLE_RATE,
                            channelCount = CHANNEL_COUNT
                        )
                        audioDataChannel.trySend(audioData)
                    }
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }
    }

    override suspend fun stopRecording(): AudioResult {
        return try {
            if (audioRecord == null) {
                throw IllegalStateException("Audio strategy not initialized")
            }

            audioRecord?.stop()
            isRecording = false
            audioJob?.cancel()
            audioJob = null

            AudioResult.RecordingStopped(
                outputPath = "", // SystemMicStrategy doesn't save to file
                durationMs = 0 // Duration tracking not implemented
            )
        } catch (e: Exception) {
            AudioResult.AudioFailed(
                CameraError.EncodeError(
                    reason = "Failed to stop recording",
                    encoderType = CameraError.EncoderType.AAC,
                    cause = e
                )
            )
        }
    }

    override fun getAudioData(): Flow<AudioData> = audioDataChannel.receiveAsFlow()

    override suspend fun release() {
        try {
            isRecording = false
            audioJob?.cancel()
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore release errors
        }
        audioRecord = null
        audioJob = null
        audioDataChannel.close()
    }

    override fun isInitialized(): Boolean = audioRecord != null

    override fun isRecording(): Boolean = isRecording

    override fun getType(): AudioStrategyType = AudioStrategyType.SYSTEM_MIC

    companion object {
        private const val TAG = "SystemMicStrategy"
        private const val SAMPLE_RATE = 8000
        private const val CHANNEL_COUNT = 1
        private const val BIT_DEPTH = 16
        private const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT_16BIT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_RECORD_SOURCE = MediaRecorder.AudioSource.MIC
    }
}
