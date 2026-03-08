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
package com.jiangdg.ausbc.camera.uvc

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.view.Surface
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.common.result.CaptureResult
import com.jiangdg.ausbc.core.contract.ICamera
import com.jiangdg.ausbc.core.contract.IRenderEngine
import com.jiangdg.ausbc.core.contract.PreviewCallback
import com.jiangdg.ausbc.core.contract.PreviewFrame
import com.jiangdg.ausbc.core.contract.CaptureRequest
import com.jiangdg.ausbc.core.contract.VideoRecordRequest
import com.jiangdg.ausbc.core.contract.AudioRecordRequest
import com.jiangdg.ausbc.core.domain.model.CameraCapabilities
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import com.jiangdg.ausbc.core.domain.model.CameraState
import com.jiangdg.ausbc.core.domain.model.PreviewSize
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.uvc.UVCCamera
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * UVC Camera implementation
 *
 * Implements the ICamera interface for USB Video Class devices.
 * Wraps the legacy UVCCamera class with modern coroutine-based API.
 *
 * @author Created for restructuring plan
 */
class UvcCamera @Inject constructor(
    private val context: Context,
    private val device: UsbDevice,
    private val renderEngine: IRenderEngine?
) : ICamera {

    private val mutex = Mutex()
    private var uvcCamera: UVCCamera? = null
    private var isPreviewing = false
    private var currentRequest: CameraRequest? = null
    private val previewCallbacks = mutableSetOf<PreviewCallback>()

    // State management
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    override val cameraState = _cameraState.asStateFlow()

    // Preview frames
    private val _previewFrames = MutableSharedFlow<PreviewFrame>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val previewFrames: Flow<PreviewFrame> = _previewFrames

    private var currentSurface: Any? = null // Surface or SurfaceTexture

    /**
     * Initialize the UVC camera
     */
    suspend fun initialize(): CameraResult<Unit> = mutex.withLock {
        if (_cameraState.value !is CameraState.Idle) {
            return CameraResult.error(CameraError.Busy)
        }

        try {
            _cameraState.value = CameraState.Opening

            // Create UVCCamera instance
            uvcCamera = UVCCamera()

            _cameraState.value = CameraState.Idle
            CameraResult.success(Unit)
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error(
                CameraError.OpenFailed(
                    reason = "Failed to initialize UVC camera",
                    cause = e
                )
            )
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Failed to initialize UVC camera",
                    cause = e
                )
            )
        }
    }

    override suspend fun open(request: CameraRequest): CameraResult<Unit> = mutex.withLock {
        if (_cameraState.value !is CameraState.Idle) {
            return CameraResult.error(CameraError.Busy)
        }

        try {
            _cameraState.value = CameraState.Opening
            currentRequest = request

            // TODO: Actually open the USB device and start camera
            // For now, simulate opening
            _cameraState.value = CameraState.Opened(
                previewWidth = request.previewWidth,
                previewHeight = request.previewHeight
            )

            Logger.i(TAG, "Camera opened: ${device.deviceName}")
            CameraResult.success(Unit)
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error(
                CameraError.OpenFailed(
                    reason = "Failed to open camera",
                    cause = e
                )
            )
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Failed to open camera",
                    cause = e
                )
            )
        }
    }

    override suspend fun close(): CameraResult<Unit> = mutex.withLock {
        if (_cameraState.value is CameraState.Idle) {
            return CameraResult.success(Unit)
        }

        try {
            // Stop preview if running
            if (isPreviewing) {
                stopPreviewInternal()
            }

            // Close UVC camera
            uvcCamera?.close()
            uvcCamera = null

            currentSurface = null
            currentRequest = null
            isPreviewing = false

            _cameraState.value = CameraState.Idle

            Logger.i(TAG, "Camera closed: ${device.deviceName}")
            CameraResult.success(Unit)
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error(
                CameraError.Closed
            )
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Failed to close camera",
                    cause = e
                )
            )
        }
    }

    override suspend fun startPreview(surface: Surface): CameraResult<Unit> = mutex.withLock {
        if (!canStartPreview()) {
            return CameraResult.error(getCannotStartPreviewError())
        }

        try {
            currentSurface = surface
            isPreviewing = true

            // Initialize render engine if available
            renderEngine?.initialize(
                com.jiangdg.ausbc.core.domain.model.RenderConfig(
                    renderMode = currentRequest?.renderMode ?: com.jiangdg.ausbc.core.domain.model.CameraRequest.RenderMode.OPENGL,
                    rotateType = currentRequest?.let { req ->
                        when (req.previewWidth) {
                            req.previewHeight -> com.jiangdg.ausbc.core.domain.model.RotateType.ANGLE_90
                            else -> com.jiangdg.ausbc.core.domain.model.RotateType.ANGLE_0
                        }
                    } ?: com.jiangdg.ausbc.core.domain.model.RotateType.ANGLE_0,
                    previewWidth = currentRequest?.previewWidth ?: 640,
                    previewHeight = currentRequest?.previewHeight ?: 480
                )
            )

            _cameraState.value = CameraState.Previewing(
                previewWidth = currentRequest?.previewWidth ?: 640,
                previewHeight = currentRequest?.previewHeight ?: 480,
                fps = 30.0f
            )

            Logger.i(TAG, "Preview started: ${device.deviceName}")
            CameraResult.success(Unit)
        } catch (e: Exception) {
            isPreviewing = false
            CameraResult.error(
                CameraError.RenderError(
                    reason = "Failed to start preview",
                    cause = e
                )
            )
        }
    }

    override suspend fun startPreview(surfaceTexture: SurfaceTexture): CameraResult<Unit> = mutex.withLock {
        if (!canStartPreview()) {
            return CameraResult.error(getCannotStartPreviewError())
        }

        try {
            currentSurface = surfaceTexture
            isPreviewing = true

            _cameraState.value = CameraState.Previewing(
                previewWidth = currentRequest?.previewWidth ?: 640,
                previewHeight = currentRequest?.previewHeight ?: 480,
                fps = 30.0f
            )

            Logger.i(TAG, "Preview started with SurfaceTexture: ${device.deviceName}")
            CameraResult.success(Unit)
        } catch (e: Exception) {
            isPreviewing = false
            CameraResult.error(
                CameraError.RenderError(
                    reason = "Failed to start preview",
                    cause = e
                )
            )
        }
    }

    override suspend fun stopPreview(): CameraResult<Unit> = mutex.withLock {
        if (!isPreviewing) {
            return CameraResult.success(Unit)
        }

        try {
            stopPreviewInternal()
            Logger.i(TAG, "Preview stopped: ${device.deviceName}")
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.RenderError(
                    reason = "Failed to stop preview",
                    cause = e
                )
            )
        }
    }

    private suspend fun stopPreviewInternal() {
        renderEngine?.stopRendering()
        uvcCamera?.stopPreview()
        currentSurface = null
        isPreviewing = false

        if (_cameraState.value is CameraState.Previewing) {
            _cameraState.value = CameraState.Opened(
                previewWidth = currentRequest?.previewWidth ?: 640,
                previewHeight = currentRequest?.previewHeight ?: 480
            )
        }
    }

    override suspend fun captureImage(request: CaptureRequest): CaptureResult {
        if (!isOpened()) {
            return CaptureResult.CaptureFailed(CameraError.Closed)
        }

        // TODO: Implement actual image capture
        return CaptureResult.ImageCaptureStarted(request.savePath)
    }

    override suspend fun startVideoRecording(request: VideoRecordRequest): CaptureResult {
        if (!isOpened()) {
            return CaptureResult.CaptureFailed(CameraError.Closed)
        }

        _cameraState.value = CameraState.Recording(
            filePath = request.savePath,
            durationMs = 0
        )

        // TODO: Implement actual video recording
        return CaptureResult.VideoRecordingStarted(request.savePath)
    }

    override suspend fun stopVideoRecording(): CaptureResult {
        if (_cameraState.value !is CameraState.Recording) {
            return CaptureResult.CaptureFailed(CameraError.Busy)
        }

        val currentState = _cameraState.value as CameraState.Recording

        _cameraState.value = CameraState.Opened(
            previewWidth = currentRequest?.previewWidth ?: 640,
            previewHeight = currentRequest?.previewHeight ?: 480
        )

        // TODO: Implement actual video recording stop
        return CaptureResult.VideoRecordingComplete(
            filePath = currentState.filePath,
            durationMs = currentState.durationMs,
            size = 0
        )
    }

    override suspend fun startAudioRecording(request: AudioRecordRequest): CaptureResult {
        // TODO: Implement audio recording
        return CaptureResult.AudioRecordingStarted(request.savePath)
    }

    override suspend fun stopAudioRecording(): CaptureResult {
        // TODO: Implement audio recording stop
        return CaptureResult.CaptureFailed(CameraError.Unknown("Audio recording not implemented yet"))
    }

    override fun getPreviewSizes(aspectRatio: Double?): List<PreviewSize> {
        // TODO: Get actual supported sizes from device
        return if (aspectRatio != null) {
            PreviewSize.getSizesForAspectRatio(aspectRatio)
        } else {
            PreviewSize.getCommonSizes()
        }
    }

    override fun getCapabilities(): CameraCapabilities {
        // TODO: Get actual capabilities from device
        return CameraCapabilities.default()
    }

    override fun isOpened(): Boolean {
        return _cameraState.value is CameraState.Opened ||
               _cameraState.value is CameraState.Previewing ||
               _cameraState.value is CameraState.Recording
    }

    override suspend fun updateResolution(width: Int, height: Int): CameraResult<Unit> {
        if (!isOpened()) {
            return CameraResult.error(CameraError.Closed)
        }

        // TODO: Implement actual resolution update
        return CameraResult.success(Unit)
    }

    override fun addPreviewCallback(callback: PreviewCallback) {
        previewCallbacks.add(callback)
    }

    override fun removePreviewCallback(callback: PreviewCallback) {
        previewCallbacks.remove(callback)
    }

    private fun canStartPreview(): Boolean {
        return _cameraState.value is CameraState.Opened && !isPreviewing
    }

    private fun getCannotStartPreviewError(): CameraError {
        return when (_cameraState.value) {
            is CameraState.Idle -> CameraError.Closed
            is CameraState.Opening -> CameraError.Busy
            is CameraState.Previewing -> CameraError.Busy
            is CameraState.Recording -> CameraError.Busy
            is CameraState.Error -> {
                (_cameraState.value as CameraState.Error).error
            }
            else -> CameraError.Busy
        }
    }

    /**
     * Get the USB device
     */
    fun getUsbDevice(): UsbDevice = device

    /**
     * Get the current camera request
     */
    fun getCurrentRequest(): CameraRequest? = currentRequest

    companion object {
        private const val TAG = "UvcCamera"
    }
}
