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
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import android.hardware.usb.UsbDevice
import com.jiangdg.usb.USBMonitor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for UsbDeviceDataSource
 *
 * @author Created for restructuring plan
 */
class UsbDeviceDataSourceTest {

    private lateinit var usbDeviceDataSource: UsbDeviceDataSource
    private lateinit var mockUsbMonitorWrapper: UsbMonitorWrapper
    private lateinit var mockUsbDevice: UsbDevice
    private lateinit var mockUsbControlBlock: USBMonitor.UsbControlBlock

    @Before
    fun setup() {
        mockUsbMonitorWrapper = mockk()
        mockUsbDevice = mockk()
        mockUsbControlBlock = mockk()

        // Set up mock defaults
        every { mockUsbDevice.deviceId } returns 1
        every { mockUsbDevice.vendorId } returns 0x046d
        every { mockUsbDevice.productId } returns 0x0825
        every { mockUsbDevice.deviceName } returns "Test Camera"
        every { mockUsbDevice.interfaces } returns emptyArray()

        every { mockUsbMonitorWrapper.hasPermission(mockUsbDevice) } returns true
        every { mockUsbMonitorWrapper.getUsbControlBlock(any()) } returns mockUsbControlBlock
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `hasPermission returns true when permission granted`() {
        // Arrange
        every { mockUsbMonitorWrapper.hasPermission(mockUsbDevice) } returns true

        // Act
        val hasPermission = usbDeviceDataSource.hasPermission(mockUsbDevice)

        // Assert
        assertTrue(hasPermission)
    }

    @Test
    fun `hasPermission returns false when permission denied`() {
        // Arrange
        every { mockUsbMonitorWrapper.hasPermission(mockUsbDevice) } returns false

        // Act
        val hasPermission = usbDeviceDataSource.hasPermission(mockUsbDevice)

        // Assert
        assertFalse(hasPermission)
    }

    @Test
    fun `isSupportedCamera returns true for UVC device`() {
        // Arrange
        val mockInterface = mockk<android.hardware.usb.UsbInterface>()
        every { mockInterface.interfaceClass } returns 0x0E // Video class
        every { mockUsbDevice.interfaces } returns arrayOf(mockInterface)

        // Act
        val isSupported = usbDeviceDataSource.isSupportedCamera(mockUsbDevice)

        // Assert
        assertTrue(isSupported)
    }

    @Test
    fun `isSupportedCamera returns true for known UVC device`() {
        // Arrange - Logitech C270
        every { mockUsbDevice.vendorId } returns 0x046d
        every { mockUsbDevice.productId } returns 0x0825
        every { mockUsbDevice.interfaces } returns emptyArray()

        // Act
        val isSupported = usbDeviceDataSource.isSupportedCamera(mockUsbDevice)

        // Assert
        assertTrue(isSupported)
    }

    @Test
    fun `isSupportedCamera returns false for non-UVC device`() {
        // Arrange
        every { mockUsbDevice.vendorId } returns 0x1234
        every { mockUsbDevice.productId } returns 0x5678
        every { mockUsbDevice.interfaces } returns emptyArray()

        // Act
        val isSupported = usbDeviceDataSource.isSupportedCamera(mockUsbDevice)

        // Assert
        assertFalse(isSupported)
    }

    @Test
    fun `getUsbControlBlock returns control block when permission granted`() {
        // Arrange
        every { mockUsbMonitorWrapper.getUsbControlBlock(1) } returns mockUsbControlBlock

        // Act
        val ctrlBlock = usbDeviceDataSource.getUsbControlBlock(mockUsbDevice)

        // Assert
        assertEquals(mockUsbControlBlock, ctrlBlock)
    }

    @Test
    fun `getUsbControlBlock returns null when no permission`() {
        // Arrange
        every { mockUsbMonitorWrapper.getUsbControlBlock(any()) } returns null

        // Act
        val ctrlBlock = usbDeviceDataSource.getUsbControlBlock(mockUsbDevice)

        // Assert
        assertEquals(null, ctrlBlock)
    }

    @Test
    fun `requestPermission calls wrapper and returns success`() {
        // Arrange
        var callbackInvoked = false
        every { mockUsbMonitorWrapper.requestPermission(mockUsbDevice, any()) } answers {
            callbackInvoked = true
            val callback = secondArg<(Boolean) -> Unit>()
            callback(true)
            true
        }

        // Act
        val result = usbDeviceDataSource.requestPermission(mockUsbDevice) { granted ->
            callbackInvoked = granted
        }

        // Assert
        assertTrue(result)
        assertTrue(callbackInvoked)
    }

    @Test
    fun `getDeviceList returns flow of devices`() = runTest {
        // Act
        val deviceListFlow = usbDeviceDataSource.getDeviceList()

        // Assert
        val devices = deviceListFlow.first()
        assertTrue(devices is List)
    }
}

/**
 * Unit tests for CameraRepository
 *
 * @author Created for restructuring plan
 */
class CameraRepositoryTest {

