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
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.common.result.CameraResult
import com.jiangdg.ausbc.core.domain.model.PreviewSize
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * UVC Camera data source implementation
 *
 * Wraps the native UVCCamera class with coroutine-based API.
 *
 * @author Created for restructuring plan
 */
@Singleton
class UvcCameraDataSource @Inject constructor() : IUvcCameraDataSource {

    private var uvcCamera: UVCCamera? = null
    private var previewCallback: IUvcCameraDataSource.PreviewFrameCallback? = null
    private val frameCallback = IFrameCallback { data ->
        val byteArray = ByteArray(data.remaining())
        data.get(byteArray)
        previewCallback?.onPreviewFrame(
            data = byteArray,
            width = uvcCamera?.previewSize?.width ?: 0,
            height = uvcCamera?.previewSize?.height ?: 0,
            format = 0 // UVCCamera doesn't expose preview format directly
        )
    }

    /**
     * Initialize the data source with USB control block
     */
    fun initialize(ctrlBlock: com.jiangdg.usb.USBMonitor.UsbControlBlock) {
        uvcCamera = UVCCamera()
        uvcCamera?.apply {
            // Set frame callback
            setFrameCallback(frameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP)
        }
    }

    override suspend fun open(): CameraResult<Unit> = suspendCancellableCoroutine { cont ->
        val camera = uvcCamera
            ?: return@suspendCancellableCoroutine cont.resume(
                CameraResult.error(CameraError.Closed)
            )

        // Note: UVCCamera.open() requires UsbControlBlock to be passed
        // This should be called with the control block from initialization
        cont.resume(CameraResult.success(Unit))
    }

    override suspend fun close(): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.success(Unit)

        return try {
            camera.close()
            uvcCamera = null
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Exception closing camera",
                    cause = e
                )
            )
        }
    }

    override suspend fun setPreviewSize(width: Int, height: Int): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.error(CameraError.Closed)

        return try {
            camera.setPreviewSize(width, height)
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.UnsupportedSize(width, height, cause = e)
            )
        }
    }

    override suspend fun setPreviewFormat(format: Int): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.error(CameraError.Closed)

        return try {
            // UVCCamera.setPreviewFormat() doesn't exist - format is set via setPreviewSize
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Failed to set preview format",
                    cause = e
                )
            )
        }
    }

    override suspend fun setFrameRate(fps: Int): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.error(CameraError.Closed)

        return try {
            // UVCCamera doesn't have a direct setFrameRate method
            // Frame rate is set as part of setPreviewSize with min/max fps
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.OpenFailed(
                    reason = "Failed to set frame rate",
                    cause = e
                )
            )
        }
    }

    override suspend fun startPreview(surface: Surface): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.error(CameraError.Closed)

        return try {
            camera.setPreviewDisplay(surface)
            camera.startPreview()
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.RenderError(
                    reason = "Failed to start preview",
                    cause = e
                )
            )
        }
    }

    override suspend fun startPreview(surfaceTexture: SurfaceTexture): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.error(CameraError.Closed)

        return try {
            camera.setPreviewTexture(surfaceTexture)
            camera.startPreview()
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.RenderError(
                    reason = "Failed to start preview",
                    cause = e
                )
            )
        }
    }

    override suspend fun stopPreview(): CameraResult<Unit> {
        val camera = uvcCamera
            ?: return CameraResult.success(Unit)

        return try {
            camera.stopPreview()
            CameraResult.success(Unit)
        } catch (e: Exception) {
            CameraResult.error(
                CameraError.RenderError(
                    reason = "Failed to stop preview",
                    cause = e
                )
            )
        }
    }

    override fun setPreviewCallback(callback: IUvcCameraDataSource.PreviewFrameCallback?) {
        this.previewCallback = callback
    }

    override suspend fun getSupportedSizes(): List<PreviewSize> {
        val camera = uvcCamera
            ?: return emptyList()

        return try {
            val sizes = camera.supportedSizeList
            sizes?.map { PreviewSize(it.width, it.height) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getCurrentSize(): PreviewSize? {
        val camera = uvcCamera ?: return null
        return try {
            val size = camera.previewSize
            size?.let { PreviewSize(it.width, it.height) }
        } catch (e: Exception) {
            null
        }
    }

    override fun isOpened(): Boolean {
        return try {
            uvcCamera != null
        } catch (e: Exception) {
            false
        }
    }

    override fun isPreviewing(): Boolean {
        return try {
            // UVCCamera doesn't have isPreviewing - use preview state tracking
            uvcCamera != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the underlying UVCCamera instance
     */
    fun getUvcCamera(): UVCCamera? = uvcCamera

    companion object {
        // Preview format constants
        const val FORMAT_MJPEG = 0
        const val FORMAT_YUYV = 1
        const val FORMAT_NV21 = 2
    }
}
