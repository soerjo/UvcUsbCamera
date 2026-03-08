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
package com.jiangdg.ausbc.utils

/**
 * Logger utility class
 *
 * Provides simple logging functionality.
 *
 * @author Created for restructuring plan
 */
object Logger {

    private const val DEFAULT_TAG = "AUSBC"

    /**
     * Log verbose message
     */
    fun v(tag: String = DEFAULT_TAG, message: String) {
        android.util.Log.v(tag, message)
    }

    /**
     * Log debug message
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        android.util.Log.d(tag, message)
    }

    /**
     * Log info message
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        android.util.Log.i(tag, message)
    }

    /**
     * Log warning message
     */
    fun w(tag: String = DEFAULT_TAG, message: String) {
        android.util.Log.w(tag, message)
    }

    /**
     * Log error message
     */
    fun e(tag: String = DEFAULT_TAG, message: String) {
        android.util.Log.e(tag, message)
    }

    /**
     * Log error with throwable
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        android.util.Log.e(tag, message, throwable)
    }
}
