package com.budgetapp.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CHECKING") }
    var balance by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var plaidAccessToken by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var showAdvancedToken by remember { mutableStateOf(false) }

    val accountTypes = listOf("CHECKING", "SAVINGS", "CREDIT", "CASH")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name") },
                placeholder = { Text("e.g. Chase Checking") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = type.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    accountTypes.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { type = t; typeExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                label = { Text("Current Balance") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Institution (optional)") },
                placeholder = { Text("e.g. Chase Bank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Plaid Bank Sync Section ─────────────────────────────────
            if (uiState.hasPlaidCredentials) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Connect via Plaid",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Automatically connect a sandbox test bank account. Transactions and balance will be synced from Plaid.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.connectSandboxBank(
                                    name = name.ifBlank { "Sandbox Account" },
                                    type = type
                                )
                            },
                            enabled = !uiState.isConnectingSandbox,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isConnectingSandbox) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Connect Sandbox Bank")
                            }
                        }

                        uiState.syncMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.contains("failed", ignoreCase = true))
                                    MaterialTheme.colorScheme.error
                                else Color(0xFF4CAF50)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAdvancedToken = !showAdvancedToken }) {
                            Text(if (showAdvancedToken) "Hide advanced" else "Advanced: Enter token manually")
                        }
                    }
                }
            }

            // Show manual token field if no credentials or advanced mode
            AnimatedVisibility(visible = !uiState.hasPlaidCredentials || showAdvancedToken) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = plaidAccessToken,
                        onValueChange = { plaidAccessToken = it },
                        label = { Text("Plaid Access Token (optional)") },
                        placeholder = { Text("access-sandbox-xxxxxxxx") },
                        supportingText = {
                            Text(
                                if (!uiState.hasPlaidCredentials)
                                    "Set up Plaid credentials in Settings first, or paste a token here."
                                else
                                    "Paste a Plaid access token if you already have one.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    viewModel.addAccount(
                        name = name,
                        type = type,
                        balance = balance.toDoubleOrNull() ?: 0.0,
                        institution = institution.ifBlank { null },
                        plaidAccessToken = plaidAccessToken.ifBlank { null }
                    )
                    onNavigateBack()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Manual Account")
            }
        }
    }
}
