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
package com.jiangdg.ausbc.core.domain.model

/**
 * Encode configuration
 *
 * Configuration for media encoding (video/audio).
 *
 * @author Created for restructuring plan
 */
data class EncodeConfig(
    val outputPath: String,
    val videoConfig: VideoConfig? = null,
    val audioConfig: AudioConfig? = null,
    val muxerType: MuxerType = MuxerType.MP4
) {
    /**
     * Check if this is a video encoding config
     */
    fun isVideoEncode(): Boolean = videoConfig != null

    /**
     * Check if this is an audio encoding config
     */
    fun isAudioEncode(): Boolean = audioConfig != null

    /**
     * Check if this is a mixed encoding config (video + audio)
     */
    fun isMixedEncode(): Boolean = videoConfig != null && audioConfig != null

    /**
     * Check if the configuration is valid
     */
    fun isValid(): Boolean {
        return outputPath.isNotEmpty() && (videoConfig != null || audioConfig != null)
    }

    /**
     * Builder for EncodeConfig
     */
    class Builder {
        private var outputPath: String = ""
        private var videoConfig: VideoConfig? = null
        private var audioConfig: AudioConfig? = null
        private var muxerType: MuxerType = MuxerType.MP4

        fun setOutputPath(path: String) = apply { outputPath = path }
        fun setVideoConfig(config: VideoConfig) = apply { videoConfig = config }
        fun setAudioConfig(config: AudioConfig) = apply { audioConfig = config }
        fun setMuxerType(type: MuxerType) = apply { muxerType = type }

        fun build(): EncodeConfig = EncodeConfig(
            outputPath = outputPath,
            videoConfig = videoConfig,
            audioConfig = audioConfig,
            muxerType = muxerType
        )
    }

    companion object {
        /**
         * Create a default video encode config
         */
        fun defaultVideo(outputPath: String): EncodeConfig = Builder()
            .setOutputPath(outputPath)
            .setVideoConfig(VideoConfig.default())
            .build()

        /**
         * Create a default audio encode config
         */
        fun defaultAudio(outputPath: String): EncodeConfig = Builder()
            .setOutputPath(outputPath)
            .setAudioConfig(AudioConfig())
            .build()

        /**
         * Create a default mixed encode config (video + audio)
         */
        fun defaultMixed(outputPath: String): EncodeConfig = Builder()
            .setOutputPath(outputPath)
            .setVideoConfig(VideoConfig.default())
            .setAudioConfig(AudioConfig())
            .build()
    }
}

/**
 * Video encoding configuration
 */
data class VideoConfig(
    val width: Int = 1280,
    val height: Int = 720,
    val bitrate: Int = 5_000_000, // 5 Mbps
    val frameRate: Int = 30,
    val codec: VideoCodec = VideoCodec.H264,
    val iFrameInterval: Int = 2, // seconds
    val profile: VideoProfile = VideoProfile.BASELINE,
    val level: VideoLevel = VideoLevel.LEVEL_3_1
) {
    /**
     * Check if the configuration is valid
     */
    fun isValid(): Boolean {
        return width > 0 && height > 0 && bitrate > 0 && frameRate > 0
    }

    /**
     * Get the aspect ratio
     */
    fun getAspectRatio(): Double {
        return width.toDouble() / height.toDouble()
    }

    companion object {
        /**
         * Create a default video config
         */
        fun default(): VideoConfig = VideoConfig()

        /**
         * Create a HD video config (720p)
         */
        fun hd(): VideoConfig = VideoConfig(
            width = 1280,
            height = 720,
            bitrate = 5_000_000,
            frameRate = 30
        )

        /**
         * Create a Full HD video config (1080p)
         */
        fun fullHd(): VideoConfig = VideoConfig(
            width = 1920,
            height = 1080,
            bitrate = 10_000_000,
            frameRate = 30
        )
    }
}

/**
 * Video codec enum
 */
enum class VideoCodec {
    H264,
    H265,
    VP8,
    VP9
}

/**
 * Video profile enum
 */
enum class VideoProfile {
    BASELINE,
    MAIN,
    HIGH
}

/**
 * Video level enum (H.264)
 */
enum class VideoLevel {
    LEVEL_3_0,
    LEVEL_3_1,
    LEVEL_4_0,
    LEVEL_4_1,
    LEVEL_4_2,
    LEVEL_5_0
}

/**
 * Muxer type enum
 */
enum class MuxerType {
    MP4,
    WEBM,
    MKV
}

/**
 * Audio encoding configuration
 */
data class AudioConfig(
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    val bitrate: Int = 128000, // 128 kbps
    val codec: AudioCodec = AudioCodec.AAC,
    val profile: AudioProfile = AudioProfile.LC
) {
    /**
     * Check if the configuration is valid
     */
    fun isValid(): Boolean {
        return sampleRate > 0 && channelCount > 0 && bitrate > 0
    }

    /**
     * Get the bit depth
     */
    fun getBitDepth(): Int = 16

    companion object {
        /**
         * Create a default audio config
         */
        fun default(): AudioConfig = AudioConfig()

        /**
         * Create a high quality audio config
         */
        fun highQuality(): AudioConfig = AudioConfig(
            sampleRate = 48000,
            channelCount = 2,
            bitrate = 192000
        )
    }
}

/**
 * Audio codec enum
 */
enum class AudioCodec {
    AAC,
    OPUS,
    MP3
}

/**
 * Audio profile enum
 */
enum class AudioProfile {
    LC,
    MAIN,
    SSR,
    LTP
}
