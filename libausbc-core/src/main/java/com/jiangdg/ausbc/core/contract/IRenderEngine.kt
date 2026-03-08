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
package com.jiangdg.ausbc.core.contract

import android.graphics.SurfaceTexture
import android.view.Surface
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.domain.model.RenderConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Base render effect interface
 *
 * Minimal contract for render effects that can be added to a render engine.
 * Implementations should provide a unique identifier and lifecycle methods.
 */
interface RenderEffect {
    /**
     * Unique effect identifier (e.g., effect type name or ID)
     */
    val effectId: String

    /**
     * Initialize the effect
     */
    fun initialize()

    /**
     * Release effect resources
     */
    fun release()

    /**
     * Check if effect is initialized
     */
    fun isInitialized(): Boolean
}

/**
 * Render engine interface for camera preview rendering
 *
 * This abstraction allows different rendering strategies:
 * - OpenGL rendering (with effects support)
 * - Direct surface rendering (no effects)
 *
 * @author Created for restructuring plan
 */
interface IRenderEngine {

    /**
     * Current render state
     */
    val renderState: StateFlow<RenderState>

    /**
     * Initialize the render engine with configuration
     *
     * @param config Render configuration
     * @return CameraResult indicating success or failure
     */
    suspend fun initialize(config: RenderConfig): CameraResult<Unit>

    /**
     * Start rendering to a Surface
     *
     * @param surface Target surface for rendering
     * @return CameraResult indicating success or failure
     */
    suspend fun startRendering(surface: Surface): CameraResult<Unit>

    /**
     * Start rendering to a SurfaceTexture
     *
     * @param surfaceTexture Target surface texture for rendering
     * @return CameraResult indicating success or failure
     */
    suspend fun startRendering(surfaceTexture: SurfaceTexture): CameraResult<Unit>

    /**
     * Stop rendering
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun stopRendering(): CameraResult<Unit>

    /**
     * Add a render effect
     *
     * @param effect Effect to apply to rendered frames
     * @return CameraResult with effect ID on success
     */
    suspend fun addEffect(effect: RenderEffect): CameraResult<String>

    /**
     * Remove a render effect
     *
     * @param effectId ID of effect to remove
     * @return CameraResult indicating success or failure
     */
    suspend fun removeEffect(effectId: String): CameraResult<Unit>

    /**
     * Update an existing render effect
     *
     * @param effectId ID of effect to update
     * @param effect New effect configuration
     * @return CameraResult indicating success or failure
     */
    suspend fun updateEffect(effectId: String, effect: RenderEffect): CameraResult<Unit>

    /**
     * Get the current EGL context (for OpenGL engines)
     *
     * @return EGL context or null if not using OpenGL
     */
    fun getRenderContext(): android.opengl.EGLContext?

    /**
     * Get current render surface size
     *
     * @return Pair of width and height, or null if not initialized
     */
    fun getSurfaceSize(): Pair<Int, Int>?

    /**
     * Release all render resources
     */
    suspend fun release()

    /**
     * Check if render engine is initialized
     */
    fun isInitialized(): Boolean

    /**
     * Check if render engine is currently rendering
     */
    fun isRendering(): Boolean
}

/**
 * Render state sealed class
 */
sealed class RenderState {
    data object Idle : RenderState()
    data object Initializing : RenderState()
    data object Ready : RenderState()
    data class Rendering(val surfaceWidth: Int, val surfaceHeight: Int) : RenderState()
    data class Error(val error: String) : RenderState()
}
