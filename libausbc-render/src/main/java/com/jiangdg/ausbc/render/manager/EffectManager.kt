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
package com.jiangdg.ausbc.render.manager

import com.jiangdg.ausbc.core.contract.RenderEffect as CoreRenderEffect
import com.jiangdg.ausbc.render.effect.EffectType
import com.jiangdg.ausbc.render.effect.RenderEffect
import com.jiangdg.ausbc.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedHashSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Effect Manager
 *
 * Manages OpenGL render effects.
 * Effects are applied in a chain in the order they were added.
 *
 * @author Created for restructuring plan
 */
@Singleton
class EffectManager @Inject constructor() {

    private val mutex = Mutex()
    private val effectChain = LinkedHashSet<RenderEffect>()

    private val _activeEffects = MutableStateFlow<List<RenderEffect>>(emptyList())
    val activeEffects: StateFlow<List<RenderEffect>> = _activeEffects

    /**
     * Add an effect to the effect chain
     */
    suspend fun addEffect(effect: RenderEffect) = mutex.withLock {
        if (effectChain.add(effect)) {
            _activeEffects.value = effectChain.toList()
            Logger.i("EffectManager", "Effect added: ${effect.javaClass.simpleName}")
            true
        } else {
            Logger.w("EffectManager", "Effect already exists: ${effect.javaClass.simpleName}")
            false
        }
    }

    /**
     * Add a core RenderEffect to the effect chain
     */
    suspend fun addEffect(effect: CoreRenderEffect) = mutex.withLock {
        // Only add if it's a render module RenderEffect
        if (effect is RenderEffect) {
            addEffect(effect)
        } else {
            Logger.w("EffectManager", "Core RenderEffect not supported: ${effect.javaClass.simpleName}")
            false
        }
    }

    /**
     * Remove an effect from the effect chain
     */
    suspend fun removeEffect(effect: RenderEffect) = mutex.withLock {
        if (effectChain.remove(effect)) {
            effect.release()
            _activeEffects.value = effectChain.toList()
            Logger.i("EffectManager", "Effect removed: ${effect.javaClass.simpleName}")
            true
        } else {
            Logger.w("EffectManager", "Effect not found: ${effect.javaClass.simpleName}")
            false
        }
    }

    /**
     * Remove effect by type
     */
    suspend fun removeEffectByType(effectType: EffectType) = mutex.withLock {
        val removed = effectChain.filter { it.effectType == effectType }
        removed.forEach { effect ->
            effectChain.remove(effect)
            effect.release()
        }
        if (removed.isNotEmpty()) {
            _activeEffects.value = effectChain.toList()
            Logger.i("EffectManager", "Removed ${removed.size} effects of type: $effectType")
            true
        } else {
            false
        }
    }

    /**
     * Remove effect by ID (string)
     */
    suspend fun removeEffectByType(effectId: String) = mutex.withLock {
        val removed = effectChain.filter { it.effectId == effectId }
        removed.forEach { effect ->
            effectChain.remove(effect)
            effect.release()
        }
        if (removed.isNotEmpty()) {
            _activeEffects.value = effectChain.toList()
            Logger.i("EffectManager", "Removed ${removed.size} effects with ID: $effectId")
            true
        } else {
            false
        }
    }

    /**
     * Update effect by ID
     */
    suspend fun updateEffect(effectId: String, newEffect: RenderEffect) = mutex.withLock {
        val existing = effectChain.firstOrNull { it.effectId == effectId }
        if (existing != null) {
            effectChain.remove(existing)
            existing.release()
            effectChain.add(newEffect)
            _activeEffects.value = effectChain.toList()
            Logger.i("EffectManager", "Effect updated: $effectId")
            true
        } else {
            Logger.w("EffectManager", "Effect not found for update: $effectId")
            false
        }
    }

    /**
     * Update effect by ID (core interface)
     */
    suspend fun updateEffect(effectId: String, newEffect: CoreRenderEffect) = mutex.withLock {
        if (newEffect is RenderEffect) {
            updateEffect(effectId, newEffect)
        } else {
            Logger.w("EffectManager", "Core RenderEffect not supported: ${newEffect.javaClass.simpleName}")
            false
        }
    }

    /**
     * Remove all effects
     */
    suspend fun removeAll() = mutex.withLock {
        effectChain.forEach { it.release() }
        effectChain.clear()
        _activeEffects.value = emptyList()
        Logger.i("EffectManager", "All effects removed")
    }

    /**
     * Release all effects
     */
    suspend fun releaseAll() = mutex.withLock {
        effectChain.forEach { effect ->
            try {
                effect.release()
            } catch (e: Exception) {
                Logger.e("EffectManager", "Error releasing effect: ${effect.javaClass.simpleName}", e)
            }
        }
        effectChain.clear()
        _activeEffects.value = emptyList()
        Logger.i("EffectManager", "All effects released")
    }

    /**
     * Get all active effects
     */
    fun getActiveEffects(): List<RenderEffect> = effectChain.toList()

    /**
     * Get effect by type
     */
    fun getEffectByType(effectType: EffectType): RenderEffect? {
        return effectChain.firstOrNull { it.effectType == effectType }
    }

    /**
     * Get effect by ID
     */
    fun getEffectById(effectId: String): RenderEffect? {
        return effectChain.firstOrNull { it.effectId == effectId }
    }

    /**
     * Check if effect is active
     */
    fun hasEffect(effect: RenderEffect): Boolean = effectChain.contains(effect)

    /**
     * Check if effect type is active
     */
    fun hasEffectType(effectType: EffectType): Boolean {
        return effectChain.any { it.effectType == effectType }
    }

    /**
     * Get effect count
     */
    fun getEffectCount(): Int = effectChain.size

    /**
     * Check if effect chain is empty
     */
    fun isEmpty(): Boolean = effectChain.isEmpty()

    /**
     * Enable or disable an effect
     */
    suspend fun setEffectEnabled(effect: RenderEffect, enabled: Boolean) = mutex.withLock {
        if (effectChain.contains(effect)) {
            effect.setEffectEnabled(enabled)
            Logger.i("EffectManager", "Effect ${effect.javaClass.simpleName} ${if (enabled) "enabled" else "disabled"}")
            true
        } else {
            false
        }
    }

    /**
     * Update effect parameters
     */
    suspend fun <T : Any> updateEffectParameter(
        effect: RenderEffect,
        parameter: String,
        value: T
    ) = mutex.withLock {
        if (effectChain.contains(effect)) {
            effect.setParameter(parameter, value)
            true
        } else {
            false
        }
    }

    /**
     * Get effect parameters
     */
    fun <T : Any> getEffectParameter(
        effect: RenderEffect,
        parameter: String
    ): T? {
        return if (effectChain.contains(effect)) {
            effect.getParameter<T>(parameter)
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "EffectManager"
    }
}
