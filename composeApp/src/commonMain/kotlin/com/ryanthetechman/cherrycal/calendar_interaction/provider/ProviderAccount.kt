package com.ryanthetechman.cherrycal.calendar_interaction.provider

import kotlinx.serialization.Serializable

@Serializable
data class ProviderAccount(
    @Serializable(with = AccountProviderRefSerializer::class)
    val accountProvider: AccountProvider,
    var identifier: String?, // e.g., email, account id
    var token: String? // e.g., access token
)