    private lateinit var cameraRepository: com.jiangdg.ausbc.camera.data.repository.CameraRepository
    private lateinit var mockDeviceRepository: com.jiangdg.ausbc.camera.data.repository.DeviceRepository
    private lateinit var mockUvcCameraFactory: com.jiangdg.ausbc.camera.uvc.UvcCameraFactory
    private lateinit var mockUsbDevice: UsbDevice

    @Before
    fun setup() {
        mockDeviceRepository = mockk()
        mockUvcCameraFactory = mockk()
        mockUsbDevice = mockk()

        every { mockUsbDevice.deviceId } returns 1
        every { mockUsbDevice.deviceName } returns "Test Camera"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `openCamera returns success when factory creates camera`() = runTest {
        // Arrange
        val mockCamera = mockk<com.jiangdg.ausbc.core.contract.ICamera>()
        every { mockDeviceRepository.hasPermission(mockUsbDevice) } returns true
        every { mockUvcCameraFactory.createCamera(mockUsbDevice) } returns CameraResult.success(mockCamera)
        every { mockCamera.open(any()) } returns CameraResult.success(Unit)
        every { mockCamera.isOpened() } returns true

        cameraRepository = com.jiangdg.ausbc.camera.data.repository.CameraRepository(
            mockDeviceRepository,
            mockUvcCameraFactory
        )

        // Act
        val result = cameraRepository.openCamera(mockUsbDevice, CameraRequest.default())

        // Assert
        assertTrue(result is CameraResult.Success)
    }

    @Test
    fun `openCamera returns error when permission denied`() = runTest {
        // Arrange
        every { mockDeviceRepository.hasPermission(mockUsbDevice) } returns false

        cameraRepository = com.jiangdg.ausbc.camera.data.repository.CameraRepository(
            mockDeviceRepository,
            mockUvcCameraFactory
        )

        // Act
        val result = cameraRepository.openCamera(mockUsbDevice, CameraRequest.default())

        // Assert
        assertTrue(result is CameraResult.Error)
    }

    @Test
    fun `closeCamera returns success when camera is open`() = runTest {
        // Arrange
        val mockCamera = mockk<com.jiangdg.ausbc.core.contract.ICamera>()
        every { mockDeviceRepository.hasPermission(mockUsbDevice) } returns true
        every { mockUvcCameraFactory.createCamera(mockUsbDevice) } returns CameraResult.success(mockCamera)
        every { mockCamera.open(any()) } returns CameraResult.success(Unit)
        every { mockCamera.close() } returns CameraResult.success(Unit)
        every { mockCamera.isOpened() } returns true

        cameraRepository = com.jiangdg.ausbc.camera.data.repository.CameraRepository(
            mockDeviceRepository,
            mockUvcCameraFactory
        )

        // Open camera first
        cameraRepository.openCamera(mockUsbDevice, CameraRequest.default())

        // Act
        val result = cameraRepository.closeCamera()

        // Assert
        assertTrue(result is CameraResult.Success)
    }

    @Test
    fun `isCameraOpened returns false when no camera is open`() {
        // Arrange
        cameraRepository = com.jiangdg.ausbc.camera.data.repository.CameraRepository(
            mockDeviceRepository,
            mockUvcCameraFactory
        )

        // Act
        val isOpened = cameraRepository.isCameraOpened()

        // Assert
        assertFalse(isOpened)
    }

    @Test
    fun `getPreviewSizes returns common sizes`() = runTest {
        // Arrange
        cameraRepository = com.jiangdg.ausbc.camera.data.repository.CameraRepository(
            mockDeviceRepository,
            mockUvcCameraFactory
        )

        // Act
        val result = cameraRepository.getPreviewSizes(mockUsbDevice, null)

        // Assert
        assertTrue(result is CameraResult.Success)
        assertTrue((result as CameraResult.Success).data.isNotEmpty())
    }

    @Test
    fun `getPreviewSizes with aspectRatio filters correctly`() = runTest {
        // Arrange
        cameraRepository = com.jiangdg.ausbc.camera.data.repository.CameraRepository(
            mockDeviceRepository,
            mockUvcCameraFactory
        )

        // Act - 16:9 aspect ratio
        val result = cameraRepository.getPreviewSizes(mockUsbDevice, 16.0 / 9.0)

        // Assert
        assertTrue(result is CameraResult.Success)
        val sizes = (result as CameraResult.Success).data
        assertTrue(sizes.any { it.aspectRatio == 16.0 / 9.0 })
    }
}
