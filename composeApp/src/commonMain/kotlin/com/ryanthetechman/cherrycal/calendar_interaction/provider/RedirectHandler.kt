package com.ryanthetechman.cherrycal.calendar_interaction.provider

/**
 * Waits for the OAuth redirect and returns the authorization code.
 */
expect suspend fun waitForRedirect(): String