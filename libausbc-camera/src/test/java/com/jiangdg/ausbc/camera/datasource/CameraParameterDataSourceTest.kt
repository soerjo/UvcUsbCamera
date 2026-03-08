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
package com.jiangdg.ausbc.camera.datasource

import com.jiangdg.ausbc.core.common.result.CameraResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.jiangdg.uvc.UVCCamera
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CameraParameterDataSource
 *
 * @author Created for restructuring plan
 */
class CameraParameterDataSourceTest {

    private lateinit var parameterDataSource: CameraParameterDataSource
    private lateinit var mockUvcCamera: UVCCamera

    @Before
    fun setup() {
        parameterDataSource = CameraParameterDataSource()
        mockUvcCamera = mockk()

        // Set up mock defaults
        every { mockUvcCamera.isOpened } returns true
        every { mockUvcCamera.brightness } returns 128
        every { mockUvcCamera.brightnessMin } returns 0
        every { mockUvcCamera.brightnessMax } returns 255
        every { mockUvcCamera.contrast } returns 128
        every { mockUvcCamera.contrastMin } returns 0
        every { mockUvcCamera.contrastMax } returns 255
        every { mockUvcCamera.isAutoFocus } returns false
        every { mockUvcCamera.isSupportAutoFocus } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setBrightness returns success when camera is open`() = runTest {
        // Arrange
        every { mockUvcCamera.setBrightness(any()) } just Runs
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setBrightness(100)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setBrightness(100) }
    }

    @Test
    fun `setBrightness returns error when camera is closed`() = runTest {
        // Arrange
        every { mockUvcCamera.isOpened } returns false
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setBrightness(100)

        // Assert
        assertTrue(result is CameraResult.Error)
    }

    @Test
    fun `getBrightness returns current brightness value`() = runTest {
        // Arrange
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.getBrightness()

        // Assert
        assertTrue(result is CameraResult.Success)
        assertEquals(128, (result as CameraResult.Success).data)
    }

    @Test
    fun `resetBrightness calls reset on camera`() = runTest {
        // Arrange
        every { mockUvcCamera.resetBrightness() } just Runs
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.resetBrightness()

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.resetBrightness() }
    }

    @Test
    fun `getBrightnessRange returns correct range`() = runTest {
        // Arrange
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val range = parameterDataSource.getBrightnessRange()

        // Assert
        assertNotNull(range)
        assertEquals(0, range?.first)
        assertEquals(255, range?.last)
    }

    @Test
    fun `setContrast returns success when camera is open`() = runTest {
        // Arrange
        every { mockUvcCamera.setContrast(any()) } just Runs
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setContrast(100)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setContrast(100) }
    }

    @Test
    fun `setAutoFocus returns success when supported`() = runTest {
        // Arrange
        every { mockUvcCamera.setAutoFocus(any()) } just Runs
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setAutoFocus(true)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setAutoFocus(true) }
    }

    @Test
    fun `getAutoFocus returns current auto focus state`() = runTest {
        // Arrange
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.getAutoFocus()

        // Assert
        assertTrue(result is CameraResult.Success)
        assertEquals(false, (result as CameraResult.Success).data)
    }

    @Test
    fun `isBrightnessSupported returns true when range exists`() {
        // Arrange
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val supported = parameterDataSource.isBrightnessSupported()

        // Assert
        assertTrue(supported)
    }

    @Test
    fun `operations return error when camera is null`() = runTest {
        // Arrange - camera is null
        parameterDataSource.setCamera(null)

        // Act
        val result = parameterDataSource.getBrightness()

        // Assert
        assertTrue(result is CameraResult.Error)
    }

    @Test
    fun `setFocus returns success when camera is open`() = runTest {
        // Arrange
        every { mockUvcCamera.setFocus(any()) } just Runs
        every { mockUvcCamera.focusMin } returns 0
        every { mockUvcCamera.focusMax } returns 255
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setFocus(100)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setFocus(100) }
    }

    @Test
    fun `setZoom returns success when supported`() = runTest {
        // Arrange
        every { mockUvcCamera.setZoom(any()) } just Runs
        every { mockUvcCamera.zoomMin } returns 0
        every { mockUvcCamera.zoomMax } returns 100
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setZoom(50)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setZoom(50) }
    }

    @Test
    fun `setGain returns success when camera is open`() = runTest {
        // Arrange
        every { mockUvcCamera.setGain(any()) } just Runs
        every { mockUvcCamera.gainMin } returns 0
        every { mockUvcCamera.gainMax } returns 255
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setGain(128)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setGain(128) }
    }

    @Test
    fun `setWhiteBalance returns success when supported`() = runTest {
        // Arrange
        every { mockUvcCamera.setWhiteBalance(any()) } just Runs
        every { mockUvcCamera.whiteBalanceMin } returns 2000
        every { mockUvcCamera.whiteBalanceMax } returns 10000
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setWhiteBalance(5000)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setWhiteBalance(5000) }
    }

    @Test
    fun `setExposure returns success when camera is open`() = runTest {
        // Arrange
        every { mockUvcCamera.setExposure(any()) } just Runs
        every { mockUvcCamera.exposureMin } returns 0
        every { mockUvcCamera.exposureMax } returns 1000
        parameterDataSource.setCamera(mockUvcCamera)

        // Act
        val result = parameterDataSource.setExposure(500)

        // Assert
        assertTrue(result is CameraResult.Success)
        verify { mockUvcCamera.setExposure(500) }
    }
}
