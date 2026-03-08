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
package com.jiangdg.ausbc.core.domain.model

import com.jiangdg.ausbc.core.common.error.CameraError

/**
 * Sealed class representing camera state
 *
 * This class provides a type-safe way to represent the current state of a camera.
 * Use with StateFlow for reactive state management.
 *
 * @author Created for restructuring plan
 */
sealed class CameraState {
    /**
     * Camera is idle (not opened)
     */
    data object Idle : CameraState()

    /**
     * Camera is opening
     */
    data object Opening : CameraState()

    /**
     * Camera is opened and ready
     */
    data class Opened(
        val previewWidth: Int,
        val previewHeight: Int
    ) : CameraState()

    /**
     * Camera preview is running
     */
    data class Previewing(
        val previewWidth: Int,
        val previewHeight: Int,
        val fps: Float
    ) : CameraState()

    /**
     * Camera is capturing
     */
    data class Capturing(
        val captureType: CaptureType
    ) : CameraState()

    /**
     * Camera is recording
     */
    data class Recording(
        val filePath: String,
        val durationMs: Long
    ) : CameraState()

    /**
     * Camera is closing
     */
    data object Closing : CameraState()

    /**
     * Camera is closed
     */
    data object Closed : CameraState()

    /**
     * Camera encountered an error
     */
    data class Error(val error: CameraError) : CameraState()

    /**
     * Check if camera is in a usable state
     */
    fun isUsable(): Boolean {
        return this is Opened || this is Previewing
    }

    /**
     * Check if camera is busy
     */
    fun isBusy(): Boolean {
        return this is Opening || this is Closing || this is Capturing || this is Recording
    }

    /**
     * Check if camera is in error state
     */
    fun isError(): Boolean {
        return this is Error
    }

    /**
     * Capture type enum
     */
    enum class CaptureType {
        IMAGE,
        VIDEO,
        AUDIO
    }
}

/**
 * Camera capabilities
 *
 * Describes the features and supported formats of a camera device.
 */
data class CameraCapabilities(
    val supportedPreviewSizes: List<PreviewSize>,
    val supportedPreviewFormats: List<CameraRequest.PreviewFormat>,
    val supportsH264: Boolean = false,
    val supportsEffects: Boolean = false,
    val supportsPhotoCapture: Boolean = true,
    val supportsVideoCapture: Boolean = true,
    val supportsAudioCapture: Boolean = true,
    val minPreviewWidth: Int = 320,
    val minPreviewHeight: Int = 240,
    val maxPreviewWidth: Int = 1920,
    val maxPreviewHeight: Int = 1080,
    val hasZoom: Boolean = false,
    val hasFocus: Boolean = false,
    val hasAutoFocus: Boolean = false
) {
    /**
     * Get preview sizes for a specific aspect ratio
     */
    fun getPreviewSizesForAspectRatio(aspectRatio: Double, tolerance: Double = 0.01): List<PreviewSize> {
        return supportedPreviewSizes.filter { size ->
            val sizeRatio = size.width.toDouble() / size.height.toDouble()
            kotlin.math.abs(sizeRatio - aspectRatio) < tolerance
        }
    }

    /**
     * Check if a specific size is supported
     */
    fun isSizeSupported(width: Int, height: Int): Boolean {
        return supportedPreviewSizes.any { it.width == width && it.height == height }
    }

    /**
     * Get the closest supported size to the requested dimensions
     */
    fun getClosestSize(width: Int, height: Int): PreviewSize? {
        val targetAspect = width.toDouble() / height.toDouble()
        val sameAspectRatioSizes = getPreviewSizesForAspectRatio(targetAspect)

        if (sameAspectRatioSizes.isNotEmpty()) {
            // Find the closest size by area
            return sameAspectRatioSizes.minByOrNull { size ->
                kotlin.math.abs(size.width * size.height - width * height)
            }
        }

        // If no exact aspect ratio match, find closest overall
        return supportedPreviewSizes.minByOrNull { size ->
            val aspectDiff = kotlin.math.abs(
                (size.width.toDouble() / size.height.toDouble()) - targetAspect
            )
            val sizeDiff = kotlin.math.abs(size.width * size.height - width * height)
            aspectDiff * 1000 + sizeDiff
        }
    }

    companion object {
        /**
         * Create default capabilities
         */
        fun default(): CameraCapabilities = CameraCapabilities(
            supportedPreviewSizes = listOf(
                PreviewSize(640, 480),
                PreviewSize(1280, 720),
                PreviewSize(1920, 1080)
            ),
            supportedPreviewFormats = listOf(
                CameraRequest.PreviewFormat.FORMAT_MJPEG,
                CameraRequest.PreviewFormat.FORMAT_YUYV
            )
        )
    }
}
