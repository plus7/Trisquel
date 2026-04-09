package net.tnose.app.trisquel

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    var showResetDialog by remember { mutableStateOf(false) }
    var autocompleteEnabled by remember {
        mutableStateOf(userPreferencesRepository.isAutocompleteFromPreviousShotEnabled())
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Trisquel") },
            text = { Text(stringResource(R.string.msg_reset_autocomplete)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    userPreferencesRepository.resetAutocompleteHistory()
                    Toast.makeText(context, context.getString(R.string.msg_reset_autocomplete_done), Toast.LENGTH_LONG).show()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_title_autocomplete_from_previous_shot)) },
                supportingContent = { Text(stringResource(R.string.pref_description_autocomplete_from_previous_shot)) },
                trailingContent = {
                    Switch(
                        checked = autocompleteEnabled,
                        onCheckedChange = { checked ->
                            autocompleteEnabled = checked
                            userPreferencesRepository.setAutocompleteFromPreviousShotEnabled(checked)
                        }
                    )
                },
                modifier = Modifier.clickable {
                    val newValue = !autocompleteEnabled
                    autocompleteEnabled = newValue
                    userPreferencesRepository.setAutocompleteFromPreviousShotEnabled(newValue)
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_title_reset_autocomplete_history)) },
                supportingContent = { Text(stringResource(R.string.pref_description_reset_autocomplete_history)) },
                modifier = Modifier.clickable {
                    showResetDialog = true
                }
            )
        }
    }
}
