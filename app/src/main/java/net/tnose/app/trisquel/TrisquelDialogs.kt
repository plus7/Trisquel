package net.tnose.app.trisquel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

sealed class ActiveDialog {
    data class Alert(val message: String) : ActiveDialog()
    data class Confirm(
        val title: String? = null,
        val message: String,
        val positive: String? = null,
        val negative: String? = null,
        val onConfirm: () -> Unit
    ) : ActiveDialog()
    data class SingleChoice(
        val title: String,
        val items: Array<String>,
        val selected: Int,
        val onConfirm: (Int) -> Unit
    ) : ActiveDialog()
    data class Select(
        val title: String? = null,
        val items: Array<String>,
        val ids: List<Int>? = null,
        val onSelected: (Int, Int?) -> Unit
    ) : ActiveDialog()
    data class RichSelection(
        val title: String,
        val icons: List<Int>,
        val titles: Array<String>,
        val descs: Array<String>,
        val onSelected: (Int) -> Unit
    ) : ActiveDialog()
    data class SearchCond(
        val title: String,
        val labels: Array<String>,
        val onSearch: (ArrayList<String>) -> Unit
    ) : ActiveDialog()
    data class Progress(
        val title: String,
        val percentage: Double,
        val status: String,
        val onCancel: () -> Unit
    ) : ActiveDialog()
}

@Composable
fun TrisquelDialogManager(
    activeDialog: ActiveDialog?,
    onDismiss: () -> Unit
) {
    if (activeDialog == null) return

    when (activeDialog) {
        is ActiveDialog.Alert -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
                },
                text = { Text(activeDialog.message) }
            )
        }
        is ActiveDialog.Confirm -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { activeDialog.title?.let { Text(it) } ?: Text("Trisquel") },
                text = { Text(activeDialog.message) },
                confirmButton = {
                    TextButton(onClick = { activeDialog.onConfirm(); onDismiss() }) {
                        Text(activeDialog.positive ?: stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(activeDialog.negative ?: stringResource(android.R.string.cancel))
                    }
                }
            )
        }
        is ActiveDialog.SingleChoice -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(activeDialog.title) },
                text = {
                    LazyColumn {
                        itemsIndexed(activeDialog.items) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeDialog.onConfirm(index); onDismiss() }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = index == activeDialog.selected, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(item)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                }
            )
        }
        is ActiveDialog.Select -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { activeDialog.title?.let { Text(it) } },
                text = {
                    LazyColumn {
                        itemsIndexed(activeDialog.items) { index, item ->
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeDialog.onSelected(index, activeDialog.ids?.get(index))
                                        onDismiss()
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                }
            )
        }
        is ActiveDialog.RichSelection -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(activeDialog.title) },
                text = {
                    LazyColumn {
                        itemsIndexed(activeDialog.titles) { index, title ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeDialog.onSelected(index); onDismiss() }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(painterResource(activeDialog.icons[index]), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text(activeDialog.descs[index], style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                }
            )
        }
        is ActiveDialog.SearchCond -> {
            val selectedLabels = remember { mutableStateListOf<String>() }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(activeDialog.title) },
                text = {
                    LazyColumn {
                        items(activeDialog.labels) { label ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedLabels.contains(label)) selectedLabels.remove(label)
                                        else selectedLabels.add(label)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = selectedLabels.contains(label), onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog.onSearch(ArrayList(selectedLabels))
                        onDismiss()
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                }
            )
        }
        is ActiveDialog.Progress -> {
            AlertDialog(
                onDismissRequest = {}, // Can't dismiss by clicking outside
                title = { Text(activeDialog.title) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (activeDialog.status.isNotEmpty()) {
                            Text(activeDialog.status, modifier = Modifier.padding(bottom = 16.dp))
                        }
                        LinearProgressIndicator(
                            progress = { (activeDialog.percentage / 100.0).toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { activeDialog.onCancel(); onDismiss() }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}
