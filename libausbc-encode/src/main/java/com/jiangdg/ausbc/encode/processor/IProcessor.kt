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
package com.jiangdg.ausbc.encode.processor

import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.encode.muxer.IMuxer
import kotlinx.coroutines.flow.Flow

/**
 * Media processor interface
 *
 * Base interface for media encoding processors (video/audio).
 * Processors handle raw media data input and produce encoded output.
 * This is a simplified placeholder implementation.
 *
 * @author Created for restructuring plan
 */
interface IProcessor {

    /**
     * Processor type
     */
    enum class Type {
        VIDEO_H264,
        VIDEO_H265,
        AUDIO_AAC,
        AUDIO_MP3,
        AUDIO_OPUS
    }

    /**
     * Get the processor type
     */
    fun getType(): Type

    /**
     * Initialize the processor
     *
     * @return true if initialization successful, false otherwise
     */
    suspend fun initialize(): Boolean

    /**
     * Start processing
     */
    suspend fun start()

    /**
     * Stop processing
     */
    suspend fun stop()

    /**
     * Pause processing
     */
    suspend fun pause()

    /**
     * Resume processing
     */
    suspend fun resume()

    /**
     * Check if processor is currently processing
     */
    fun isProcessing(): Boolean

    /**
     * Check if processor is initialized
     */
    fun isInitialized(): Boolean

    /**
     * Put raw data into processor
     *
     * @param data Raw media data (YUV for video, PCM for audio)
     */
    suspend fun putRawData(data: RawData)

    /**
     * Get flow of processed encoded data
     */
    fun getEncodedData(): Flow<ProcessedData>

    /**
     * Update bitrate for encoding
     *
     * @param bitrate Bitrate in bps
     */
    suspend fun updateBitRate(bitrate: Int)

    /**
     * Release processor resources
     */
    suspend fun release()

    /**
     * Set muxer for recording encoded data
     *
     * @param muxer Media muxer to write encoded data
     */
    suspend fun setMuxer(muxer: IMuxer?)

    /**
     * Get processor configuration
     */
    fun getConfig(): ProcessorConfig
}

/**
 * Processed encoded data
 *
 * @param data Encoded data buffer
 * @param type Data type (H264 key frame, SPS/PPS, etc.)
 * @param presentationTimeUs Presentation timestamp in microseconds
 * @param size Data size in bytes
 */
data class ProcessedData(
    val data: ByteArray,
    val type: DataType,
    val presentationTimeUs: Long,
    val size: Int = data.size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedData

        if (!data.contentEquals(other.data)) return false
        if (type != other.type) return false
        if (presentationTimeUs != other.presentationTimeUs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + presentationTimeUs.hashCode()
        return result
    }

    /**
     * Data type enum for encoded output
     */
    enum class DataType {
        // Video types
        VIDEO_H264_KEY,      // H.264 I-frame (key frame)
        VIDEO_H264_SPS,      // H.264 SPS/PPS
        VIDEO_H264,          // H.264 P-frame
        VIDEO_H265_KEY,      // H.265 I-frame
        VIDEO_H265_SPS,      // H.265 SPS/PPS/VPS
        VIDEO_H265,          // H.265 P-frame
        // Audio types
        AUDIO_AAC,           // AAC data (without ADTS)
        AUDIO_MP3,           // MP3 data
        AUDIO_OPUS,          // Opus data
        // Metadata
        METADATA             // Metadata
    }
}

/**
 * Processor configuration
 *
 * @param isVideo true if video processor, false if audio
 * @param bitrate Encoding bitrate in bps
 * @param configKey Configuration key for specific processor
 */
data class ProcessorConfig(
    val isVideo: Boolean,
    val bitrate: Int? = null,
    val configKey: String = ""
) {
    companion object {
        /**
         * Default video H.264 config
         */
        fun defaultH264(): ProcessorConfig = ProcessorConfig(
            isVideo = true,
            bitrate = 5_000_000,
            configKey = "h264"
        )

        /**
         * Default audio AAC config
         */
        fun defaultAAC(): ProcessorConfig = ProcessorConfig(
            isVideo = false,
            bitrate = 128000,
            configKey = "aac"
        )
    }
}
