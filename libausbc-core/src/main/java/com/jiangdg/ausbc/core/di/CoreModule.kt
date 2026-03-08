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
package com.jiangdg.ausbc.core.di

import android.content.Context
import com.jiangdg.ausbc.core.domain.repository.ICameraRepository
import com.jiangdg.ausbc.core.domain.repository.IDeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Core dependency injection module
 *
 * Provides core dependencies for the camera system.
 *
 * @author Created for restructuring plan
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    /**
     * Provide application context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    // Note: Repository implementations will be provided by their respective modules:
    // - ICameraRepository by libausbc-camera module
    // - IDeviceRepository by libausbc-camera module
    // - IRenderEngine by libausbc-render module
    // - IEncodeEngine by libausbc-encode module
    // - IAudioStrategy by libausbc-encode module
}
