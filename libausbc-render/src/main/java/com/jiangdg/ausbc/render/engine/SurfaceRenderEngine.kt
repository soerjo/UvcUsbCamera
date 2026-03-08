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
package com.jiangdg.ausbc.render.engine

import android.graphics.SurfaceTexture
import android.view.Surface
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.contract.IRenderEngine
import com.jiangdg.ausbc.core.contract.RenderEffect
import com.jiangdg.ausbc.core.contract.RenderState
import com.jiangdg.ausbc.core.domain.model.RenderConfig
import com.jiangdg.ausbc.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Surface Render Engine implementation
 *
 * Provides direct surface rendering without OpenGL.
 * This is a simpler rendering approach that doesn't support effects.
 *
 * @author Created for restructuring plan
 */
@Singleton
class SurfaceRenderEngine @Inject constructor() : IRenderEngine {

    private var renderConfig: RenderConfig? = null
    private var isInitializedFlag = false
    private var isRenderingFlag = false
    private var currentSurface: Any? = null

    private val _renderState: MutableStateFlow<RenderState> = MutableStateFlow(RenderState.Idle)
    override val renderState: StateFlow<RenderState> = _renderState

    override suspend fun initialize(config: RenderConfig): CameraResult<Unit> {
        if (isInitializedFlag) {
            return CameraResult.Error(com.jiangdg.ausbc.core.common.error.CameraError.Busy)
        }

        return try {
            renderConfig = config
            isInitializedFlag = true
            _renderState.value = RenderState.Ready
            Logger.i("SurfaceRenderEngine", "Render engine initialized (direct mode)")
            CameraResult.Success(Unit)
        } catch (e: Exception) {
            _renderState.value = RenderState.Error(e.message ?: "Unknown error")
            CameraResult.Error(
                com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                    reason = "Failed to initialize render engine",
                    cause = e
                )
            )
        }
    }

    override suspend fun startRendering(surface: Surface): CameraResult<Unit> {
        if (!isInitializedFlag) {
            return CameraResult.Error(com.jiangdg.ausbc.core.common.error.CameraError.RenderError("Engine not initialized"))
        }

        if (isRenderingFlag) {
            return CameraResult.Error(com.jiangdg.ausbc.core.common.error.CameraError.Busy)
        }

        return try {
            currentSurface = surface
            isRenderingFlag = true

            val size = getSurfaceSize()
            if (size != null) {
                _renderState.value = RenderState.Rendering(size.first, size.second)
            }

            Logger.i("SurfaceRenderEngine", "Rendering started on Surface")
            CameraResult.Success(Unit)
        } catch (e: Exception) {
            isRenderingFlag = false
            _renderState.value = RenderState.Error(e.message ?: "Unknown error")
            CameraResult.Error(
                com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                    reason = "Failed to start rendering",
                    cause = e
                )
            )
        }
    }

    override suspend fun startRendering(surfaceTexture: SurfaceTexture): CameraResult<Unit> {
        if (!isInitializedFlag) {
            return CameraResult.Error(com.jiangdg.ausbc.core.common.error.CameraError.RenderError("Engine not initialized"))
        }

        if (isRenderingFlag) {
            return CameraResult.Error(com.jiangdg.ausbc.core.common.error.CameraError.Busy)
        }

        return try {
            currentSurface = surfaceTexture
            isRenderingFlag = true

            val size = getSurfaceSize()
            if (size != null) {
                _renderState.value = RenderState.Rendering(size.first, size.second)
            }

            Logger.i("SurfaceRenderEngine", "Rendering started on SurfaceTexture")
            CameraResult.Success(Unit)
        } catch (e: Exception) {
            isRenderingFlag = false
            _renderState.value = RenderState.Error(e.message ?: "Unknown error")
            CameraResult.Error(
                com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                    reason = "Failed to start rendering",
                    cause = e
                )
            )
        }
    }

    override suspend fun stopRendering(): CameraResult<Unit> {
        if (!isRenderingFlag) {
            return CameraResult.Success(Unit)
        }

        return try {
            isRenderingFlag = false
            currentSurface = null
            _renderState.value = RenderState.Ready

            Logger.i("SurfaceRenderEngine", "Rendering stopped")
            CameraResult.Success(Unit)
        } catch (e: Exception) {
            _renderState.value = RenderState.Error(e.message ?: "Unknown error")
            CameraResult.Error(
                com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                    reason = "Failed to stop rendering",
                    cause = e
                )
            )
        }
    }

    override suspend fun addEffect(effect: RenderEffect): CameraResult<String> {
        // Surface rendering doesn't support effects
        return CameraResult.Error(
            com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                reason = "Effects not supported in surface rendering mode. Use OpenGL rendering mode instead."
            )
        )
    }

    override suspend fun removeEffect(effectId: String): CameraResult<Unit> {
        // Surface rendering doesn't support effects
        return CameraResult.Error(
            com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                reason = "Effects not supported in surface rendering mode. Use OpenGL rendering mode instead."
            )
        )
    }

    override suspend fun updateEffect(effectId: String, effect: RenderEffect): CameraResult<Unit> {
        // Surface rendering doesn't support effects
        return CameraResult.Error(
            com.jiangdg.ausbc.core.common.error.CameraError.RenderError(
                reason = "Effects not supported in surface rendering mode. Use OpenGL rendering mode instead."
            )
        )
    }

    override fun getRenderContext(): android.opengl.EGLContext? {
        // Surface rendering doesn't have an EGL context
        return null
    }

    override fun getSurfaceSize(): Pair<Int, Int>? {
        return when (val surface = currentSurface) {
            is Surface -> {
                if (surface.isValid) {
                    // Use the render config size since Surface doesn't expose dimensions directly
                    val width = renderConfig?.previewWidth ?: 640
                    val height = renderConfig?.previewHeight ?: 480
                    Pair(width, height)
                } else {
                    null
                }
            }
            is SurfaceTexture -> {
                surface.setDefaultBufferSize(640, 480)
                Pair(640, 480)
            }
            else -> null
        }
    }

    /**
     * Release all resources
     */
    override suspend fun release() {
        stopRendering()
        isInitializedFlag = false
        _renderState.value = RenderState.Idle
    }

    override fun isInitialized(): Boolean = isInitializedFlag

    override fun isRendering(): Boolean = isRenderingFlag

    /**
     * Get current render config
     */
    fun getRenderConfig(): RenderConfig? = renderConfig

    companion object {
        private const val TAG = "SurfaceRenderEngine"
    }
}
