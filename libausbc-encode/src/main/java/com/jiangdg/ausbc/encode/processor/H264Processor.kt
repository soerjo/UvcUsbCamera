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
package com.jiangdg.ausbc.encode.processor

import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.encode.muxer.IMuxer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * H.264 video processor
 *
 * This is a simplified placeholder implementation.
 *
 * @property width Video width
 * @property height Video height
 * @property bitrate Encoding bitrate in bps
 * @property frameRate Frame rate in fps
 * @property iFrameInterval I-frame interval in seconds
 * @author Created for restructuring plan
 */
class H264Processor(
    private val width: Int,
    private val height: Int,
    private var bitrate: Int = 5_000_000,
    private val frameRate: Int = 30,
    private val iFrameInterval: Int = 2
) : IProcessor {

    private val _state = MutableStateFlow<ProcessorState>(ProcessorState.Idle)
    val state: StateFlow<ProcessorState> = _state.asStateFlow()

    private val encodedDataChannel = Channel<ProcessedData>(Channel.BUFFERED)
    private var muxer: IMuxer? = null

    override fun getType(): IProcessor.Type = IProcessor.Type.VIDEO_H264

    override suspend fun initialize(): Boolean = try {
        if (_state.value != ProcessorState.Idle) {
            throw IllegalStateException("Processor already initialized")
        }

        _state.value = ProcessorState.Initialized
        true
    } catch (e: Exception) {
        _state.value = ProcessorState.Error(e.message ?: "Unknown error")
        false
    }

    override suspend fun start() {
        if (_state.value != ProcessorState.Initialized && _state.value != ProcessorState.Paused) {
            throw IllegalStateException("Processor not initialized")
        }

        _state.value = ProcessorState.Processing
    }

    override suspend fun stop() {
        if (_state.value != ProcessorState.Processing) {
            throw IllegalStateException("Processor not processing")
        }

        _state.value = ProcessorState.Stopped
    }

    override suspend fun pause() {
        if (_state.value != ProcessorState.Processing) {
            throw IllegalStateException("Processor not processing")
        }

        _state.value = ProcessorState.Paused
    }

    override suspend fun resume() {
        if (_state.value != ProcessorState.Paused) {
            throw IllegalStateException("Processor not paused")
        }

        _state.value = ProcessorState.Processing
    }

    override fun isProcessing(): Boolean = _state.value == ProcessorState.Processing

    override fun isInitialized(): Boolean = _state.value is ProcessorState.Initialized ||
                                       _state.value is ProcessorState.Processing ||
                                       _state.value is ProcessorState.Paused ||
                                       _state.value is ProcessorState.Stopped

    override suspend fun putRawData(data: RawData) {
        if (!isProcessing()) {
            return
        }
        // Placeholder - no actual processing
    }

    override fun getEncodedData(): Flow<ProcessedData> = encodedDataChannel.receiveAsFlow()

    override suspend fun updateBitRate(bitrate: Int) {
        this.bitrate = bitrate
    }

    override suspend fun release() {
        muxer = null
        encodedDataChannel.close()
        _state.value = ProcessorState.Idle
    }

    override suspend fun setMuxer(muxer: IMuxer?) {
        this.muxer = muxer
    }

    override fun getConfig(): ProcessorConfig = ProcessorConfig(
        isVideo = true,
        bitrate = bitrate,
        configKey = "h264"
    )

    /**
     * Processor state
     */
    sealed class ProcessorState {
        data object Idle : ProcessorState()
        data object Initialized : ProcessorState()
        data object Processing : ProcessorState()
        data object Paused : ProcessorState()
        data object Stopped : ProcessorState()
        data class Error(val message: String) : ProcessorState()
    }
}
