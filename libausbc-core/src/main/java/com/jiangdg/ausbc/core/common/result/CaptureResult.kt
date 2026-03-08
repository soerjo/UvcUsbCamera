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
package com.jiangdg.ausbc.core.common.result

import com.jiangdg.ausbc.core.common.error.CameraError

/**
 * Result wrapper for capture operations (image/video)
 *
 * @author Created for restructuring plan
 */
sealed class CaptureResult {
    /**
     * Image capture started successfully
     */
    data class ImageCaptureStarted(val filePath: String) : CaptureResult()

    /**
     * Image capture completed successfully
     */
    data class ImageCaptureComplete(val filePath: String, val size: Long) : CaptureResult()

    /**
     * Video recording started successfully
     */
    data class VideoRecordingStarted(val filePath: String) : CaptureResult()

    /**
     * Video recording stopped successfully
     */
    data class VideoRecordingComplete(
        val filePath: String,
        val durationMs: Long,
        val size: Long
    ) : CaptureResult()

    /**
     * Audio recording started successfully
     */
    data class AudioRecordingStarted(val filePath: String) : CaptureResult()

    /**
     * Audio recording stopped successfully
     */
    data class AudioRecordingComplete(
        val filePath: String,
        val durationMs: Long,
        val size: Long
    ) : CaptureResult()

    /**
     * Capture operation failed
     */
    data class CaptureFailed(val error: CameraError) : CaptureResult()

    /**
     * Capture was cancelled by user
     */
    data object CaptureCancelled : CaptureResult()

    /**
     * Capture is in progress
     */
    data class CaptureInProgress(val progress: Float) : CaptureResult()

    /**
     * Check if the result is a success
     */
    val isSuccess: Boolean
        get() = this !is CaptureFailed && this !is CaptureCancelled

    companion object {
        /**
         * Create a failed capture result
         */
        fun failed(error: CameraError): CaptureResult = CaptureFailed(error)
    }
}

/**
 * Result wrapper for encoding operations
 */
sealed class EncodeResult {
    /**
     * Encoder initialized successfully
     */
    data object Initialized : EncodeResult()

    /**
     * Encoding started successfully
     */
    data object Started : EncodeResult()

    /**
     * Encoding stopped successfully
     */
    data class Stopped(val outputPath: String, val durationMs: Long) : EncodeResult()

    /**
     * Encoding is in progress
     */
    data class EncodingProgress(val progress: Float, val bitrate: Long) : EncodeResult()

    /**
     * Data encoded successfully
     */
    data class DataEncoded(
        val dataType: DataType,
        val size: Int,
        val timestamp: Long
    ) : EncodeResult()

    /**
     * Encode operation failed
     */
    data class EncodeFailed(val error: CameraError) : EncodeResult()

    /**
     * Check if the result is a success
     */
    val isSuccess: Boolean
        get() = this !is EncodeFailed

    /**
     * Data type enumeration
     */
    enum class DataType {
        VIDEO_FRAME,
        AUDIO_FRAME,
        METADATA
    }

    companion object {
        /**
         * Create a failed encode result
         */
        fun failed(error: CameraError): EncodeResult = EncodeFailed(error)
    }
}
