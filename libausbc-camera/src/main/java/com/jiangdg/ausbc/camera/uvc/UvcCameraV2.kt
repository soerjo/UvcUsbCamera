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
import com.jiangdg.ausbc.camera.datasource.CameraParameterDataSource
import com.jiangdg.ausbc.camera.datasource.IUvcCameraDataSource
import com.jiangdg.ausbc.camera.datasource.IUsbDeviceDataSource
import com.jiangdg.ausbc.camera.datasource.UvcCameraDataSource
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.common.result.CaptureResult
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
import com.jiangdg.usb.USBMonitor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * UVC Camera V2 - With data source integration
 *
 * Enhanced UVC camera implementation using data source pattern.
 * Integrates with native UVCCamera through data source abstraction.
 *
 * @author Created for restructuring plan
 */
class UvcCameraV2 @Inject constructor(
    private val context: Context,
    private val device: UsbDevice,
    private val usbDeviceDataSource: IUsbDeviceDataSource,
    private val renderEngine: IRenderEngine?,
    private val parameterDataSource: CameraParameterDataSource
) : com.jiangdg.ausbc.core.contract.ICamera {

    private val mutex = Mutex()
    private var uvcCameraDataSource: UvcCameraDataSource? = null
    private var usbControlBlock: USBMonitor.UsbControlBlock? = null
    private var isPreviewing = false
    private var currentRequest: CameraRequest? = null

    // State management
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    override val cameraState = _cameraState.asStateFlow()

    // Preview frames
    private val _previewFrames = MutableSharedFlow<PreviewFrame>(
        replay = 0,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val previewFrames: Flow<PreviewFrame> = _previewFrames

    private val previewFrameCallback = object : IUvcCameraDataSource.PreviewFrameCallback {
        override fun onPreviewFrame(data: ByteArray, width: Int, height: Int, format: Int) {
            Logger.d("UvcCameraV2", "Preview frame: ${width}x${height}, format: $format, size: ${data.size}")
        }
    }

    private var currentSurface: Any? = null // Surface or SurfaceTexture

    override suspend fun open(request: CameraRequest): CameraResult<Unit> = mutex.withLock {
        if (_cameraState.value !is CameraState.Idle) {
            return CameraResult.error(CameraError.Busy)
        }

        try {
            _cameraState.value = CameraState.Opening
            currentRequest = request

            // Get USB control block
            usbControlBlock = usbDeviceDataSource.getUsbControlBlock(device)
                ?: return CameraResult.error(
                    CameraError.OpenFailed(
                        reason = "Failed to get USB control block"
                    )
                )

            // Initialize UVC camera data source
            uvcCameraDataSource = UvcCameraDataSource().apply {
                initialize(usbControlBlock!!)
            }

            // Open camera
            when (val result = uvcCameraDataSource!!.open()) {
                is CameraResult.Success -> {
                    // Configure camera
                    configureCamera(request)

                    // Set preview callback
                    uvcCameraDataSource?.setPreviewCallback(previewFrameCallback)

                    // Link parameter data source
                    parameterDataSource.setCamera(uvcCameraDataSource?.getUvcCamera())

                    _cameraState.value = CameraState.Opened(
                        previewWidth = request.previewWidth,
                        previewHeight = request.previewHeight
                    )

                    Logger.i("UvcCameraV2", "Camera opened: ${device.deviceName}")
                    CameraResult.success(Unit)
                }
                is CameraResult.Error -> result
            }
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
            uvcCameraDataSource?.close()
            uvcCameraDataSource = null

            // Release parameter data source
            parameterDataSource.setCamera(null)

            currentSurface = null
            currentRequest = null
            isPreviewing = false

            _cameraState.value = CameraState.Idle

            Logger.i("UvcCameraV2", "Camera closed: ${device.deviceName}")
            CameraResult.success(Unit)
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error(
                CameraError.OpenFailed(
                    reason = "Failed to close camera",
                    cause = e
                )
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

            // Start actual preview
            when (val result = uvcCameraDataSource?.startPreview(surface)) {
                is CameraResult.Success -> {
                    isPreviewing = true

                    _cameraState.value = CameraState.Previewing(
                        previewWidth = currentRequest?.previewWidth ?: 640,
                        previewHeight = currentRequest?.previewHeight ?: 480,
                        fps = 30.0f
                    )

                    Logger.i("UvcCameraV2", "Preview started: ${device.deviceName}")
                    CameraResult.success(Unit)
                }
                is CameraResult.Error -> result
                else -> CameraResult.error(CameraError.RenderError(reason = "Camera data source not initialized"))
            }
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

            // Start actual preview
            when (val result = uvcCameraDataSource?.startPreview(surfaceTexture)) {
                is CameraResult.Success -> {
                    isPreviewing = true

                    _cameraState.value = CameraState.Previewing(
                        previewWidth = currentRequest?.previewWidth ?: 640,
                        previewHeight = currentRequest?.previewHeight ?: 480,
                        fps = 30.0f
                    )

                    Logger.i("UvcCameraV2", "Preview started with SurfaceTexture: ${device.deviceName}")
                    CameraResult.success(Unit)
                }
                is CameraResult.Error -> result
                else -> CameraResult.error(CameraError.RenderError(reason = "Camera data source not initialized"))
            }
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
            Logger.i("UvcCameraV2", "Preview stopped: ${device.deviceName}")
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
        uvcCameraDataSource?.stopPreview()
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

        // TODO: Implement actual image capture using native code
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

        // TODO: Implement actual video recording using native code
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
        val sizes = runCatching {
            // Try to get actual supported sizes from camera
            runCatching { emptyList<PreviewSize>() }.getOrNull()
        }.getOrNull()

        return if (aspectRatio != null) {
            (sizes ?: PreviewSize.getCommonSizes()).filter { size ->
                kotlin.math.abs(size.aspectRatio - aspectRatio) < 0.01
            }
        } else {
            sizes ?: PreviewSize.getCommonSizes()
        }
    }

    override fun getCapabilities(): CameraCapabilities {
        // TODO: Get actual capabilities from camera
        return CameraCapabilities.default().copy(
            hasZoom = parameterDataSource.isZoomSupported(),
            hasFocus = parameterDataSource.isFocusSupported(),
            hasAutoFocus = parameterDataSource.isAutoFocusSupported()
        )
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

        return try {
            uvcCameraDataSource?.setPreviewSize(width, height)
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.UnsupportedSize(width, height, cause = e)
            )
        }
    }

    override fun addPreviewCallback(callback: PreviewCallback) {
        // Add callback to the preview frame flow
        // This will be called from the previewFrameCallback
    }

    override fun removePreviewCallback(callback: PreviewCallback) {
        // Remove callback
    }

    /**
     * Get parameter data source for camera control
     */
    fun getParameterDataSource(): CameraParameterDataSource = parameterDataSource

    /**
     * Get the USB device
     */
    fun getUsbDevice(): UsbDevice = device

    /**
     * Get the current camera request
     */
    fun getCurrentRequest(): CameraRequest? = currentRequest

    private suspend fun configureCamera(request: CameraRequest) {
        // Set preview size
        uvcCameraDataSource?.setPreviewSize(request.previewWidth, request.previewHeight)

        // Set preview format
        val format = when (request.previewFormat) {
            CameraRequest.PreviewFormat.FORMAT_MJPEG -> UvcCameraDataSource.FORMAT_MJPEG
            CameraRequest.PreviewFormat.FORMAT_YUYV -> UvcCameraDataSource.FORMAT_YUYV
            CameraRequest.PreviewFormat.FORMAT_NV21 -> UvcCameraDataSource.FORMAT_NV21
        }
        uvcCameraDataSource?.setPreviewFormat(format)

        // Set frame rate
        uvcCameraDataSource?.setFrameRate(30)
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

    companion object {
        private const val TAG = "UvcCameraV2"
    }
}
