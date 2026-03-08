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
import kotlinx.coroutines.flow.Flow

/**
 * Device repository interface
 *
 * Provides abstraction for USB device management.
 *
 * @author Created for restructuring plan
 */
interface IDeviceRepository {

    /**
     * Get a flow of connected USB devices
     *
     * @return Flow emitting the current list of connected devices
     */
    fun getConnectedDevices(): Flow<List<UsbDevice>>

    /**
     * Request permission for a USB device
     *
     * @param device USB device to request permission for
     * @return True if permission was granted
     */
    suspend fun requestPermission(device: UsbDevice): Boolean

    /**
     * Check if permission is granted for a device
     *
     * @param device USB device
     * @return True if permission is granted
     */
    fun hasPermission(device: UsbDevice): Boolean

    /**
     * Check if a device is a supported camera
     *
     * @param device USB device
     * @return True if device is a supported camera
     */
    fun isSupportedCamera(device: UsbDevice): Boolean

    /**
     * Get device name/description
     *
     * @param device USB device
     * @return Device name or null
     */
    fun getDeviceName(device: UsbDevice): String?

    /**
     * Get device identifier
     *
     * @param device USB device
     * @return Unique device identifier
     */
    fun getDeviceId(device: UsbDevice): String

    /**
     * Register a device connection observer
     *
     * @param observer Observer to receive device connection events
     */
    fun registerDeviceObserver(observer: DeviceConnectionObserver)

    /**
     * Unregister a device connection observer
     *
     * @param observer Observer to remove
     */
    fun unregisterDeviceObserver(observer: DeviceConnectionObserver)
}

/**
 * Device connection observer interface
 *
 * Receives callbacks when devices are connected or disconnected.
 */
interface DeviceConnectionObserver {
    /**
     * Called when a device is attached
     */
    fun onDeviceAttached(device: UsbDevice)

    /**
     * Called when a device is detached
     */
    fun onDeviceDetached(device: UsbDevice)

    /**
     * Called when device permission is granted
     */
    fun onPermissionGranted(device: UsbDevice)

    /**
     * Called when device permission is denied
     */
    fun onPermissionDenied(device: UsbDevice)
}
