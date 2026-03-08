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

import android.graphics.SurfaceTexture
import android.view.Surface
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.common.result.CaptureResult
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import com.jiangdg.ausbc.core.domain.model.CameraCapabilities
import com.jiangdg.ausbc.core.domain.model.CameraState
import com.jiangdg.ausbc.core.domain.model.PreviewSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core camera interface defining all camera operations
 *
 * This is the modern runtime API that replaces the deprecated ICameraStrategy.
 * All camera implementations should implement this interface.
 *
 * Key features:
 * - Coroutine-based async operations
 * - StateFlow for reactive state management
 * - Flow-based preview frame streaming
 * - Type-safe error handling with CameraResult
 *
 * @author Created for restructuring plan
 */
interface ICamera {

    /**
     * Get the current camera state as a StateFlow
     *
     * Collect this flow to observe camera state changes.
     * States: Idle, Opening, Opened, Closed, Error
     */
    val cameraState: StateFlow<CameraState>

    /**
     * Flow of preview frames
     *
     * Collect this flow to receive preview frame data in real-time.
     * The flow emits frames as they become available from the camera.
     */
    val previewFrames: Flow<PreviewFrame>

    /**
     * Open the camera with the specified request
     *
     * @param request Camera configuration request
     * @return CameraResult indicating success or failure
     */
    suspend fun open(request: CameraRequest): CameraResult<Unit>

    /**
     * Close the camera and release resources
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun close(): CameraResult<Unit>

    /**
     * Start camera preview
     *
     * @param surface Target surface for rendering
     * @return CameraResult indicating success or failure
     */
    suspend fun startPreview(surface: Surface): CameraResult<Unit>

    /**
     * Start camera preview to a SurfaceTexture
     *
     * @param surfaceTexture Target surface texture for rendering
     * @return CameraResult indicating success or failure
     */
    suspend fun startPreview(surfaceTexture: SurfaceTexture): CameraResult<Unit>

    /**
     * Stop camera preview
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun stopPreview(): CameraResult<Unit>

    /**
     * Capture a still image
     *
     * @param request Capture request configuration
     * @return CaptureResult with the captured file path
     */
    suspend fun captureImage(request: CaptureRequest): CaptureResult

    /**
     * Start video recording
     *
     * @param request Video recording configuration
     * @return CaptureResult indicating recording started
     */
    suspend fun startVideoRecording(request: VideoRecordRequest): CaptureResult

    /**
     * Stop video recording
     *
     * @return CaptureResult with the recorded file info
     */
    suspend fun stopVideoRecording(): CaptureResult

    /**
     * Start audio recording
     *
     * @param request Audio recording configuration
     * @return CaptureResult indicating recording started
     */
    suspend fun startAudioRecording(request: AudioRecordRequest): CaptureResult

    /**
     * Stop audio recording
     *
     * @return CaptureResult with the recorded file info
     */
    suspend fun stopAudioRecording(): CaptureResult

    /**
     * Get all supported preview sizes
     *
     * @param aspectRatio Optional aspect ratio filter (e.g., 16.0/9.0 for 16:9)
     * @return List of supported preview sizes
     */
    fun getPreviewSizes(aspectRatio: Double? = null): List<PreviewSize>

    /**
     * Get camera capabilities
     *
     * @return CameraCapabilities describing supported features
     */
    fun getCapabilities(): CameraCapabilities

    /**
     * Check if camera is currently opened
     */
    fun isOpened(): Boolean

    /**
     * Update camera resolution
     *
     * @param width New preview width
     * @param height New preview height
     * @return CameraResult indicating success or failure
     */
    suspend fun updateResolution(width: Int, height: Int): CameraResult<Unit>

    /**
     * Add a preview data callback for raw frame data
     *
     * @param callback Callback to receive preview frames
     */
    fun addPreviewCallback(callback: PreviewCallback)

    /**
     * Remove a preview data callback
     *
     * @param callback Callback to remove
     */
    fun removePreviewCallback(callback: PreviewCallback)
}

/**
 * Preview frame data container
 */
data class PreviewFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val format: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreviewFrame

        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (format != other.format) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + format
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Capture request for still images
 */
data class CaptureRequest(
    val savePath: String,
    val format: CaptureFormat = CaptureFormat.JPEG
)

/**
 * Video recording request
 */
data class VideoRecordRequest(
    val savePath: String,
    val durationMs: Long? = null,
    val quality: VideoQuality = VideoQuality.HIGH
)

/**
 * Audio recording request
 */
data class AudioRecordRequest(
    val savePath: String,
    val durationMs: Long? = null
)

/**
 * Preview callback interface
 */
fun interface PreviewCallback {
    fun onPreviewFrame(frame: PreviewFrame)
}

/**
 * Capture format enum
 */
enum class CaptureFormat {
    JPEG,
    PNG,
    RAW
}

/**
 * Video quality enum
 */
enum class VideoQuality {
    LOW,
    MEDIUM,
    HIGH,
    UHD
}
