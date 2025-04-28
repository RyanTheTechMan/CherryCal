package com.ryanthetechman.cherrycal.calendar_interaction.provider

expect object AccountStorage {
     /**
      * Saves the account to persistent storage.
      * NOTE: Should account for if a provider already has an account with the same identifier and remove and save the new one
      * Returns the saved account.
      */
     suspend fun saveAccount(account: ProviderAccount): ProviderAccount

     /**
      * Returns the account from storage, or null if it doesn't exist.
      */
     suspend fun getAccount(accountProvider: AccountProvider? = null, accountIdentifier: String): ProviderAccount?

     /**
      * Removes the account from storage, if it exists.
      */
     suspend fun removeAccount(account: ProviderAccount)

     /**
      * Returns all accounts in persistent storage.
      */
     suspend fun getAllAccounts(): List<ProviderAccount>
}