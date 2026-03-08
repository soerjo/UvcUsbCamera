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
package com.jiangdg.ausbc.camera.di

import com.jiangdg.ausbc.camera.data.repository.CameraRepository
import com.jiangdg.ausbc.camera.data.repository.DeviceRepository
import com.jiangdg.ausbc.camera.platform.UsbDeviceManager
import com.jiangdg.ausbc.camera.uvc.UvcCameraFactory
import com.jiangdg.ausbc.core.domain.repository.ICameraRepository
import com.jiangdg.ausbc.core.domain.repository.IDeviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Camera module dependency injection
 *
 * Provides camera-related dependencies.
 *
 * @author Created for restructuring plan
 */
@Module(
    includes = [
        CameraBindings::class
    ]
)
@InstallIn(SingletonComponent::class)
object CameraModule {

    // Repository implementations provided by bindings below
}

/**
 * Camera bindings module
 *
 * Binds interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraBindings {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(impl: CameraRepository): ICameraRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepository): IDeviceRepository
}
