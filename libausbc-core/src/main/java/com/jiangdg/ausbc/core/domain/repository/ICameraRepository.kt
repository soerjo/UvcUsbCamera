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
package com.jiangdg.ausbc.core.domain.repository

import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import kotlinx.coroutines.flow.Flow

/**
 * Camera repository interface
 *
 * Provides abstraction for camera operations. Implementations handle
 * camera lifecycle, USB communication, and state management.
 *
 * @author Created for restructuring plan
 */
interface ICameraRepository {

    /**
     * Open a camera device
     *
     * @param device USB device to open
     * @param request Camera configuration
     * @return CameraResult indicating success or failure
     */
    suspend fun openCamera(device: UsbDevice, request: CameraRequest): CameraResult<Unit>

    /**
     * Close the current camera
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun closeCamera(): CameraResult<Unit>

    /**
     * Get a list of connected camera devices
     *
     * @return Flow emitting list of connected devices
     */
    fun getConnectedDevices(): Flow<List<UsbDevice>>

    /**
     * Request USB permission for a device
     *
     * @param device USB device
     * @return CameraResult indicating success or failure
     */
    suspend fun requestPermission(device: UsbDevice): CameraResult<Boolean>

    /**
     * Check if permission is granted for a device
     *
     * @param device USB device
     * @return True if permission is granted
     */
    fun hasPermission(device: UsbDevice): Boolean

    /**
     * Get camera capabilities
     *
     * @param device USB device
     * @return CameraResult with capabilities
     */
    suspend fun getCapabilities(device: UsbDevice): CameraResult<com.jiangdg.ausbc.core.domain.model.CameraCapabilities>

    /**
     * Get supported preview sizes
     *
     * @param device USB device
     * @param aspectRatio Optional aspect ratio filter
     * @return CameraResult with list of preview sizes
     */
    suspend fun getPreviewSizes(
        device: UsbDevice,
        aspectRatio: Double?
    ): CameraResult<List<com.jiangdg.ausbc.core.domain.model.PreviewSize>>

    /**
     * Switch to a different camera
     *
     * @param device USB device to switch to
     * @return CameraResult indicating success or failure
     */
    suspend fun switchCamera(device: UsbDevice): CameraResult<Unit>
}
