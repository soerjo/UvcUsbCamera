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

import android.content.Context
import android.hardware.usb.UsbDevice
import com.jiangdg.usb.USBMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB Device data source implementation
 *
 * Provides Flow-based USB device monitoring and wraps USBMonitor.
 *
 * @author Created for restructuring plan
 */
@Singleton
class UsbDeviceDataSource @Inject constructor(
    private val context: Context,
    private val usbMonitorWrapper: UsbMonitorWrapper
) : IUsbDeviceDataSource {

    private val listeners = mutableSetOf<USBMonitor.OnDeviceConnectListener>()
    private val _deviceList = MutableStateFlow<List<UsbDevice>>(emptyList())

    private val deviceListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            device ?: return
            if (isSupportedCamera(device)) {
                addDevice(device)
            }
        }

        override fun onDetach(device: UsbDevice?) {
            device ?: return
            removeDevice(device)
        }

        override fun onConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {
            // Notify listeners
            listeners.forEach { it.onConnect(device, ctrlBlock, createNew) }
        }

        override fun onDisconnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?
        ) {
            // Notify listeners
            listeners.forEach { it.onDisconnect(device, ctrlBlock) }
        }

        override fun onCancel(device: UsbDevice?) {
            // Notify listeners
            listeners.forEach { it.onCancel(device) }
        }
    }

    init {
        usbMonitorWrapper.registerListener(deviceListener)
        usbMonitorWrapper.startMonitoring()
    }

    override fun getUsbControlBlock(device: UsbDevice): USBMonitor.UsbControlBlock? {
        return usbMonitorWrapper.getUsbControlBlock(device)
    }

    override fun hasPermission(device: UsbDevice): Boolean {
        return usbMonitorWrapper.hasPermission(device)
    }

    override fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit): Boolean {
        return usbMonitorWrapper.requestPermission(device, callback)
    }

    override fun isSupportedCamera(device: UsbDevice): Boolean {
        // Check for UVC class codes
        val hasVideoClass = (0 until device.interfaceCount).any { i ->
            val usbInterface = device.getInterface(i)
            usbInterface.interfaceClass == 0x0E // Video class
        }

        if (hasVideoClass) return true

        // Check for known UVC devices
        return isKnownUvcDevice(device.vendorId, device.productId)
    }

    override fun getDeviceList(): Flow<List<UsbDevice>> {
        return _deviceList.asStateFlow()
    }

    override fun registerDeviceListener(listener: USBMonitor.OnDeviceConnectListener) {
        listeners.add(listener)
    }

    override fun unregisterDeviceListener(listener: USBMonitor.OnDeviceConnectListener) {
        listeners.remove(listener)
    }

    private fun addDevice(device: UsbDevice) {
        val current = _deviceList.value.toMutableList()
        if (!current.any { it.deviceId == device.deviceId }) {
            current.add(device)
            _deviceList.value = current
        }
    }

    private fun removeDevice(device: UsbDevice) {
        val current = _deviceList.value.toMutableList()
        current.removeAll { it.deviceId == device.deviceId }
        _deviceList.value = current
    }

    private fun isKnownUvcDevice(vendorId: Int, productId: Int): Boolean {
        // Add known UVC devices here
        return false
    }

    /**
     * Clean up resources
     */
    fun release() {
        usbMonitorWrapper.unregisterListener(deviceListener)
        usbMonitorWrapper.stopMonitoring()
        listeners.clear()
    }
}

/**
 * Wrapper for USBMonitor to enable dependency injection
 */
class UsbMonitorWrapper @Inject constructor(
    private val context: Context
) {
    private var usbMonitor: USBMonitor? = null
    private val controlBlocks = mutableMapOf<String, USBMonitor.UsbControlBlock>()

    private val listener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            device ?: return
        }

        override fun onDetach(device: UsbDevice?) {
            device ?: return
            controlBlocks.remove("${device.vendorId}:${device.productId}:${device.deviceId}")
        }

        override fun onConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {
            device ?: return
            ctrlBlock ?: return
            val key = "${device.vendorId}:${device.productId}:${device.deviceId}"
            controlBlocks[key] = ctrlBlock
            listeners.forEach { it.onConnect(device, ctrlBlock, createNew) }
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            device ?: return
            val key = "${device.vendorId}:${device.productId}:${device.deviceId}"
            controlBlocks.remove(key)
            listeners.forEach { it.onDisconnect(device, ctrlBlock) }
        }

        override fun onCancel(device: UsbDevice?) {
            device ?: return
            listeners.forEach { it.onCancel(device) }
        }
    }

    private val listeners = mutableSetOf<USBMonitor.OnDeviceConnectListener>()

    fun registerListener(listener: USBMonitor.OnDeviceConnectListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: USBMonitor.OnDeviceConnectListener) {
        listeners.remove(listener)
    }

    fun startMonitoring() {
        if (usbMonitor == null) {
            usbMonitor = USBMonitor(context, listener)
        }
        usbMonitor?.register()
    }

    fun stopMonitoring() {
        usbMonitor?.unregister()
    }

    fun getUsbControlBlock(device: UsbDevice): USBMonitor.UsbControlBlock? {
        val key = "${device.vendorId}:${device.productId}:${device.deviceId}"
        return controlBlocks[key]
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbMonitor?.hasPermission(device) ?: false
    }

    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit): Boolean {
        return usbMonitor?.requestPermission(device) ?: false
    }

    fun getMonitor(): USBMonitor? = usbMonitor

    fun destroy() {
        usbMonitor?.destroy()
        usbMonitor = null
        listeners.clear()
        controlBlocks.clear()
    }
}
