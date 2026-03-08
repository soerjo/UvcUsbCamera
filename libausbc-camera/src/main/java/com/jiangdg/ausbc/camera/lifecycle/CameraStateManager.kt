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
package com.jiangdg.ausbc.camera.lifecycle

import com.jiangdg.ausbc.core.common.error.CameraError
import com.jiangdg.ausbc.core.domain.model.CameraState
import com.jiangdg.ausbc.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera state manager
 *
 * Manages camera state transitions and ensures valid state changes.
 * Implements a state machine for camera lifecycle.
 *
 * State Transitions:
 * Idle -> Opening -> Opened -> Previewing -> Recording
 * -> Closing -> Closed -> Idle
 *
 * Error states can occur from any state except Idle.
 *
 * @author Created for restructuring plan
 */
@Singleton
class CameraStateManager @Inject constructor() {

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    /**
     * Transition to Opening state
     */
    fun beginOpening(): Boolean {
        return if (_cameraState.value is CameraState.Idle) {
            _cameraState.value = CameraState.Opening
            Logger.d(TAG, "State: Idle -> Opening")
            true
        } else {
            Logger.w(TAG, "Cannot open camera from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition to Opened state
     */
    fun completeOpening(width: Int, height: Int): Boolean {
        return if (_cameraState.value is CameraState.Opening) {
            _cameraState.value = CameraState.Opened(width, height)
            Logger.d(TAG, "State: Opening -> Opened ($width x $height)")
            true
        } else {
            Logger.w(TAG, "Cannot complete opening from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition to Previewing state
     */
    fun beginPreviewing(width: Int, height: Int, fps: Float = 30f): Boolean {
        return if (_cameraState.value is CameraState.Opened || _cameraState.value is CameraState.Closed) {
            _cameraState.value = CameraState.Previewing(width, height, fps)
            Logger.d(TAG, "State: ${_cameraState.value} -> Previewing ($width x $height @ ${fps}fps)")
            true
        } else {
            Logger.w(TAG, "Cannot start preview from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition from Previewing to Opened
     */
    fun stopPreviewing(): Boolean {
        return if (_cameraState.value is CameraState.Previewing) {
            val previewingState = _cameraState.value as CameraState.Previewing
            _cameraState.value = CameraState.Opened(
                previewingState.previewWidth,
                previewingState.previewHeight
            )
            Logger.d(TAG, "State: Previewing -> Opened")
            true
        } else {
            Logger.w(TAG, "Cannot stop preview from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition to Recording state
     */
    fun beginRecording(filePath: String): Boolean {
        return if (_cameraState.value is CameraState.Previewing) {
            _cameraState.value = CameraState.Recording(filePath, 0)
            Logger.d(TAG, "State: Previewing -> Recording ($filePath)")
            true
        } else {
            Logger.w(TAG, "Cannot start recording from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Update recording duration
     */
    fun updateRecordingDuration(durationMs: Long): Boolean {
        return if (_cameraState.value is CameraState.Recording) {
            val recordingState = _cameraState.value as CameraState.Recording
            _cameraState.value = CameraState.Recording(recordingState.filePath, durationMs)
            true
        } else {
            false
        }
    }

    /**
     * Transition from Recording to Previewing
     */
    fun stopRecording(): Boolean {
        return if (_cameraState.value is CameraState.Recording) {
            val recordingState = _cameraState.value as CameraState.Recording
            _cameraState.value = CameraState.Previewing(
                recordingState.durationMs.let { 640 },
                recordingState.durationMs.let { 480 },
                30f
            )
            Logger.d(TAG, "State: Recording -> Previewing")
            true
        } else {
            Logger.w(TAG, "Cannot stop recording from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition to Closing state
     */
    fun beginClosing(): Boolean {
        return if (_cameraState.value !is CameraState.Idle &&
                   _cameraState.value !is CameraState.Opening &&
                   _cameraState.value !is CameraState.Closing) {
            _cameraState.value = CameraState.Closing
            Logger.d(TAG, "State: ${_cameraState.value} -> Closing")
            true
        } else {
            Logger.w(TAG, "Cannot close from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition to Closed state
     */
    fun completeClosing(): Boolean {
        return if (_cameraState.value is CameraState.Closing) {
            _cameraState.value = CameraState.Closed
            Logger.d(TAG, "State: Closing -> Closed")
            true
        } else {
            Logger.w(TAG, "Cannot complete closing from state: ${_cameraState.value}")
            false
        }
    }

    /**
     * Transition to Idle state
     */
    fun resetToIdle(): Boolean {
        _cameraState.value = CameraState.Idle
        Logger.d(TAG, "State: -> Idle")
        return true
    }

    /**
     * Transition to Error state
     */
    fun enterError(error: CameraError): Boolean {
        Logger.e(TAG, "State: ${_cameraState.value} -> Error: ${error.message}")
        _cameraState.value = CameraState.Error(error)
        return true
    }

    /**
     * Check if camera is in a usable state
     */
    fun isUsable(): Boolean {
        return when (_cameraState.value) {
            is CameraState.Opened, is CameraState.Previewing -> true
            else -> false
        }
    }

    /**
     * Check if camera is busy
     */
    fun isBusy(): Boolean {
        return when (_cameraState.value) {
            is CameraState.Opening, is CameraState.Closing,
            is CameraState.Capturing, is CameraState.Recording -> true
            else -> false
        }
    }

    /**
     * Check if camera is in error state
     */
    fun isInError(): Boolean {
        return _cameraState.value is CameraState.Error
    }

    /**
     * Check if camera is opened
     */
    fun isOpened(): Boolean {
        return _cameraState.value is CameraState.Opened ||
               _cameraState.value is CameraState.Previewing ||
               _cameraState.value is CameraState.Recording
    }

    /**
     * Check if camera is previewing
     */
    fun isPreviewing(): Boolean {
        return _cameraState.value is CameraState.Previewing
    }

    /**
     * Check if camera is recording
     */
    fun isRecording(): Boolean {
        return _cameraState.value is CameraState.Recording
    }

    companion object {
        private const val TAG = "CameraStateManager"
    }
}
