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
package com.jiangdg.ausbc.camera.platform

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.usb.USBMonitor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB device manager
 *
 * Handles USB device detection, permissions, and connection monitoring.
 * Wraps the legacy USBMonitor for backward compatibility.
 *
 * @author Created for restructuring plan
 */
@Singleton
class UsbDeviceManager @Inject constructor(
    private val context: Context
) {
    private var usbMonitor: USBMonitor? = null
    private val permissionCallbacks = mutableMapOf<String, (Boolean) -> Unit>()
    private val usbControlBlocks = mutableMapOf<String, USBMonitor.UsbControlBlock>()

    /**
     * Start monitoring USB devices
     */
    fun startMonitoring(
        onDeviceAttached: (UsbDevice) -> Unit,
        onDeviceDetached: (UsbDevice) -> Unit
    ) {
        if (usbMonitor != null) return

        usbMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                device ?: return
                if (isSupportedCamera(device)) {
                    Logger.i(TAG, "Device attached: ${device.deviceName}")
                    onDeviceAttached(device)
                }
            }

            override fun onDetach(device: UsbDevice?) {
                device ?: return
                Logger.i(TAG, "Device detached: ${device.deviceName}")
                usbControlBlocks.remove(getDeviceKey(device))
                onDeviceDetached(device)
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                device ?: return
                ctrlBlock ?: return
                Logger.i(TAG, "Device connected: ${device.deviceName}")

                // Store the control block
                usbControlBlocks[getDeviceKey(device)] = ctrlBlock
                permissionCallbacks[getDeviceKey(device)]?.invoke(true)
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                device ?: return
                Logger.i(TAG, "Device disconnected: ${device.deviceName}")
                usbControlBlocks.remove(getDeviceKey(device))
            }

            override fun onCancel(device: UsbDevice?) {
                device ?: return
                Logger.w(TAG, "Device connection cancelled: ${device.deviceName}")
                permissionCallbacks[getDeviceKey(device)]?.invoke(false)
            }
        })

        usbMonitor?.register()
    }

    /**
     * Stop monitoring USB devices
     */
    fun stopMonitoring() {
        usbMonitor?.unregister()
        usbMonitor?.destroy()
        usbMonitor = null
        permissionCallbacks.clear()
        usbControlBlocks.clear()
    }

    /**
     * Request permission for a USB device
     */
    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit): Boolean {
        val monitor = usbMonitor ?: return false

        val key = getDeviceKey(device)
        permissionCallbacks[key] = callback

        return monitor.requestPermission(device)
    }

    /**
     * Check if permission is granted for a device
     */
    fun hasPermission(device: UsbDevice): Boolean {
        val monitor = usbMonitor ?: return false
        return monitor.hasPermission(device)
    }

    /**
     * Check if a device is a supported UVC camera
     */
    fun isSupportedCamera(device: UsbDevice): Boolean {
        // Check for UVC class codes
        val hasVideoClass = (0 until device.interfaceCount).any { i ->
            val usbInterface = device.getInterface(i)
            usbInterface.interfaceClass == 0x0E // Video class
        }

        if (hasVideoClass) return true

        // Check for known UVC devices
        // Add known vendor/product IDs here if needed
        return false
    }

    /**
     * Get device name
     */
    fun getDeviceName(device: UsbDevice): String? {
        return device.deviceName
    }

    /**
     * Get USB control block for a device
     */
    fun getUsbControlBlock(device: UsbDevice): USBMonitor.UsbControlBlock? {
        if (!hasPermission(device)) return null
        return usbControlBlocks[getDeviceKey(device)]
    }

    /**
     * Get the USB monitor instance
     */
    fun getUsbMonitor(): USBMonitor? = usbMonitor

    private fun getDeviceKey(device: UsbDevice): String {
        return "${device.vendorId}:${device.productId}:${device.deviceId}"
    }

    companion object {
        private const val TAG = "UsbDeviceManager"
    }
}
