package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class SingleChoiceDialogFragment : AbstractDialogFragment() {
    var choice = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray("items") ?: emptyArray()
        val selected = arguments?.getInt("selected") ?: 0
        val title = arguments?.getString("title") ?: ""
        val positiveText = arguments?.getString("positive", getString(android.R.string.yes))
        val id = arguments?.getInt("id") ?: -1

        val dialog = Dialog(requireContext())
        dialog.setContentView(ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    var currentSelection by remember { mutableStateOf(selected) }
                    
                    AlertDialog(
                        onDismissRequest = {
                            notifyDialogCancelled()
                            dismiss()
                        },
                        title = if (title.isNotEmpty()) {
                            { Text(text = title) }
                        } else null,
                        text = {
                            LazyColumn {
                                itemsIndexed(items) { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { currentSelection = index }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (currentSelection == index),
                                            onClick = { currentSelection = index }
                                        )
                                        Text(
                                            text = item,
                                            modifier = Modifier.padding(start = 8.dp),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                choice = currentSelection
                                val data = Intent().apply {
                                    putExtra("id", id)
                                    putExtra("which", choice)
                                }
                                notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                                dismiss()
                            }) {
                                Text(positiveText ?: stringResource(android.R.string.yes))
                            }
                        },
                        dismissButton = {}
                    )
                }
            }
        })
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = true
        return dialog
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {
            return SingleChoiceDialogFragment()
        }
    }
}
