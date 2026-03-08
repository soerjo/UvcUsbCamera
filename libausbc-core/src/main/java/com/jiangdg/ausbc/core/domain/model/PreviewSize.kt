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
 * Preview size data class
 *
 * Represents a supported camera preview resolution.
 *
 * @author Created for restructuring plan
 */
data class PreviewSize(
    val width: Int,
    val height: Int
) : Comparable<PreviewSize> {
    /**
     * Get the aspect ratio
     */
    val aspectRatio: Double
        get() = width.toDouble() / height.toDouble()

    /**
     * Get the number of megapixels
     */
    val megapixels: Double
        get() = (width * height) / 1_000_000.0

    /**
     * Check if this is a HD resolution (1280x720)
     */
    fun isHD(): Boolean {
        return width == 1280 && height == 720
    }

    /**
     * Check if this is a Full HD resolution (1920x1080)
     */
    fun isFullHD(): Boolean {
        return width == 1920 && height == 1080
    }

    /**
     * Check if this is a 4K resolution (3840x2160)
     */
    fun is4K(): Boolean {
        return width == 3840 && height == 2160
    }

    /**
     * Get a human-readable description
     */
    fun getDescription(): String {
        return when {
            is4K() -> "4K"
            isFullHD() -> "Full HD"
            isHD() -> "HD"
            width == 640 && height == 480 -> "VGA"
            width == 320 && height == 240 -> "QVGA"
            else -> "${width}x${height}"
        }
    }

    /**
     * Compare by total pixels (area)
     */
    override fun compareTo(other: PreviewSize): Int {
        val thisArea = width * height
        val otherArea = other.width * other.height
        return thisArea.compareTo(otherArea)
    }

    override fun toString(): String {
        return "${width}x${height}"
    }

    companion object {
        /**
         * Common preview sizes
         */
        val QVGA = PreviewSize(320, 240)
        val VGA = PreviewSize(640, 480)
        val HD = PreviewSize(1280, 720)
        val FullHD = PreviewSize(1920, 1080)
        val UHD4K = PreviewSize(3840, 2160)

        /**
         * Get common sizes
         */
        fun getCommonSizes(): List<PreviewSize> {
            return listOf(
                PreviewSize(320, 240),
                PreviewSize(352, 288),
                PreviewSize(640, 360),
                PreviewSize(640, 480),
                PreviewSize(800, 448),
                PreviewSize(800, 600),
                PreviewSize(864, 480),
                PreviewSize(960, 544),
                PreviewSize(960, 720),
                PreviewSize(1024, 576),
                PreviewSize(1280, 720),
                PreviewSize(1280, 800),
                PreviewSize(1280, 960),
                PreviewSize(1920, 1080)
            )
        }

        /**
         * Get sizes for a specific aspect ratio
         */
        fun getSizesForAspectRatio(aspectRatio: Double, tolerance: Double = 0.01): List<PreviewSize> {
            return getCommonSizes().filter { size ->
                kotlin.math.abs(size.aspectRatio - aspectRatio) < tolerance
            }
        }

        /**
         * Parse from string (e.g., "640x480")
         */
        fun parse(str: String): PreviewSize? {
            val parts = str.split("x", "X", "*")
            if (parts.size != 2) return null
            return try {
                val width = parts[0].trim().toInt()
                val height = parts[1].trim().toInt()
                PreviewSize(width, height)
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}
