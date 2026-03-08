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
package com.jiangdg.ausbc.render.manager

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.Surface
import com.jiangdg.ausbc.utils.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Render Context Manager
 *
 * Manages EGL context, display, and surfaces for OpenGL rendering.
 * Handles EGL lifecycle and context sharing.
 *
 * @author Created for restructuring plan
 */
@Singleton
class RenderContextManager @Inject constructor() {

    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglConfig: EGLConfig? = null
    private var eglSurface: EGLSurface? = null

    private var isInitialized = false
    private var surfaceCreated = false

    // EGL configuration attributes
    private val configAttribs = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_DEPTH_SIZE, 16,
        EGL14.EGL_STENCIL_SIZE, 8,
        EGL14.EGL_SAMPLE_BUFFERS, 0,
        EGL14.EGL_SAMPLES, 0,
        EGL14.EGL_NONE
    )

    // EGL context attributes
    private val contextAttribs = intArrayOf(
        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL14.EGL_NONE
    )

    // Surface attributes
    private val surfaceAttribs = intArrayOf(
        EGL14.EGL_NONE
    )

    /**
     * Initialize EGL context
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Logger.w("RenderContextManager", "Already initialized")
            return true
        }

        return try {
            // Get EGL display
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
                throw RuntimeException("Unable to get EGL14 display")
            }

            // Initialize EGL
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw RuntimeException("Unable to initialize EGL14")
            }

            // Choose EGL config
            val numConfig = IntArray(1)
            val configs = arrayOfNulls<EGLConfig>(1)
            if (!EGL14.eglChooseConfig(
                    eglDisplay,
                    configAttribs,
                    0,
                    configs,
                    0,
                    1,
                    numConfig,
                    0
                ) || numConfig[0] == 0
            ) {
                throw RuntimeException("Unable to find EGL config")
            }
            eglConfig = configs[0]

            // Create EGL context
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                contextAttribs,
                0
            )

            if (eglContext === EGL14.EGL_NO_CONTEXT) {
                throw RuntimeException("Failed to create EGL context")
            }

            isInitialized = true
            Logger.i("RenderContextManager", "EGL context initialized successfully")
            true
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Failed to initialize EGL context", e)
            release()
            false
        }
    }

    /**
     * Create EGL surface from Android Surface
     */
    fun createSurface(surface: Surface): Boolean {
        if (!isInitialized) {
            Logger.w("RenderContextManager", "Not initialized")
            return false
        }

        return try {
            // Release existing surface
            if (surfaceCreated) {
                releaseSurface()
            }

            // Create window surface
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,
                eglConfig,
                surface,
                surfaceAttribs,
                0
            )

            if (eglSurface === EGL14.EGL_NO_SURFACE) {
                throw RuntimeException("Failed to create EGL surface")
            }

            // Make context and surface current
            if (!EGL14.eglMakeCurrent(
                    eglDisplay,
                    eglSurface,
                    eglSurface,
                    eglContext
                )
            ) {
                throw RuntimeException("Failed to make EGL context current")
            }

            surfaceCreated = true
            Logger.i("RenderContextManager", "EGL surface created")
            true
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Failed to create surface", e)
            releaseSurface()
            false
        }
    }

    /**
     * Create EGL surface from SurfaceTexture
     */
    fun createSurface(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
        if (!isInitialized) {
            Logger.w("RenderContextManager", "Not initialized")
            return false
        }

        return try {
            // Release existing surface
            if (surfaceCreated) {
                releaseSurface()
            }

            // Create window surface from SurfaceTexture
            val surface = Surface(surfaceTexture)
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,
                eglConfig,
                surface,
                surfaceAttribs,
                0
            )

            if (eglSurface === EGL14.EGL_NO_SURFACE) {
                throw RuntimeException("Failed to create EGL surface from SurfaceTexture")
            }

            // Make context and surface current
            if (!EGL14.eglMakeCurrent(
                    eglDisplay,
                    eglSurface,
                    eglSurface,
                    eglContext
                )
            ) {
                throw RuntimeException("Failed to make EGL context current")
            }

            surfaceCreated = true
            Logger.i("RenderContextManager", "EGL surface created from SurfaceTexture")
            true
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Failed to create surface", e)
            releaseSurface()
            false
        }
    }

    /**
     * Release EGL surface
     */
    fun releaseSurface() {
        if (!surfaceCreated) {
            return
        }

        try {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )

            eglSurface?.let {
                EGL14.eglDestroySurface(eglDisplay, it)
            }

            eglSurface = null
            surfaceCreated = false

            Logger.i("RenderContextManager", "EGL surface released")
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Error releasing surface", e)
        }
    }

    /**
     * Release all EGL resources
     */
    fun release() {
        releaseSurface()

        try {
            eglContext?.let {
                EGL14.eglDestroyContext(eglDisplay, it)
            }
            eglContext = null

            eglDisplay?.let {
                EGL14.eglTerminate(it)
            }
            eglDisplay = null

            eglConfig = null
            isInitialized = false

            Logger.i("RenderContextManager", "EGL resources released")
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Error releasing EGL resources", e)
        }
    }

    /**
     * Swap buffers (double buffering)
     */
    fun swapBuffers(): Boolean {
        if (!surfaceCreated) {
            return false
        }

        return try {
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Error swapping buffers", e)
            false
        }
    }

    /**
     * Get EGL context
     */
    fun getEGLContext(): EGLContext? = eglContext

    /**
     * Get EGL display
     */
    fun getEGLDisplay(): EGLDisplay? = eglDisplay

    /**
     * Check if initialized
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Check if surface is created
     */
    fun isSurfaceCreated(): Boolean = surfaceCreated

    /**
     * Set swap interval (0 = immediate, 1 = vsync)
     */
    fun setSwapInterval(interval: Int): Boolean {
        if (!surfaceCreated) {
            return false
        }

        return try {
            EGL14.eglSwapInterval(eglDisplay, interval)
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Error setting swap interval", e)
            false
        }
    }

    /**
     * Query surface width and height
     */
    fun getSurfaceSize(): Pair<Int, Int>? {
        if (!surfaceCreated) {
            return null
        }

        return try {
            val width = IntArray(1)
            val height = IntArray(1)

            if (EGL14.eglQuerySurface(
                    eglDisplay,
                    eglSurface,
                    EGL14.EGL_WIDTH,
                    width,
                    0
                ) &&
                EGL14.eglQuerySurface(
                    eglDisplay,
                    eglSurface,
                    EGL14.EGL_HEIGHT,
                    height,
                    0
                )
            ) {
                Pair(width[0], height[0])
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e("RenderContextManager", "Error querying surface size", e)
            null
        }
    }

    companion object {
        private const val TAG = "RenderContextManager"
    }
}
