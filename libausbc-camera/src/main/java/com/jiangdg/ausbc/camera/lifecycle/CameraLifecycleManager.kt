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
package com.jiangdg.ausbc.camera.lifecycle

import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import com.jiangdg.ausbc.core.domain.model.CameraState
import com.jiangdg.ausbc.core.domain.repository.IDeviceRepository
import com.jiangdg.ausbc.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera lifecycle manager
 *
 * Manages the lifecycle of camera connections, handling device
 * attach/detach events and camera state transitions.
 *
 * @author Created for restructuring plan
 */
@Singleton
class CameraLifecycleManager @Inject constructor(
    private val deviceRepository: IDeviceRepository
) {
    private val managerScope = CoroutineScope(SupervisorJob())
    private var monitoringJob: Job? = null

    private val _lifecycleState = MutableStateFlow<LifecycleState>(LifecycleState.Idle)
    val lifecycleState: Flow<LifecycleState> = _lifecycleState.asStateFlow()

    private var currentDevice: UsbDevice? = null
    private val deviceConnectionObservers = mutableSetOf<DeviceConnectionObserver>()

    init {
        startDeviceMonitoring()
    }

    /**
     * Start monitoring USB devices
     */
    private fun startDeviceMonitoring() {
        monitoringJob = deviceRepository.getConnectedDevices()
            .onEach { devices ->
                handleDeviceListChange(devices)
            }
            .catch { e ->
                Logger.e(TAG, "Error monitoring devices", e)
                _lifecycleState.value = LifecycleState.Error(
                    CameraError.Unknown(
                        message = "Device monitoring error",
                        cause = e
                    )
                )
            }
            .launchIn(managerScope)
    }

    /**
     * Stop monitoring and clean up
     */
    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
        deviceConnectionObservers.clear()
        _lifecycleState.value = LifecycleState.Idle
    }

    /**
     * Open a camera device
     */
    fun openCamera(
        device: UsbDevice,
        request: CameraRequest,
        onResult: (CameraResult<Unit>) -> Unit
    ) {
        managerScope.launch {
            try {
                _lifecycleState.value = LifecycleState.Opening(device)

                // Check permission first
                if (!deviceRepository.hasPermission(device)) {
                    Logger.w(TAG, "Permission not granted for device: ${device.deviceName}")
                    _lifecycleState.value = LifecycleState.Error(
                        CameraError.PermissionDenied("USB_DEVICE")
                    )
                    onResult(CameraResult.error(CameraError.PermissionDenied("USB_DEVICE")))
                    return@launch
                }

                currentDevice = device
                _lifecycleState.value = LifecycleState.Opened(device)

                Logger.i(TAG, "Camera opened: ${device.deviceName}")
                onResult(CameraResult.success(Unit))
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to open camera", e)
                val error = CameraError.OpenFailed(
                    reason = "Failed to open camera",
                    cause = e
                )
                _lifecycleState.value = LifecycleState.Error(error)
                onResult(CameraResult.error(error))
            }
        }
    }

    /**
     * Close the current camera
     */
    fun closeCamera(onResult: (CameraResult<Unit>) -> Unit) {
        managerScope.launch {
            try {
                val device = currentDevice
                    ?: return@launch onResult(CameraResult.error(CameraError.Closed))

                _lifecycleState.value = LifecycleState.Closing(device)

                currentDevice = null
                _lifecycleState.value = LifecycleState.Idle

                Logger.i(TAG, "Camera closed: ${device.deviceName}")
                onResult(CameraResult.success(Unit))
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to close camera", e)
                val error = CameraError.Unknown(
                    message = "Failed to close camera",
                    cause = e
                )
                _lifecycleState.value = LifecycleState.Error(error)
                onResult(CameraResult.error(error))
            }
        }
    }

    /**
     * Get the current device
     */
    fun getCurrentDevice(): UsbDevice? = currentDevice

    /**
     * Check if a device is currently opened
     */
    fun isDeviceOpened(): Boolean = currentDevice != null

    /**
     * Register a device connection observer
     */
    fun registerObserver(observer: DeviceConnectionObserver) {
        deviceConnectionObservers.add(observer)
    }

    /**
     * Unregister a device connection observer
     */
    fun unregisterObserver(observer: DeviceConnectionObserver) {
        deviceConnectionObservers.remove(observer)
    }

    private fun handleDeviceListChange(devices: List<UsbDevice>) {
        // Check if current device was disconnected
        currentDevice?.let { current ->
            if (!devices.any { it.deviceId == current.deviceId }) {
                Logger.w(TAG, "Current device disconnected: ${current.deviceName}")
                handleDeviceDisconnected(current)
            }
        }

        // Notify observers of device list changes
        deviceConnectionObservers.forEach { observer ->
            observer.onDevicesChanged(devices)
        }
    }

    private fun handleDeviceDisconnected(device: UsbDevice) {
        deviceConnectionObservers.forEach { observer ->
            observer.onDeviceDisconnected(device)
        }

        if (currentDevice?.deviceId == device.deviceId) {
            currentDevice = null
            _lifecycleState.value = LifecycleState.Disconnected(device)
        }
    }

    /**
     * Lifecycle state sealed class
     */
    sealed class LifecycleState {
        data object Idle : LifecycleState()
        data class Opening(val device: UsbDevice) : LifecycleState()
        data class Opened(val device: UsbDevice) : LifecycleState()
        data class Closing(val device: UsbDevice) : LifecycleState()
        data class Disconnected(val device: UsbDevice) : LifecycleState()
        data class Error(val error: CameraError) : LifecycleState()
    }

    /**
     * Device connection observer interface
     */
    interface DeviceConnectionObserver {
        fun onDevicesChanged(devices: List<UsbDevice>) {}
        fun onDeviceDisconnected(device: UsbDevice) {}
        fun onDeviceConnected(device: UsbDevice) {}
    }

    companion object {
        private const val TAG = "CameraLifecycleManager"
    }
}
