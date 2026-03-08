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

import android.content.Context
import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.camera.platform.UsbDeviceManager
import com.jiangdg.ausbc.core.domain.repository.IDeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device repository implementation
 *
 * Manages USB device detection, permissions, and connection monitoring.
 *
 * @author Created for restructuring plan
 */
@Singleton
class DeviceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbDeviceManager: UsbDeviceManager
) : IDeviceRepository {

    private val _connectedDevices = MutableStateFlow<List<UsbDevice>>(emptyList())
    private val connectionObservers = mutableSetOf<com.jiangdg.ausbc.core.domain.repository.DeviceConnectionObserver>()

    init {
        // Start monitoring USB devices
        usbDeviceManager.startMonitoring(
            onDeviceAttached = { device ->
                if (isSupportedCamera(device)) {
                    addDevice(device)
                    notifyDeviceAttached(device)
                }
            },
            onDeviceDetached = { device ->
                removeDevice(device)
                notifyDeviceDetached(device)
            }
        )
    }

    override fun getConnectedDevices(): Flow<List<UsbDevice>> {
        return _connectedDevices.asStateFlow()
    }

    override suspend fun requestPermission(device: UsbDevice): Boolean {
        return usbDeviceManager.requestPermission(device) { granted ->
            if (granted) {
                notifyPermissionGranted(device)
            } else {
                notifyPermissionDenied(device)
            }
        }
    }

    override fun hasPermission(device: UsbDevice): Boolean {
        return usbDeviceManager.hasPermission(device)
    }

    override fun isSupportedCamera(device: UsbDevice): Boolean {
        return usbDeviceManager.isSupportedCamera(device)
    }

    override fun getDeviceName(device: UsbDevice): String? {
        return usbDeviceManager.getDeviceName(device)
    }

    override fun getDeviceId(device: UsbDevice): String {
        return "${device.vendorId}:${device.productId}:${device.deviceId}"
    }

    override fun registerDeviceObserver(observer: com.jiangdg.ausbc.core.domain.repository.DeviceConnectionObserver) {
        connectionObservers.add(observer)
    }

    override fun unregisterDeviceObserver(observer: com.jiangdg.ausbc.core.domain.repository.DeviceConnectionObserver) {
        connectionObservers.remove(observer)
    }

    /**
     * Get devices by vendor and product ID
     */
    fun getDevices(vendorId: Int, productId: Int): Flow<List<UsbDevice>> {
        return getConnectedDevices().map { devices ->
            devices.filter { it.vendorId == vendorId && it.productId == productId }
        }
    }

    /**
     * Get first connected camera device
     */
    fun getFirstCamera(): UsbDevice? {
        return _connectedDevices.value.firstOrNull { isSupportedCamera(it) }
    }

    private fun addDevice(device: UsbDevice) {
        val current = _connectedDevices.value.toMutableList()
        if (!current.any { it.deviceId == device.deviceId }) {
            current.add(device)
            _connectedDevices.value = current
        }
    }

    private fun removeDevice(device: UsbDevice) {
        val current = _connectedDevices.value.toMutableList()
        current.removeAll { it.deviceId == device.deviceId }
        _connectedDevices.value = current
    }

    private fun notifyDeviceAttached(device: UsbDevice) {
        connectionObservers.forEach { it.onDeviceAttached(device) }
    }

    private fun notifyDeviceDetached(device: UsbDevice) {
        connectionObservers.forEach { it.onDeviceDetached(device) }
    }

    private fun notifyPermissionGranted(device: UsbDevice) {
        connectionObservers.forEach { it.onPermissionGranted(device) }
    }

    private fun notifyPermissionDenied(device: UsbDevice) {
        connectionObservers.forEach { it.onPermissionDenied(device) }
    }

    /**
     * Clean up resources
     */
    fun release() {
        usbDeviceManager.stopMonitoring()
        connectionObservers.clear()
    }
}
