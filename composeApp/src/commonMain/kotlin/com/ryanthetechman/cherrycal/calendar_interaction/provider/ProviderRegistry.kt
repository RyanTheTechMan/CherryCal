package com.ryanthetechman.cherrycal.calendar_interaction.provider

import kotlin.reflect.KClass

object ProviderRegistry {
    private val providers = mutableMapOf<KClass<out AccountProvider>, AccountProvider>()

    fun <T : AccountProvider> register(provider: T) {
        providers[provider::class] = provider
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : AccountProvider> get(providerClass: KClass<T>): T? {
        return providers[providerClass] as? T
    }

    fun all(): List<AccountProvider> {
        return providers.values.toList()
    }
}