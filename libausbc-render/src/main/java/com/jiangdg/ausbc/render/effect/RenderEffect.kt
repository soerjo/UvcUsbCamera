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
package com.jiangdg.ausbc.render.effect

import com.jiangdg.ausbc.core.contract.RenderEffect as CoreRenderEffect

/**
 * Render effect interface
 *
 * Base interface for all OpenGL render effects.
 * Extends the core RenderEffect interface with additional
 * render-specific functionality.
 *
 * @author Created for restructuring plan
 */
interface RenderEffect : CoreRenderEffect {
    /**
     * Effect type identifier
     */
    val effectType: EffectType

    /**
     * Effect name
     */
    val effectName: String
        get() = effectType.name

    /**
     * Effect ID (maps to core interface)
     */
    override val effectId: String
        get() = effectType.name

    /**
     * Initialize the effect (returns Boolean for render-specific usage)
     */
    fun initializeEffect(): Boolean

    /**
     * Apply the effect
     */
    fun apply()

    /**
     * Check if effect is enabled
     */
    fun isEnabledEffect(): Boolean

    /**
     * Enable or disable the effect
     */
    fun setEffectEnabled(enabled: Boolean)

    /**
     * Set effect parameter
     */
    fun <T : Any> setParameter(name: String, value: T)

    /**
     * Get effect parameter
     */
    fun <T : Any> getParameter(name: String): T?

    /**
     * Get all parameter names
     */
    fun getParameterNames(): Set<String>

    // Bridge methods to core interface
    override fun initialize() {
        initializeEffect()
    }
}

/**
 * Effect type enum
 */
enum class EffectType {
    NONE,
    BLACK_WHITE,
    SEPIA,
    NEGATIVE,
    BLUR,
    SHARPEN,
    ZOOM,
    SOUL,
    VIGNETTE,
    COLOR_BALANCE,
    BRIGHTNESS,
    CONTRAST,
    SATURATION
}

/**
 * Abstract base class for render effects
 *
 * Provides common functionality for effect implementations.
 */
abstract class AbstractEffect : RenderEffect {
    protected var enabled = true
    protected var initialized = false
    protected val parameters = mutableMapOf<String, Any>()

    override fun isEnabledEffect(): Boolean = enabled

    override fun setEffectEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getParameter(name: String): T? {
        return parameters[name] as? T
    }

    override fun <T : Any> setParameter(name: String, value: T) {
        parameters[name] = value
        onParameterChanged(name, value)
    }

    override fun getParameterNames(): Set<String> = parameters.keys

    /**
     * Called when a parameter is changed
     */
    protected open fun onParameterChanged(name: String, value: Any) {
        // Override in subclasses if needed
    }

    override fun release() {
        initialized = false
        parameters.clear()
    }

    override fun isInitialized(): Boolean = initialized
}

/**
 * Black and white effect
 *
 * Converts the rendered image to grayscale.
 */
class BlackWhiteEffect : AbstractEffect() {

    override val effectType = EffectType.BLACK_WHITE

    private var shaderProgram = 0
    private var intensity = 1.0f

    init {
        parameters["intensity"] = intensity
    }

    override fun initializeEffect(): Boolean {
        // Create shader program for grayscale conversion
        // This would involve compiling and linking shaders
        initialized = true
        return true
    }

    override fun apply() {
        if (!initialized || !enabled) return

        // Apply grayscale effect using shader
        // This is a placeholder - actual implementation would use OpenGL
    }

    override fun release() {
        if (shaderProgram != 0) {
            // Delete shader program
            shaderProgram = 0
        }
        super.release()
    }

    override fun <T : Any> setParameter(name: String, value: T) {
        when (name) {
            "intensity" -> {
                intensity = (value as? Float) ?: 1.0f
                parameters[name] = intensity
            }
            else -> super.setParameter(name, value)
        }
    }
}

/**
 * Sepia effect
 *
 * Applies a sepia tone to the rendered image.
 */
class SepiaEffect : AbstractEffect() {

    override val effectType = EffectType.SEPIA

    private var intensity = 0.8f

    init {
        parameters["intensity"] = intensity
    }

    override fun initializeEffect(): Boolean {
        // Create shader program for sepia tone
        initialized = true
        return true
    }

    override fun apply() {
        if (!initialized || !enabled) return

        // Apply sepia effect
    }

    override fun <T : Any> setParameter(name: String, value: T) {
        when (name) {
            "intensity" -> {
                intensity = (value as? Float) ?: 0.8f
                parameters[name] = intensity
            }
            else -> super.setParameter(name, value)
        }
    }
}

/**
 * Blur effect
 *
 * Applies a Gaussian blur to the rendered image.
 */
class BlurEffect : AbstractEffect() {

    override val effectType = EffectType.BLUR

    private var radius = 5.0f

    init {
        parameters["radius"] = radius
    }

    override fun initializeEffect(): Boolean {
        // Create shader program for blur
        initialized = true
        return true
    }

    override fun apply() {
        if (!initialized || !enabled) return

        // Apply blur effect
    }

    override fun <T : Any> setParameter(name: String, value: T) {
        when (name) {
            "radius" -> {
                radius = (value as? Float) ?: 5.0f
                parameters[name] = radius
            }
            else -> super.setParameter(name, value)
        }
    }
}

/**
 * Zoom effect
 *
 * Applies a zoom effect to the rendered image.
 */
class ZoomEffect : AbstractEffect() {

    override val effectType = EffectType.ZOOM

    private var zoomLevel = 1.0f
    private var x = 0.5f
    private var y = 0.5f

    init {
        parameters["zoomLevel"] = zoomLevel
        parameters["x"] = x
        parameters["y"] = y
    }

    override fun initializeEffect(): Boolean {
        // Create shader program for zoom
        initialized = true
        return true
    }

    override fun apply() {
        if (!initialized || !enabled) return

        // Apply zoom effect
    }

    override fun <T : Any> setParameter(name: String, value: T) {
        when (name) {
            "zoomLevel" -> {
                zoomLevel = (value as? Float) ?: 1.0f
                parameters[name] = zoomLevel
            }
            "x" -> {
                x = (value as? Float) ?: 0.5f
                parameters[name] = x
            }
            "y" -> {
                y = (value as? Float) ?: 0.5f
                parameters[name] = y
            }
            else -> super.setParameter(name, value)
        }
    }

    /**
     * Set zoom level
     */
    fun setZoom(level: Float) {
        zoomLevel = level.coerceIn(1.0f, 10.0f)
        setParameter("zoomLevel", zoomLevel)
    }

    /**
     * Set zoom center
     */
    fun setZoomCenter(x: Float, y: Float) {
        this.x = x.coerceIn(0.0f, 1.0f)
        this.y = y.coerceIn(0.0f, 1.0f)
        setParameter("x", this.x)
        setParameter("y", this.y)
    }
}

/**
 * Effect factory
 *
 * Creates effect instances by type.
 */
object EffectFactory {

    /**
     * Create an effect by type
     */
    fun createEffect(effectType: EffectType): RenderEffect? {
        return when (effectType) {
            EffectType.BLACK_WHITE -> BlackWhiteEffect()
            EffectType.SEPIA -> SepiaEffect()
            EffectType.BLUR -> BlurEffect()
            EffectType.ZOOM -> ZoomEffect()
            else -> null
        }
    }

    /**
     * Get all available effect types
     */
    fun getAvailableEffectTypes(): List<EffectType> {
        return listOf(
            EffectType.BLACK_WHITE,
            EffectType.SEPIA,
            EffectType.BLUR,
            EffectType.ZOOM
        )
    }
}
