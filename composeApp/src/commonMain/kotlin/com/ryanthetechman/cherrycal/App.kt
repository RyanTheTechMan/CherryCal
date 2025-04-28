package com.ryanthetechman.cherrycal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ryanthetechman.cherrycal.calendar_interaction.Calendar
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountProvider
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountStorage
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderAccount
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderRegistry
import com.ryanthetechman.cherrycal.calendar_interaction.provider.google.GoogleAccountProvider
import com.ryanthetechman.cherrycal.ui.AccountManagerUI
import com.ryanthetechman.cherrycal.ui.AddAccountDialog
import com.ryanthetechman.cherrycal.ui.CalendarScreen
import kotlinx.coroutines.launch

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()

    var linkedAccounts by remember { mutableStateOf<List<ProviderAccount>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<ProviderAccount?>(null) }
    var currentProvider by remember { mutableStateOf<AccountProvider?>(null) }
    var selectedCalendar by remember { mutableStateOf<Calendar?>(null) }

    // State to control showing the Add Account Dialog.
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var providerToAdd by remember { mutableStateOf<AccountProvider?>(null) }

    // While we are loading the account (or calendars), show a spinner.
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        ProviderRegistry.register(GoogleAccountProvider)
    }

    LaunchedEffect(Unit) {
        linkedAccounts = AccountStorage.getAllAccounts()
        if (linkedAccounts.isNotEmpty()) {
            selectedAccount = linkedAccounts.first()
            currentProvider = ProviderRegistry.get(selectedAccount!!.accountProvider::class)
            currentProvider?.getCalendars(selectedAccount!!)?.onSuccess { calendars ->
                selectedCalendar = calendars.firstOrNull()
            }
        }
        loading = false
    }

    MaterialTheme {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (selectedAccount == null || currentProvider == null || selectedCalendar == null) {
            AccountManagerUI(
                linkedAccounts = linkedAccounts,
                selectedAccount = selectedAccount,
                onRemoveAccount = { accountToRemove ->
                    coroutineScope.launch {
                        AccountStorage.removeAccount(accountToRemove)
                        // Refresh accounts.
                        linkedAccounts = AccountStorage.getAllAccounts()
                        if (selectedAccount == accountToRemove) {
                            selectedAccount = linkedAccounts.firstOrNull()
                            currentProvider = selectedAccount?.let { ProviderRegistry.get(it.accountProvider::class) }
                            selectedCalendar = null
                            // Fetch calendars for the new account.
                            selectedAccount?.let { acct ->
                                currentProvider?.getCalendars(acct)?.onSuccess { calendars ->
                                    selectedCalendar = calendars.firstOrNull()
                                }
                            }
                        }
                    }
                },
                onSelectAccount = { account ->
                    selectedAccount = account
                    currentProvider = ProviderRegistry.get(account.accountProvider::class)
                    coroutineScope.launch {
                        currentProvider?.getCalendars(account)?.onSuccess { calendars ->
                            selectedCalendar = calendars.firstOrNull()
                        }
                    }
                },
                onAddAccountClicked = { showAddAccountDialog = true }
            )

            if (showAddAccountDialog) {
                AddAccountDialog(
                    providers = ProviderRegistry.all(),
                    providerToAdd = providerToAdd,
                    onProviderSelected = { providerToAdd = it },
                    onCancel = { showAddAccountDialog = false },
                    onConfirm = {
                        showAddAccountDialog = false
                        providerToAdd?.let { chosenProvider ->
                            coroutineScope.launch {
                                val account = if (chosenProvider is GoogleAccountProvider) {
                                    GoogleAccountProvider.configureAccount()
                                } else {
                                    println("Unsupported provider.")
                                    null
                                }
                                if (account == null) {
                                    println("Account failed.")
                                } else {
                                    linkedAccounts = AccountStorage.getAllAccounts()
                                    selectedAccount = account
                                    currentProvider = ProviderRegistry.get(account.accountProvider::class)
                                    currentProvider?.getCalendars(account)?.onSuccess { calendars ->
                                        selectedCalendar = calendars.firstOrNull()
                                    }
                                }
                            }
                        }
                    }
                )
            }
        } else {
            CalendarScreen(
                selectedAccount = selectedAccount,
                currentProvider = currentProvider,
                selectedCalendar = selectedCalendar
            )
        }
    }
}