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

import com.jiangdg.ausbc.core.common.result.CameraResult

/**
 * Camera parameter data source interface
 *
 * Provides methods to control UVC camera parameters like brightness,
 * contrast, saturation, etc.
 *
 * @author Created for restructuring plan
 */
interface ICameraParameterDataSource {

    /**
     * Brightness control
     */
    suspend fun setBrightness(value: Int): CameraResult<Unit>
    suspend fun getBrightness(): CameraResult<Int>
    suspend fun resetBrightness(): CameraResult<Unit>
    fun getBrightnessRange(): IntRange?

    /**
     * Contrast control
     */
    suspend fun setContrast(value: Int): CameraResult<Unit>
    suspend fun getContrast(): CameraResult<Int>
    suspend fun resetContrast(): CameraResult<Unit>
    fun getContrastRange(): IntRange?

    /**
     * Saturation control
     */
    suspend fun setSaturation(value: Int): CameraResult<Unit>
    suspend fun getSaturation(): CameraResult<Int>
    suspend fun resetSaturation(): CameraResult<Unit>
    fun getSaturationRange(): IntRange?

    /**
     * Sharpness control
     */
    suspend fun setSharpness(value: Int): CameraResult<Unit>
    suspend fun getSharpness(): CameraResult<Int>
    suspend fun resetSharpness(): CameraResult<Unit>
    fun getSharpnessRange(): IntRange?

    /**
     * Gain control
     */
    suspend fun setGain(value: Int): CameraResult<Unit>
    suspend fun getGain(): CameraResult<Int>
    suspend fun resetGain(): CameraResult<Unit>
    fun getGainRange(): IntRange?

    /**
     * Gamma control
     */
    suspend fun setGamma(value: Int): CameraResult<Unit>
    suspend fun getGamma(): CameraResult<Int>
    suspend fun resetGamma(): CameraResult<Unit>
    fun getGammaRange(): IntRange?

    /**
     * White balance control
     */
    suspend fun setWhiteBalance(value: Int): CameraResult<Unit>
    suspend fun getWhiteBalance(): CameraResult<Int>
    suspend fun resetWhiteBalance(): CameraResult<Unit>
    fun getWhiteBalanceRange(): IntRange?

    /**
     * Exposure control
     */
    suspend fun setExposure(value: Int): CameraResult<Unit>
    suspend fun getExposure(): CameraResult<Int>
    suspend fun resetExposure(): CameraResult<Unit>
    fun getExposureRange(): IntRange?

    /**
     * Focus control
     */
    suspend fun setFocus(value: Int): CameraResult<Unit>
    suspend fun getFocus(): CameraResult<Int>
    suspend fun resetFocus(): CameraResult<Unit>
    fun getFocusRange(): IntRange?

    /**
     * Zoom control
     */
    suspend fun setZoom(value: Int): CameraResult<Unit>
    suspend fun getZoom(): CameraResult<Int>
    suspend fun resetZoom(): CameraResult<Unit>
    fun getZoomRange(): IntRange?

    /**
     * Auto focus control
     */
    suspend fun setAutoFocus(enabled: Boolean): CameraResult<Unit>
    suspend fun getAutoFocus(): CameraResult<Boolean>

    /**
     * Auto white balance control
     */
    suspend fun setAutoWhiteBalance(enabled: Boolean): CameraResult<Unit>
    suspend fun getAutoWhiteBalance(): CameraResult<Boolean>

    /**
     * Check if parameter is supported
     */
    fun isBrightnessSupported(): Boolean
    fun isContrastSupported(): Boolean
    fun isSaturationSupported(): Boolean
    fun isSharpnessSupported(): Boolean
    fun isGainSupported(): Boolean
    fun isGammaSupported(): Boolean
    fun isWhiteBalanceSupported(): Boolean
    fun isExposureSupported(): Boolean
    fun isFocusSupported(): Boolean
    fun isZoomSupported(): Boolean
    fun isAutoFocusSupported(): Boolean
    fun isAutoWhiteBalanceSupported(): Boolean
}
