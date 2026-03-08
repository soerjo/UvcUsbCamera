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
 * Render configuration
 *
 * Configuration for render engine initialization.
 *
 * @author Created for restructuring plan
 */
data class RenderConfig(
    val renderMode: CameraRequest.RenderMode = CameraRequest.RenderMode.OPENGL,
    val rotateType: RotateType = RotateType.ANGLE_0,
    val previewWidth: Int = 640,
    val previewHeight: Int = 480,
    val enableEffects: Boolean = true,
    val sharedContext: android.opengl.EGLContext? = null
) {
    /**
     * Check if the configuration is valid
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

    /**
     * Builder for RenderConfig
     */
    class Builder {
        private var renderMode: CameraRequest.RenderMode = CameraRequest.RenderMode.OPENGL
        private var rotateType: RotateType = RotateType.ANGLE_0
        private var previewWidth: Int = 640
        private var previewHeight: Int = 480
        private var enableEffects: Boolean = true
        private var sharedContext: android.opengl.EGLContext? = null

        fun setRenderMode(mode: CameraRequest.RenderMode) = apply { renderMode = mode }
        fun setRotateType(type: RotateType) = apply { rotateType = type }
        fun setPreviewWidth(width: Int) = apply { previewWidth = width }
        fun setPreviewHeight(height: Int) = apply { previewHeight = height }
        fun setEnableEffects(enable: Boolean) = apply { enableEffects = enable }
        fun setSharedContext(context: android.opengl.EGLContext?) = apply { sharedContext = context }

        fun build(): RenderConfig = RenderConfig(
            renderMode = renderMode,
            rotateType = rotateType,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            enableEffects = enableEffects,
            sharedContext = sharedContext
        )
    }
}
