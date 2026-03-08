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
 * Result type for encode file operations
 *
 * @param T The type of data on success
 *
 * @author Created for restructuring plan
 */
sealed class EncodeFileResult<out T> {
    /**
     * Success result with data
     *
     * @property data The result data
     */
    data class Success<T>(val data: T) : EncodeFileResult<T>()

    /**
     * Error result with error information
     *
     * @property error The error that occurred
     */
    data class Error(val error: CameraError) : EncodeFileResult<Nothing>()

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
     * Map the success value to a new type
     *
     * @param transform Function to transform the data
     * @return New EncodeFileResult with transformed data
     */
    fun <R> map(transform: (T) -> R): EncodeFileResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Flat map the success value to a new EncodeFileResult
     *
     * @param transform Function to transform the data to a new EncodeFileResult
     * @return New EncodeFileResult from transformation
     */
    fun <R> flatMap(transform: (T) -> EncodeFileResult<R>): EncodeFileResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    /**
     * Map the error to a new EncodeFileResult
     *
     * @param transform Function to transform the error
     * @return New EncodeFileResult with transformed error
     */
    fun mapError(transform: (CameraError) -> CameraError): EncodeFileResult<T> = when (this) {
        is Success -> this
        is Error -> Error(transform(error))
    }

    /**
     * Fold the result into a single value
     *
     * @param onSuccess Function to apply on success
     * @param onError Function to apply on error
     * @return The folded value
     */
    fun <R> fold(onSuccess: (T) -> R, onError: (CameraError) -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(error)
    }

    /**
     * Get the data or throw an exception if error
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw EncodeFileException(error)
    }

    /**
     * Get the data or return default value
     *
     * @param defaultValue Default value to return if error
     * @return The data or default value
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }

    /**
     * Get the data or compute default value
     *
     * @param defaultValue Function to compute default value
     * @return The data or computed default value
     */
    fun getOrElse(defaultValue: () -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue()
    }

    /**
     * Execute action based on result state
     *
     * @param onSuccess Action to execute on success
     * @param onError Action to execute on error
     */
    fun onSuccess(onSuccess: (T) -> Unit): EncodeFileResult<T> {
        if (this is Success) onSuccess(data)
        return this
    }

    /**
     * Execute action on error
     *
     * @param onError Action to execute on error
     */
    fun onError(onError: (CameraError) -> Unit): EncodeFileResult<T> {
        if (this is Error) onError(error)
        return this
    }

    companion object {
        /**
         * Create a success result
         */
        fun <T> success(data: T): EncodeFileResult<T> = Success(data)

        /**
         * Create an error result
         */
        fun <T> error(err: CameraError): EncodeFileResult<T> = Error(err)

        /**
         * Catch exceptions and return EncodeFileResult
         *
         * @param block Block to execute
         * @return EncodeFileResult with data or error
         */
        suspend fun <T> catch(block: suspend () -> T): EncodeFileResult<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(CameraError.EncodeError(e.message ?: "Unknown error"))
        }

        /**
         * Catch exceptions and return EncodeFileResult (non-suspend)
         *
         * @param block Block to execute
         * @return EncodeFileResult with data or error
         */
        fun <T> catching(block: () -> T): EncodeFileResult<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(CameraError.EncodeError(e.message ?: "Unknown error"))
        }
    }
}

/**
 * Exception thrown when getting data from failed EncodeFileResult
 *
 * @property error The error that occurred
 */
class EncodeFileException(val error: CameraError) : Exception(error.message)

/**
 * Convenience extension to create success result
 */
@Suppress("UNCHECKED_CAST")
fun <T> T.toEncodeFileSuccess(): EncodeFileResult<T> = EncodeFileResult.Companion.success(this)

/**
 * Convenience extension to create error result
 */
fun CameraError.toEncodeFileError(): EncodeFileResult<Nothing> = EncodeFileResult.Companion.error(this)
