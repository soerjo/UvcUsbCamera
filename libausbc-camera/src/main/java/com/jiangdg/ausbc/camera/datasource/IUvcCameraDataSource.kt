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
package com.jiangdg.ausbc.camera.datasource

import android.graphics.SurfaceTexture
import android.view.Surface
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.domain.model.PreviewSize
import kotlinx.coroutines.flow.Flow

/**
 * UVC Camera data source interface
 *
 * Provides abstraction for native UVC camera operations.
 * This interface wraps the native UVCCamera class.
 *
 * @author Created for restructuring plan
 */
interface IUvcCameraDataSource {

    /**
     * Open the UVC camera
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun open(): CameraResult<Unit>

    /**
     * Close the UVC camera
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun close(): CameraResult<Unit>

    /**
     * Set preview size
     *
     * @param width Preview width
     * @param height Preview height
     * @return CameraResult indicating success or failure
     */
    suspend fun setPreviewSize(width: Int, height: Int): CameraResult<Unit>

    /**
     * Set preview format
     *
     * @param format Preview format (MJPEG, YUYV, etc.)
     * @return CameraResult indicating success or failure
     */
    suspend fun setPreviewFormat(format: Int): CameraResult<Unit>

    /**
     * Set frame rate
     *
     * @param fps Frames per second
     * @return CameraResult indicating success or failure
     */
    suspend fun setFrameRate(fps: Int): CameraResult<Unit>

    /**
     * Start preview to Surface
     *
     * @param surface Target surface
     * @return CameraResult indicating success or failure
     */
    suspend fun startPreview(surface: Surface): CameraResult<Unit>

    /**
     * Start preview to SurfaceTexture
     *
     * @param surfaceTexture Target surface texture
     * @return CameraResult indicating success or failure
     */
    suspend fun startPreview(surfaceTexture: SurfaceTexture): CameraResult<Unit>

    /**
     * Stop preview
     *
     * @return CameraResult indicating success or failure
     */
    suspend fun stopPreview(): CameraResult<Unit>

    /**
     * Set preview callback
     *
     * @param callback Preview frame callback
     */
    fun setPreviewCallback(callback: PreviewFrameCallback?)

    /**
     * Get supported preview sizes
     *
     * @return List of supported preview sizes
     */
    suspend fun getSupportedSizes(): List<PreviewSize>

    /**
     * Get current preview size
     *
     * @return Current preview size or null
     */
    fun getCurrentSize(): PreviewSize?

    /**
     * Check if camera is opened
     */
    fun isOpened(): Boolean

    /**
     * Check if preview is running
     */
    fun isPreviewing(): Boolean

    /**
     * Preview frame callback interface
     */
    interface PreviewFrameCallback {
        fun onPreviewFrame(data: ByteArray, width: Int, height: Int, format: Int)
    }
}
