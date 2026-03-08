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

/**
 * Camera request configuration
 *
 * Use the Builder to create camera request instances.
 *
 * @author Created for restructuring plan
 */
data class CameraRequest(
    val previewWidth: Int = 640,
    val previewHeight: Int = 480,
    val renderMode: RenderMode = RenderMode.OPENGL,
    val rotateType: RotateType = RotateType.ANGLE_0,
    val audioSource: AudioSource = AudioSource.SOURCE_SYS_MIC,
    val previewFormat: PreviewFormat = PreviewFormat.FORMAT_MJPEG,
    val aspectRatioShow: Boolean = true,
    val captureRawImage: Boolean = false,
    val rawPreviewData: Boolean = false
) {
    /**
     * Render mode enum
     */
    enum class RenderMode {
        /** GPU rendering with effects support */
        OPENGL,
        /** Direct surface rendering without effects */
        MEDIAN
    }

    /**
     * Preview format enum
     */
    enum class PreviewFormat {
        /** Motion JPEG format */
        FORMAT_MJPEG,
        /** YUYV format */
        FORMAT_YUYV,
        /** NV21 format */
        FORMAT_NV21
    }

    /**
     * Audio source enum
     */
    enum class AudioSource {
        /** System microphone */
        SOURCE_SYS_MIC,
        /** Auto-detect (prefers UAC if available) */
        SOURCE_AUTO,
        /** USB Audio Class microphone */
        SOURCE_UAC,
        /** No audio */
        SOURCE_NONE
    }

    /**
     * Builder for CameraRequest
     */
    class Builder {
        private var previewWidth: Int = 640
        private var previewHeight: Int = 480
        private var renderMode: RenderMode = RenderMode.OPENGL
        private var rotateType: RotateType = RotateType.ANGLE_0
        private var audioSource: AudioSource = AudioSource.SOURCE_SYS_MIC
        private var previewFormat: PreviewFormat = PreviewFormat.FORMAT_MJPEG
        private var aspectRatioShow: Boolean = true
        private var captureRawImage: Boolean = false
        private var rawPreviewData: Boolean = false

        fun setPreviewWidth(width: Int) = apply { previewWidth = width }
        fun setPreviewHeight(height: Int) = apply { previewHeight = height }
        fun setRenderMode(mode: RenderMode) = apply { renderMode = mode }
        fun setDefaultRotateType(type: RotateType) = apply { rotateType = type }
        fun setAudioSource(source: AudioSource) = apply { audioSource = source }
        fun setPreviewFormat(format: PreviewFormat) = apply { previewFormat = format }
        fun setAspectRatioShow(show: Boolean) = apply { aspectRatioShow = show }
        fun setCaptureRawImage(capture: Boolean) = apply { captureRawImage = capture }
        fun setRawPreviewData(raw: Boolean) = apply { rawPreviewData = raw }

        fun create(): CameraRequest = CameraRequest(
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            renderMode = renderMode,
            rotateType = rotateType,
            audioSource = audioSource,
            previewFormat = previewFormat,
            aspectRatioShow = aspectRatioShow,
            captureRawImage = captureRawImage,
            rawPreviewData = rawPreviewData
        )
    }

    /**
     * Check if the request is valid
     */
    fun isValid(): Boolean {
        return previewWidth > 0 && previewHeight > 0
    }

    /**
     * Get the aspect ratio
     */
    fun getAspectRatio(): Double {
        return previewWidth.toDouble() / previewHeight.toDouble()
    }

    companion object {
        /**
         * Create a default camera request
         */
        fun default(): CameraRequest = Builder().create()

        /**
         * Create a HD request (1280x720)
         */
        fun hd(): CameraRequest = Builder()
            .setPreviewWidth(1280)
            .setPreviewHeight(720)
            .create()

        /**
         * Create a Full HD request (1920x1080)
         */
        fun fullHd(): CameraRequest = Builder()
            .setPreviewWidth(1920)
            .setPreviewHeight(1080)
            .create()
    }
}

/**
 * Rotate type enum
 */
enum class RotateType(val angle: Int) {
    ANGLE_0(0),
    ANGLE_90(90),
    ANGLE_180(180),
    ANGLE_270(270);

    companion object {
        fun fromAngle(angle: Int): RotateType {
            return values().firstOrNull { it.angle == angle } ?: ANGLE_0
        }
    }
}
