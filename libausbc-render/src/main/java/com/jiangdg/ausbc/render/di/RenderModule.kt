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
package com.jiangdg.ausbc.render.di

import com.jiangdg.ausbc.core.contract.IRenderEngine
import com.jiangdg.ausbc.render.engine.OpenGLRenderEngine
import com.jiangdg.ausbc.render.engine.SurfaceRenderEngine
import com.jiangdg.ausbc.core.di.OpenGLRender
import com.jiangdg.ausbc.core.di.MedianRender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Render module dependency injection
 *
 * Provides render-related dependencies.
 *
 * @author Created for restructuring plan
 */
@Module(
    includes = [
        RenderBindings::class
    ]
)
@InstallIn(SingletonComponent::class)
object RenderModule {

    // Providers handled by bindings below
}

/**
 * Render bindings module
 *
 * Binds interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RenderBindings {

    @Binds
    @Singleton
    @OpenGLRender
    abstract fun bindOpenGLRenderEngine(impl: OpenGLRenderEngine): IRenderEngine

    @Binds
    @Singleton
    @MedianRender
    abstract fun bindSurfaceRenderEngine(impl: SurfaceRenderEngine): IRenderEngine
}
