package com.ryanthetechman.cherrycal.calendar_interaction.provider

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AccountData(val accounts: MutableList<ProviderAccount> = mutableListOf())

actual object AccountStorage {
    private val mutex = Mutex()
    private val storageFile = File(System.getProperty("user.home"), ".cherrycal-accounts.json")
    private var accountData: AccountData = loadAccounts()

    private fun loadAccounts(): AccountData {
        println("Loading accounts from file ${storageFile.absolutePath}")
        return if (storageFile.exists()) {
            try {
                Json.decodeFromString(storageFile.readText())
            } catch (e: Exception) {
                println("Failed to load accounts: ${e.message}")
                AccountData()
            }
        } else {
            AccountData()
        }
    }

    private fun saveToFile() {
        storageFile.writeText(Json.encodeToString(accountData))
        println("Saved accounts to file ${storageFile.absolutePath}")
    }

    actual suspend fun saveAccount(account: ProviderAccount): ProviderAccount {
        mutex.withLock {
            // Update account if one with the same provider and identifier exists.
            val index = accountData.accounts.indexOfFirst {
                it.accountProvider == account.accountProvider && it.identifier == account.identifier
            }
            if (index >= 0) {
                accountData.accounts[index] = account
            } else {
                accountData.accounts.add(account)
            }
            saveToFile()
            return account
        }
    }

    actual suspend fun getAccount(accountProvider: AccountProvider?, accountIdentifier: String): ProviderAccount? {
        return mutex.withLock {
            accountData.accounts.firstOrNull {
                if (accountProvider == null) return@firstOrNull it.identifier == accountIdentifier
                it.identifier == accountIdentifier && it.accountProvider == accountProvider
            }
        }
    }

    actual suspend fun removeAccount(account: ProviderAccount) {
        mutex.withLock {
            accountData.accounts.removeIf {
                it.accountProvider == account.accountProvider && it.identifier == account.identifier
            }
            saveToFile()
        }
    }

    actual suspend fun getAllAccounts(): List<ProviderAccount> = mutex.withLock {
        accountData.accounts.toList()
    }
}