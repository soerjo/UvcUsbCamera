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
package com.jiangdg.ausbc.core.contract

import com.jiangdg.ausbc.core.common.result.AudioResult
import kotlinx.coroutines.flow.Flow

/**
 * Audio strategy interface for audio capture
 *
 * Defines the contract for different audio sources:
 * - System microphone
 * - USB Audio Class (UAC) device
 *
 * @author Created for restructuring plan
 */
interface IAudioStrategy {

    /**
     * Audio configuration
     */
    val audioConfig: AudioConfig

    /**
     * Initialize the audio strategy
     *
     * @return AudioResult with configuration on success
     */
    suspend fun initialize(): AudioResult

    /**
     * Start audio recording
     *
     * @return AudioResult indicating success or failure
     */
    suspend fun startRecording(): AudioResult

    /**
     * Stop audio recording
     *
     * @return AudioResult indicating success or failure
     */
    suspend fun stopRecording(): AudioResult

    /**
     * Flow of audio data
     *
     * Collect this flow to receive audio frames in real-time.
     */
    fun getAudioData(): Flow<AudioData>

    /**
     * Release audio resources
     */
    suspend fun release()

    /**
     * Check if audio strategy is initialized
     */
    fun isInitialized(): Boolean

    /**
     * Check if audio strategy is currently recording
     */
    fun isRecording(): Boolean

    /**
     * Get audio strategy type
     */
    fun getType(): AudioStrategyType
}

/**
 * Audio configuration
 */
data class AudioConfig(
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    val bitDepth: Int = 16,
    val bufferSize: Int = 4096
)

/**
 * Audio data container
 */
data class AudioData(
    val data: ByteArray,
    val size: Int,
    val timestamp: Long,
    val sampleRate: Int,
    val channelCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioData

        if (!data.contentEquals(other.data)) return false
        if (size != other.size) return false
        if (timestamp != other.timestamp) return false
        if (sampleRate != other.sampleRate) return false
        if (channelCount != other.channelCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + size
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        return result
    }
}

/**
 * Audio strategy type enum
 */
enum class AudioStrategyType {
    SYSTEM_MIC,
    UAC,
    NONE
}
