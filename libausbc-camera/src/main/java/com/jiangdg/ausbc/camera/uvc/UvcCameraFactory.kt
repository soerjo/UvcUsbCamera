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
import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.contract.ICamera
import com.jiangdg.ausbc.core.contract.IRenderEngine
import com.jiangdg.ausbc.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider

/**
 * Factory for creating UVC camera instances
 *
 * This factory creates ICamera implementations for UVC devices.
 *
 * @author Created for restructuring plan
 */
class UvcCameraFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val renderEngineProvider: Provider<IRenderEngine?>
) {

    /**
     * Create a UVC camera instance
     *
     * @param device USB device
     * @return CameraResult with the camera instance or error
     */
    fun createCamera(device: UsbDevice): CameraResult<ICamera> {
        if (!isSupportedUvcDevice(device)) {
            Logger.w(TAG, "Device is not a supported UVC camera: ${device.deviceName}")
            return CameraResult.error(
                CameraError.UnsupportedDevice(device)
            )
        }

        return try {
            val camera = UvcCamera(
                context = context,
                device = device,
                renderEngine = renderEngineProvider.get()
            )
            CameraResult.success(camera)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create UVC camera", e)
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Failed to create camera instance",
                    cause = e
                )
            )
        }
    }

    /**
     * Check if a device is a supported UVC camera
     *
     * @param device USB device
     * @return True if device is a supported UVC camera
     */
    fun isSupportedUvcDevice(device: UsbDevice): Boolean {
        // Check for UVC class codes
        val hasVideoClass = (0 until device.interfaceCount).any { i ->
    val usbInterface = device.getInterface(i)
    usbInterface.interfaceClass == 0x0E // Video class
}

        return hasVideoClass
    }

    companion object {
        private const val TAG = "UvcCameraFactory"
    }
}
