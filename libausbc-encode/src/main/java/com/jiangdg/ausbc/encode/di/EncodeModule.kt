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
package com.jiangdg.ausbc.encode.di

import android.content.Context
import com.jiangdg.ausbc.core.contract.IAudioStrategy
import com.jiangdg.ausbc.core.contract.IEncodeEngine
import com.jiangdg.ausbc.encode.audio.SystemMicStrategy
import com.jiangdg.ausbc.encode.engine.AACEncodeEngine
import com.jiangdg.ausbc.encode.engine.H264EncodeEngine
import com.jiangdg.ausbc.encode.muxer.IMuxer
import com.jiangdg.ausbc.encode.muxer.Mp4MuxerV2
import com.jiangdg.ausbc.encode.muxer.MuxerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Encode module dependency injection
 *
 * Provides encoding-related dependencies.
 *
 * @author Created for restructuring plan
 */
@Module
@InstallIn(SingletonComponent::class)
object EncodeModule {

    /**
     * Provide H.264 encode engine
     */
    @Provides
    @Singleton
    @H264Encoder
    fun provideH264EncodeEngine(): IEncodeEngine = H264EncodeEngine()

    /**
     * Provide AAC encode engine
     */
    @Provides
    @Singleton
    @AACEncoder
    fun provideAACEncodeEngine(): IEncodeEngine = AACEncodeEngine()

    /**
     * Provide system microphone strategy
     */
    @Provides
    @Singleton
    @SystemMic
    fun provideSystemMicStrategy(
        @ApplicationContext context: Context
    ): IAudioStrategy = SystemMicStrategy(context)

    /**
     * Provide MP4 muxer
     */
    @Provides
    @Mp4Muxer
    fun provideMp4Muxer(
        @ApplicationContext context: Context
    ): IMuxer = Mp4MuxerV2(context)

    /**
     * Provide muxer factory
     */
    @Provides
    @Singleton
    fun provideMuxerFactory(): MuxerFactory = MuxerFactory
}

/**
 * Qualifier for H.264 encoder
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class H264Encoder

/**
 * Qualifier for AAC encoder
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AACEncoder

/**
 * Qualifier for system microphone
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SystemMic

/**
 * Qualifier for MP4 muxer
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Mp4Muxer
