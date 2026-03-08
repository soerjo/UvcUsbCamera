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
package com.jiangdg.ausbc.encode.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import com.jiangdg.ausbc.core.common.error.CameraError
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

/**
 * Media muxer interface
 *
 * Muxer combines encoded video and audio streams into a single file.
 * This is a simplified placeholder implementation.
 *
 * @author Created for restructuring plan
 */
interface IMuxer {

    /**
     * Muxer state
     */
    sealed class State {
        data object Idle : State()
        data object Initialized : State()
        data object Started : State()
        data object Stopped : State()
        data class Error(val error: CameraError) : State()
    }

    /**
     * Get the current muxer state
     */
    fun getState(): StateFlow<State>

    /**
     * Initialize the muxer
     *
     * @param outputPath Output file path
     * @return Unit on success
     */
    suspend fun initialize(outputPath: String): Unit

    /**
     * Add a media track
     *
     * @param format Media format for the track
     * @param isVideo true if video track, false if audio track
     * @return Track index
     */
    suspend fun addTrack(format: MediaFormat?, isVideo: Boolean): Int

    /**
     * Write encoded data to the muxer
     *
     * @param buffer Encoded data buffer
     * @param bufferInfo Buffer information
     * @param isVideo true if video data, false if audio data
     */
    suspend fun writeSampleData(
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        isVideo: Boolean
    ): Unit

    /**
     * Start the muxer
     */
    suspend fun start(): Unit

    /**
     * Stop and release the muxer
     *
     * @return Output path
     */
    suspend fun stop(): String

    /**
     * Release all muxer resources
     */
    suspend fun release()

    /**
     * Check if muxer is started
     */
    fun isStarted(): Boolean

    /**
     * Get the output path
     */
    fun getOutputPath(): String?

    /**
     * Set auto file splitting duration
     *
     * @param durationInSec Duration in seconds for auto-split (0 = no split)
     */
    fun setAutoSplitDuration(durationInSec: Long)

    /**
     * Set video-only mode (no audio)
     *
     * @param videoOnly true if video only, false otherwise
     */
    fun setVideoOnly(videoOnly: Boolean)
}

/**
 * Muxer factory
 *
 * Creates muxer instances based on output format.
 */
object MuxerFactory {

    /**
     * Muxer type enum
     */
    enum class MuxerType {
        MP4,
        WEBM
    }

    /**
     * Create a muxer instance
     *
     * @param type Muxer type
     * @return IMuxer instance
     */
    fun create(type: MuxerType = MuxerType.MP4): IMuxer {
        return when (type) {
            MuxerType.MP4 -> Mp4MuxerV2()
            MuxerType.WEBM -> throw UnsupportedOperationException("WEBM muxer not implemented yet")
        }
    }

    /**
     * Create MP4 muxer
     */
    fun createMp4(): IMuxer = Mp4MuxerV2()
}
