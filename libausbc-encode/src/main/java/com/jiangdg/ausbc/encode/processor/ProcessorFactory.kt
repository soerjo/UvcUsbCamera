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

import com.jiangdg.ausbc.core.domain.model.EncodeConfig

/**
 * Factory for creating media processors
 *
 * Creates appropriate processor instances based on configuration.
 *
 * @author Created for restructuring plan
 */
object ProcessorFactory {

    /**
     * Create a processor based on encode config
     *
     * @param config Encode configuration
     * @return IProcessor instance
     * @throws UnsupportedOperationException if codec is not supported
     */
    fun createProcessor(config: EncodeConfig): IProcessor {
        // Video processor
        val videoConfig = config.videoConfig
        if (videoConfig != null) {
            return when (videoConfig.codec) {
                com.jiangdg.ausbc.core.domain.model.VideoCodec.H264 -> createH264Processor(config)
                else -> throw UnsupportedOperationException(
                    "Video codec ${videoConfig.codec} not supported yet"
                )
            }
        }

        // Audio processor
        val audioConfig = config.audioConfig
        if (audioConfig != null) {
            return when (audioConfig.codec) {
                com.jiangdg.ausbc.core.domain.model.AudioCodec.AAC -> createAACProcessor(config)
                else -> throw UnsupportedOperationException(
                    "Audio codec ${audioConfig.codec} not supported yet"
                )
            }
        }

        throw IllegalArgumentException("Invalid encode config: no video or audio config")
    }

    /**
     * Create H.264 video processor
     *
     * @param config Encode configuration
     * @return H264Processor instance
     */
    fun createH264Processor(config: EncodeConfig): IProcessor {
        val videoConfig = config.videoConfig
            ?: throw IllegalArgumentException("Video config required for H264 processor")

        return H264Processor(
            width = videoConfig.width,
            height = videoConfig.height,
            bitrate = videoConfig.bitrate,
            frameRate = videoConfig.frameRate,
            iFrameInterval = videoConfig.iFrameInterval
        )
    }

    /**
     * Create AAC audio processor
     *
     * @param config Encode configuration
     * @return AACProcessor instance
     */
    fun createAACProcessor(config: EncodeConfig): IProcessor {
        val audioConfig = config.audioConfig
            ?: throw IllegalArgumentException("Audio config required for AAC processor")

        return AACProcessor(
            sampleRate = audioConfig.sampleRate,
            channelCount = audioConfig.channelCount,
            bitrate = audioConfig.bitrate
        )
    }

    /**
     * Create processor by type
     *
     * @param type Processor type
     * @param config Processor-specific configuration
     * @return IProcessor instance
     */
    fun createByType(type: IProcessor.Type, config: Map<String, Any> = emptyMap()): IProcessor {
        return when (type) {
            IProcessor.Type.VIDEO_H264 -> {
                H264Processor(
                    width = config["width"] as? Int ?: 1280,
                    height = config["height"] as? Int ?: 720,
                    bitrate = config["bitrate"] as? Int ?: 5_000_000,
                    frameRate = config["frameRate"] as? Int ?: 30,
                    iFrameInterval = config["iFrameInterval"] as? Int ?: 2
                )
            }
            IProcessor.Type.AUDIO_AAC -> {
                AACProcessor(
                    sampleRate = config["sampleRate"] as? Int ?: 44100,
                    channelCount = config["channelCount"] as? Int ?: 1,
                    bitrate = config["bitrate"] as? Int ?: 128000
                )
            }
            else -> throw UnsupportedOperationException("Processor type $type not supported yet")
        }
    }
}
