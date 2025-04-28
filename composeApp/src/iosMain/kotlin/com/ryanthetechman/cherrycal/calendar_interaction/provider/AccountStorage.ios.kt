package com.ryanthetechman.cherrycal.calendar_interaction.provider

actual object AccountStorage {
    /**
     * Saves the account to persistent storage.
     * NOTE: Should account for if a provider already has an account with the same identifier and remove and save the new one
     * Returns the saved account.
     */
    actual suspend fun saveAccount(account: ProviderAccount): ProviderAccount {
        TODO("Not yet implemented")
    }

    /**
     * Returns the account from storage, or null if it doesn't exist.
     */
    actual suspend fun getAccount(accountProvider: AccountProvider?, accountIdentifier: String): ProviderAccount? {
        TODO("Not yet implemented")
    }

    /**
     * Removes the account from storage, if it exists.
     */
    actual suspend fun removeAccount(account: ProviderAccount) {
    }

    /**
     * Returns all accounts in persistent storage.
     */
    actual suspend fun getAllAccounts(): List<ProviderAccount> {
        TODO("Not yet implemented")
    }

}