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
package com.jiangdg.ausbc.camera.datasource

import android.hardware.usb.UsbDevice
import com.jiangdg.usb.USBMonitor
import kotlinx.coroutines.flow.Flow

/**
 * USB Device data source interface
 *
 * Provides abstraction for USB device operations.
 *
 * @author Created for restructuring plan
 */
interface IUsbDeviceDataSource {

    /**
     * Get USB control block for a device
     *
     * @param device USB device
     * @return USB control block or null
     */
    fun getUsbControlBlock(device: UsbDevice): USBMonitor.UsbControlBlock?

    /**
     * Check if device has permission
     *
     * @param device USB device
     * @return True if permission granted
     */
    fun hasPermission(device: UsbDevice): Boolean

    /**
     * Request permission for a device
     *
     * @param device USB device
     * @param callback Permission result callback
     */
    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit): Boolean

    /**
     * Check if device is a supported camera
     *
     * @param device USB device
     * @return True if supported
     */
    fun isSupportedCamera(device: UsbDevice): Boolean

    /**
     * Get device list
     *
     * @return Flow emitting device list changes
     */
    fun getDeviceList(): Flow<List<UsbDevice>>

    /**
     * Register device listener
     *
     * @param listener Device connection listener
     */
    fun registerDeviceListener(listener: USBMonitor.OnDeviceConnectListener)

    /**
     * Unregister device listener
     *
     * @param listener Device connection listener
     */
    fun unregisterDeviceListener(listener: USBMonitor.OnDeviceConnectListener)
}
