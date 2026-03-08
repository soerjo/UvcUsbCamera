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
package com.jiangdg.ausbc.encode.engine

import android.view.Surface
import com.jiangdg.ausbc.core.contract.EncodedData
import com.jiangdg.ausbc.core.contract.EncodeState
import com.jiangdg.ausbc.core.contract.IEncodeEngine
import com.jiangdg.ausbc.core.common.result.EncodeResult
import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.domain.model.EncodeConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * H.264 video encode engine
 *
 * Implements IEncodeEngine for H.264 video encoding.
 * This is a simplified placeholder implementation.
 *
 * @author Created for restructuring plan
 */
class H264EncodeEngine : IEncodeEngine {

    private val _state = MutableStateFlow<EncodeState>(EncodeState.Idle)
    private val encodedDataChannel = Channel<EncodedData>(Channel.BUFFERED)

    private var config: EncodeConfig? = null
    private var inputSurface: Surface? = null

    override suspend fun initialize(encodeConfig: EncodeConfig): EncodeResult = try {
        if (_state.value !is EncodeState.Idle) {
            throw IllegalStateException("Encoder already initialized")
        }

        val videoConfig = encodeConfig.videoConfig
            ?: throw IllegalArgumentException("Video config required for H264 encoder")

        config = encodeConfig
        _state.value = EncodeState.Initialized
        EncodeResult.Initialized
    } catch (e: Exception) {
        EncodeResult.EncodeFailed(CameraError.EncodeError(
            reason = "Failed to initialize H.264 encoder",
            encoderType = CameraError.EncoderType.H264,
            cause = e
        ))
    }

    override suspend fun startEncoding(): EncodeResult = try {
        if (_state.value !is EncodeState.Initialized && _state.value !is EncodeState.Paused) {
            throw IllegalStateException("Encoder not initialized")
        }

        _state.value = EncodeState.Encoding
        EncodeResult.Started
    } catch (e: Exception) {
        EncodeResult.EncodeFailed(CameraError.EncodeError(
            reason = "Failed to start encoding",
            encoderType = CameraError.EncoderType.H264,
            cause = e
        ))
    }

    override suspend fun stopEncoding(): EncodeResult = try {
        if (_state.value !is EncodeState.Encoding) {
            throw IllegalStateException("Encoder not encoding")
        }

        _state.value = EncodeState.Stopping(config?.outputPath)
        _state.value = EncodeState.Idle
        EncodeResult.Stopped(config?.outputPath ?: "Unknown", 0)
    } catch (e: Exception) {
        EncodeResult.EncodeFailed(CameraError.EncodeError(
            reason = "Failed to stop encoding",
            encoderType = CameraError.EncoderType.H264,
            cause = e
        ))
    }

    override suspend fun pauseEncoding(): EncodeResult = try {
        if (_state.value !is EncodeState.Encoding) {
            throw IllegalStateException("Encoder not encoding")
        }

        _state.value = EncodeState.Paused
        EncodeResult.Started // Placeholder
    } catch (e: Exception) {
        EncodeResult.EncodeFailed(CameraError.EncodeError(
            reason = "Failed to pause encoding",
            encoderType = CameraError.EncoderType.H264,
            cause = e
        ))
    }

    override suspend fun resumeEncoding(): EncodeResult = try {
        if (_state.value !is EncodeState.Paused) {
            throw IllegalStateException("Encoder not paused")
        }

        _state.value = EncodeState.Encoding
        EncodeResult.Started // Placeholder
    } catch (e: Exception) {
        EncodeResult.EncodeFailed(CameraError.EncodeError(
            reason = "Failed to resume encoding",
            encoderType = CameraError.EncoderType.H264,
            cause = e
        ))
    }

    override fun getEncodedData(): Flow<EncodedData> = encodedDataChannel.receiveAsFlow()

    override fun getEncodeState(): EncodeState = _state.value

    override suspend fun release() {
        inputSurface?.release()
        inputSurface = null
        config = null
        encodedDataChannel.close()
        _state.value = EncodeState.Idle
    }

    override fun isInitialized(): Boolean = _state.value is EncodeState.Initialized || _state.value is EncodeState.Encoding || _state.value is EncodeState.Paused

    override fun isEncoding(): Boolean = _state.value is EncodeState.Encoding

    /**
     * Get the input surface for OpenGL rendering
     */
    fun getInputSurface(): Surface? = inputSurface

    companion object {
        private const val TAG = "H264EncodeEngine"
    }
}
