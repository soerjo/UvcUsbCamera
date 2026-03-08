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
package com.jiangdg.ausbc.core.common.result

import com.jiangdg.ausbc.core.common.error.CameraError

/**
 * Result wrapper for audio operations
 *
 * @author Created for restructuring plan
 */
sealed class AudioResult {
    /**
     * Audio strategy initialized successfully
     */
    data class Initialized(val sampleRate: Int, val channelCount: Int) : AudioResult()

    /**
     * Audio recording started
     */
    data class RecordingStarted(val bufferSize: Int) : AudioResult()

    /**
     * Audio recording stopped
     */
    data class RecordingStopped(val outputPath: String, val durationMs: Long) : AudioResult()

    /**
     * Audio data received
     */
    data class AudioData(
        val data: ByteArray,
        val size: Int,
        val timestamp: Long
    ) : AudioResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AudioData

            if (!data.contentEquals(other.data)) return false
            if (size != other.size) return false
            if (timestamp != other.timestamp) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + size
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    /**
     * Audio operation failed
     */
    data class AudioFailed(val error: CameraError) : AudioResult()

    /**
     * Check if the result is a success
     */
    val isSuccess: Boolean
        get() = this !is AudioFailed

    companion object {
        /**
         * Create a failed audio result
         */
        fun failed(error: CameraError): AudioResult = AudioFailed(error)
    }
}
