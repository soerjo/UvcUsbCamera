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
package com.jiangdg.ausbc.camera.data.repository

import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.camera.uvc.UvcCameraFactory
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.domain.model.CameraCapabilities
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import com.jiangdg.ausbc.core.domain.model.PreviewSize
import com.jiangdg.ausbc.core.domain.repository.ICameraRepository
import com.jiangdg.ausbc.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera repository implementation
 *
 * Manages camera lifecycle, USB device communication, and state.
 *
 * @author Created for restructuring plan
 */
@Singleton
class CameraRepository @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val uvcCameraFactory: UvcCameraFactory
) : ICameraRepository {

    private val mutex = Mutex()
    private var currentCamera: com.jiangdg.ausbc.core.contract.ICamera? = null

    override suspend fun openCamera(
        device: UsbDevice,
        request: CameraRequest
    ): CameraResult<Unit> = mutex.withLock {
        // Check if camera is already open
        if (currentCamera?.isOpened() == true) {
            Logger.w(TAG, "Camera already opened")
            return CameraResult.error(CameraError.Busy)
        }

        // Check permission
        if (!deviceRepository.hasPermission(device)) {
            Logger.w(TAG, "Permission not granted for device: ${device.deviceName}")
            return CameraResult.error(
                CameraError.PermissionDenied(
                    permission = "USB_DEVICE",
                    cause = SecurityException("USB permission not granted")
                )
            )
        }

        // Create camera instance
        when (val result = uvcCameraFactory.createCamera(device)) {
            is CameraResult.Success -> {
                currentCamera = result.data
                Logger.i(TAG, "Camera created successfully: ${device.deviceName}")

                // Open camera with request
                return when (val openResult = currentCamera?.open(request)) {
                    is CameraResult.Success -> {
                        Logger.i(TAG, "Camera opened successfully: ${device.deviceName}")
                        CameraResult.success(Unit)
                    }
                    is CameraResult.Error -> openResult
                    null -> CameraResult.error(CameraError.Closed)
                }
            }
            is CameraResult.Error -> {
                Logger.e(TAG, "Failed to create camera: ${result.error.message}")
                result
            }
        }
    }

    override suspend fun closeCamera(): CameraResult<Unit> = mutex.withLock {
        val camera = currentCamera
            ?: return CameraResult.error(CameraError.Closed)

        return when (val result = camera.close()) {
            is CameraResult.Success -> {
                currentCamera = null
                Logger.i(TAG, "Camera closed successfully")
                CameraResult.success(Unit)
            }
            is CameraResult.Error -> result
        }
    }

    override fun getConnectedDevices(): Flow<List<UsbDevice>> {
        return deviceRepository.getConnectedDevices()
    }

    override suspend fun requestPermission(device: UsbDevice): CameraResult<Boolean> {
        return if (deviceRepository.requestPermission(device)) {
            CameraResult.success(true)
        } else {
            CameraResult.error(
                CameraError.PermissionDenied(permission = "USB_DEVICE")
            )
        }
    }

    override fun hasPermission(device: UsbDevice): Boolean {
        return deviceRepository.hasPermission(device)
    }

    override suspend fun getCapabilities(device: UsbDevice): CameraResult<CameraCapabilities> {
        // For now, return default capabilities
        // TODO: Get actual capabilities from device
        return CameraResult.success(CameraCapabilities.default())
    }

    override suspend fun getPreviewSizes(
        device: UsbDevice,
        aspectRatio: Double?
    ): CameraResult<List<PreviewSize>> {
        // For now, return common sizes
        // TODO: Get actual supported sizes from device
        return CameraResult.success(
            if (aspectRatio != null) {
                PreviewSize.getSizesForAspectRatio(aspectRatio)
            } else {
                PreviewSize.getCommonSizes()
            }
        )
    }

    override suspend fun switchCamera(device: UsbDevice): CameraResult<Unit> {
        // Close current camera
        closeCamera()

        // Open new camera
        return openCamera(device, CameraRequest.default())
    }

    /**
     * Get current camera instance
     */
    fun getCurrentCamera(): com.jiangdg.ausbc.core.contract.ICamera? = currentCamera

    /**
     * Get current camera state
     */
    fun getCameraState(): Flow<com.jiangdg.ausbc.core.domain.model.CameraState> {
        return currentCamera?.cameraState
            ?: MutableStateFlow(com.jiangdg.ausbc.core.domain.model.CameraState.Idle)
    }

    /**
     * Check if camera is currently opened
     */
    fun isCameraOpened(): Boolean {
        return currentCamera?.isOpened() == true
    }

    companion object {
        private const val TAG = "CameraRepository"
    }
}
