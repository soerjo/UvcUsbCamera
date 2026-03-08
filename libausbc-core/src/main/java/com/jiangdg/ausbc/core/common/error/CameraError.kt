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
package com.jiangdg.ausbc.core.common.error

import android.hardware.usb.UsbDevice

/**
 * Sealed hierarchy of camera-related errors
 *
 * This class provides a type-safe way to handle all camera-related errors
 * in the application. Each error type carries relevant context information.
 *
 * @author Created for restructuring plan
 */
sealed class CameraError {
    /**
     * Human-readable error message
     */
    abstract val message: String

    /**
     * Underlying cause if applicable
     */
    abstract val cause: Throwable?

    /**
     * Permission was denied by the user
     */
    data class PermissionDenied(
        val permission: String,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Permission denied: $permission"
    }

    /**
     * USB device not found or disconnected
     */
    data class DeviceNotFound(
        val deviceId: Int,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "USB device not found: $deviceId"
    }

    /**
     * Failed to open camera device
     */
    data class OpenFailed(
        val reason: String,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Failed to open camera: $reason"
    }

    /**
     * Requested preview size is not supported
     */
    data class UnsupportedSize(
        val width: Int,
        val height: Int,
        val supportedSizes: List<String> = emptyList(),
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Unsupported size: ${width}x${height}"
    }

    /**
     * Media encoding error
     */
    data class EncodeError(
        val reason: String,
        val encoderType: EncoderType = EncoderType.UNKNOWN,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Encoding error ($encoderType): $reason"
    }

    /**
     * Rendering error
     */
    data class RenderError(
        val reason: String,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Render error: $reason"
    }

    /**
     * Camera is busy with another operation
     */
    data object Busy : CameraError() {
        override val message: String = "Camera is busy"
        override val cause: Throwable? = null
    }

    /**
     * Camera has been closed
     */
    data object Closed : CameraError() {
        override val message: String = "Camera has been closed"
        override val cause: Throwable? = null
    }

    /**
     * Camera device is not supported
     */
    data class UnsupportedDevice(
        val device: UsbDevice,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Unsupported device: ${device.deviceName}"
    }

    /**
     * Timeout occurred during operation
     */
    data class Timeout(
        val operation: String,
        val timeoutMs: Long,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Timeout during '$operation' after ${timeoutMs}ms"
    }

    /**
     * Invalid configuration or parameter
     */
    data class InvalidConfig(
        val parameter: String,
        val reason: String,
        override val cause: Throwable? = null
    ) : CameraError() {
        override val message: String = "Invalid config '$parameter': $reason"
    }

    /**
     * Unknown or unexpected error
     */
    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : CameraError()

    /**
     * Encoder type enumeration
     */
    enum class EncoderType {
        H264,
        AAC,
        MP4,
        UNKNOWN
    }

    /**
     * Convert to Throwable for compatibility with exception handling code
     */
    fun toException(): Exception = CameraException(this)
}

/**
 * Exception wrapper for CameraError
 */
class CameraException(val cameraError: CameraError) : Exception(cameraError.message, cameraError.cause)
