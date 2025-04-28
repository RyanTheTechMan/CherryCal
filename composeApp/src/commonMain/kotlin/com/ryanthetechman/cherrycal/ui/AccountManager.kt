package com.ryanthetechman.cherrycal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountProvider
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderAccount

@Composable
fun AccountManagerUI(
    linkedAccounts: List<ProviderAccount>,
    selectedAccount: ProviderAccount?,
    onRemoveAccount: (ProviderAccount) -> Unit,
    onSelectAccount: (ProviderAccount) -> Unit,
    onAddAccountClicked: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
        Text(text = "Account Manager", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(linkedAccounts) { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAccount(account) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nameLabel = buildString {
                        append(account.accountProvider.providerName)
                        append(": ")
                        append(account.identifier ?: "N/A")
                    }
                    Text(text = nameLabel, modifier = Modifier.weight(1f))
                    if (selectedAccount == account) {
                        Text(" (Active)", style = MaterialTheme.typography.caption)
                    }
                    IconButton(onClick = { onRemoveAccount(account) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove account")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onAddAccountClicked) {
            Text("Add Account")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddAccountDialog(
    providers: List<AccountProvider>,
    providerToAdd: AccountProvider?,
    onProviderSelected: (AccountProvider?) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add New Provider Account") },
        text = {
            Column {
                Text("Choose a provider to add:")
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = providerToAdd?.providerName ?: "Select Provider",
                        onValueChange = { /* no-op */ },
                        label = { Text("Provider") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                onClick = {
                                    onProviderSelected(provider)
                                    dropdownExpanded = false
                                }
                            ) {
                                Text(
                                    text = provider.providerName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onConfirm,
                    enabled = providerToAdd != null
                ) {
                    Text("OK")
                }
            }
        }
    )
}