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
 * Generic result wrapper for camera operations
 *
 * This sealed class provides a type-safe way to handle operations that can fail.
 * It follows the Result pattern from functional programming.
 *
 * Usage example:
 * ```kotlin
 * when (val result = repository.openCamera(request)) {
 *     is CameraResult.Success -> {
 *         val camera = result.data
 *         // Handle success
 *     }
 *     is CameraResult.Error -> {
 *         val error = result.error
 *         // Handle error
 *     }
 * }
 * ```
 *
 * @param T The type of data on success
 * @author Created for restructuring plan
 */
sealed class CameraResult<out T> {
    /**
     * Represents a successful operation containing data
     */
    data class Success<T>(val data: T) : CameraResult<T>()

    /**
     * Represents a failed operation with error information
     */
    data class Error(val error: CameraError) : CameraResult<Nothing>()

    /**
     * Check if the result is successful
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Check if the result is an error
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Get the data if successful, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Get the data if successful, or throw the error
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw error.toException()
    }

    /**
     * Get the data if successful, or return a default value
     */
    fun getOrElse(defaultValue: () -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue()
    }

    /**
     * Map the success value to a different type
     */
    fun <R> map(transform: (T) -> R): CameraResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this as CameraResult<R>
    }

    /**
     * Map the error to a different error
     */
    fun mapError(transform: (CameraError) -> CameraError): CameraResult<T> = when (this) {
        is Success -> this
        is Error -> Error(transform(error))
    }

    /**
     * FlatMap the success value to a new Result
     */
    suspend fun <R> flatMap(transform: suspend (T) -> CameraResult<R>): CameraResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this as CameraResult<R>
    }

    /**
     * Execute an action based on the result type
     */
    inline fun onSuccess(action: (T) -> Unit): CameraResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Execute an action if the result is an error
     */
    inline fun onError(action: (CameraError) -> Unit): CameraResult<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }

    /**
     * Execute an action based on the result type
     */
    inline fun fold(
        onSuccess: (T) -> Unit,
        onError: (CameraError) -> Unit
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(error)
        }
    }

    companion object {
        /**
         * Create a successful result
         */
        fun <T> success(data: T): CameraResult<T> = Success(data)

        /**
         * Create an error result
         */
        fun <T> error(error: CameraError): CameraResult<T> = Error(error)

        /**
         * Catch exceptions and convert to error result
         */
        inline fun <T> catch(block: () -> CameraResult<T>): CameraResult<T> {
            return try {
                block()
            } catch (e: Throwable) {
                Error(com.jiangdg.ausbc.core.common.error.ErrorHandler.fromThrowable(e))
            }
        }
    }
}

/**
 * Convenience extension to wrap a value in CameraResult.Success
 */
fun <T> T.toSuccessResult(): CameraResult<T> = CameraResult.success(this)

/**
 * Convenience extension to wrap an error in CameraResult.Error
 */
fun CameraError.toErrorResult(): CameraResult<Nothing> = CameraResult.error(this)
