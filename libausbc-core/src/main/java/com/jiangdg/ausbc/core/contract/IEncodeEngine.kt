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

import com.jiangdg.ausbc.core.common.result.EncodeResult
import com.jiangdg.ausbc.core.domain.model.EncodeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Encode engine interface for media encoding
 *
 * Handles video and audio encoding for capture and streaming.
 *
 * @author Created for restructuring plan
 */
interface IEncodeEngine {

    /**
     * Initialize the encoder with configuration
     *
     * @param config Encode configuration
     * @return EncodeResult indicating success or failure
     */
    suspend fun initialize(config: EncodeConfig): EncodeResult

    /**
     * Start encoding
     *
     * @return EncodeResult indicating success or failure
     */
    suspend fun startEncoding(): EncodeResult

    /**
     * Stop encoding and finalize output
     *
     * @return EncodeResult with output path on success
     */
    suspend fun stopEncoding(): EncodeResult

    /**
     * Pause encoding
     *
     * @return EncodeResult indicating success or failure
     */
    suspend fun pauseEncoding(): EncodeResult

    /**
     * Resume encoding
     *
     * @return EncodeResult indicating success or failure
     */
    suspend fun resumeEncoding(): EncodeResult

    /**
     * Flow of encoded data
     *
     * Collect this flow to receive encoded frames in real-time.
     * Useful for streaming scenarios.
     */
    fun getEncodedData(): Flow<EncodedData>

    /**
     * Get current encoding state
     */
    fun getEncodeState(): EncodeState

    /**
     * Release encoder resources
     */
    suspend fun release()

    /**
     * Check if encoder is initialized
     */
    fun isInitialized(): Boolean

    /**
     * Check if encoder is currently encoding
     */
    fun isEncoding(): Boolean
}

/**
 * Encoded data container
 */
data class EncodedData(
    val data: ByteArray,
    val type: DataType,
    val isKeyFrame: Boolean = false,
    val presentationTimeUs: Long,
    val size: Int = data.size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncodedData

        if (!data.contentEquals(other.data)) return false
        if (type != other.type) return false
        if (isKeyFrame != other.isKeyFrame) return false
        if (presentationTimeUs != other.presentationTimeUs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + isKeyFrame.hashCode()
        result = 31 * result + presentationTimeUs.hashCode()
        return result
    }

    /**
     * Data type enum
     */
    enum class DataType {
        VIDEO_H264,
        AUDIO_AAC,
        METADATA
    }
}

/**
 * Encode state enum
 */
sealed class EncodeState {
    data object Idle : EncodeState()
    data object Initialized : EncodeState()
    data object Encoding : EncodeState()
    data object Paused : EncodeState()
    data class Stopping(val outputPath: String?) : EncodeState()
    data class Error(val error: String) : EncodeState()
}
