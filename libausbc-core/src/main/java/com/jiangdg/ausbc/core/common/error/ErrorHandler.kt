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

import android.content.Context

/**
 * Centralized error handler for camera operations
 *
 * Provides methods to convert errors to user-friendly messages
 * and handle specific error scenarios.
 *
 * @author Created for restructuring plan
 */
object ErrorHandler {

    /**
     * Get a user-friendly error message for a camera error
     *
     * @param error The camera error
     * @param context Android context for string resources
     * @return User-friendly error message
     */
    fun getUserMessage(error: CameraError, context: Context? = null): String {
        return when (error) {
            is CameraError.PermissionDenied -> {
                "Camera permission is required. Please grant permission in settings."
            }
            is CameraError.DeviceNotFound -> {
                "Camera device not found. Please check the connection."
            }
            is CameraError.OpenFailed -> {
                "Failed to open camera: ${(error as CameraError.OpenFailed).reason}"
            }
            is CameraError.UnsupportedSize -> {
                val sizeError = error as CameraError.UnsupportedSize
                "Size ${sizeError.width}x${sizeError.height} is not supported."
            }
            is CameraError.EncodeError -> {
                "Encoding error: ${(error as CameraError.EncodeError).reason}"
            }
            is CameraError.RenderError -> {
                "Rendering error: ${(error as CameraError.RenderError).reason}"
            }
            is CameraError.Busy -> {
                "Camera is busy. Please wait and try again."
            }
            is CameraError.Closed -> {
                "Camera has been closed. Please reopen to continue."
            }
            is CameraError.UnsupportedDevice -> {
                val devError = error as CameraError.UnsupportedDevice
                "Camera device '${devError.device.deviceName}' is not supported."
            }
            is CameraError.Timeout -> {
                val timeout = error as CameraError.Timeout
                "Operation '${timeout.operation}' timed out after ${timeout.timeoutMs}ms."
            }
            is CameraError.InvalidConfig -> {
                val configError = error as CameraError.InvalidConfig
                "Invalid configuration: ${configError.parameter} - ${configError.reason}"
            }
            is CameraError.Unknown -> {
                error.message
            }
        }
    }

    /**
     * Check if an error is recoverable (user can retry)
     */
    fun isRecoverable(error: CameraError): Boolean {
        return when (error) {
            is CameraError.Busy, is CameraError.Timeout, is CameraError.OpenFailed -> true
            is CameraError.PermissionDenied, is CameraError.DeviceNotFound, is CameraError.Closed,
            is CameraError.UnsupportedDevice, is CameraError.UnsupportedSize, is CameraError.InvalidConfig -> false
            is CameraError.EncodeError, is CameraError.RenderError -> true
            is CameraError.Unknown -> error.cause != null
        }
    }

    /**
     * Check if user action is required to resolve the error
     */
    fun requiresUserAction(error: CameraError): Boolean {
        return when (error) {
            is CameraError.PermissionDenied, is CameraError.UnsupportedDevice, is CameraError.InvalidConfig -> true
            else -> false
        }
    }

    /**
     * Convert a generic Throwable to CameraError if possible
     */
    fun fromThrowable(throwable: Throwable): CameraError {
        return when (throwable) {
            is CameraError -> throwable
            is SecurityException -> CameraError.PermissionDenied(
                permission = "UNKNOWN",
                cause = throwable
            )
            is IllegalArgumentException -> CameraError.InvalidConfig(
                parameter = "UNKNOWN",
                reason = throwable.message ?: "Invalid argument",
                cause = throwable
            )
            is IllegalStateException -> CameraError.Busy
            else -> CameraError.Unknown(
                message = throwable.message ?: "Unknown error",
                cause = throwable
            )
        }
    }
}
