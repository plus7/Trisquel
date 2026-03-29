package net.tnose.app.trisquel

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        setContent {
            MaterialTheme {
                SettingsScreen(sharedPreferences)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen(sharedPreferences: SharedPreferences) {
        var showResetDialog by remember { mutableStateOf(false) }
        var autocompleteEnabled by remember {
            mutableStateOf(sharedPreferences.getBoolean("autocomplete_from_previous_shot", false))
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Trisquel") },
                text = { Text(stringResource(R.string.msg_reset_autocomplete)) },
                confirmButton = {
                    TextButton(onClick = {
                        sharedPreferences.edit {
                            putString("lens_manufacturer", "[]")
                            putString("camera_manufacturer", "[]")
                            putString("camera_mounts", "[]")
                            putString("film_manufacturer", "[]")
                            putString("film_brand", "{}")
                        }
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_reset_autocomplete_done), Toast.LENGTH_LONG).show()
                        showResetDialog = false
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
                        IconButton(onClick = { finish() }) {
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
                                sharedPreferences.edit { putBoolean("autocomplete_from_previous_shot", checked) }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        val newValue = !autocompleteEnabled
                        autocompleteEnabled = newValue
                        sharedPreferences.edit { putBoolean("autocomplete_from_previous_shot", newValue) }
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
}
