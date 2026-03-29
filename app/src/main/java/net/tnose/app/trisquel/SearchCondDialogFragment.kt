package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class SearchCondDialogFragment : AbstractDialogFragment() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray("labels") ?: emptyArray()
        val title = arguments?.getString("title") ?: ""

        val dialog = Dialog(requireContext())
        dialog.setContentView(ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val checkedStates = remember {
                        mutableStateMapOf<String, Boolean>().apply {
                            items.forEach { put(it, false) }
                        }
                    }

                    AlertDialog(
                        onDismissRequest = {
                            notifyDialogCancelled()
                            dismiss()
                        },
                        title = if (title.isNotEmpty()) {
                            { Text(text = title) }
                        } else null,
                        text = {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (items.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.msg_error_no_tagged_items),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items.forEach { label ->
                                            val isChecked = checkedStates[label] ?: false
                                            FilterChip(
                                                selected = isChecked,
                                                onClick = {
                                                    checkedStates[label] = !isChecked
                                                },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val data = Intent().apply {
                                    val checkedLabels = items.filter { checkedStates[it] == true }
                                    putStringArrayListExtra("checked_labels", ArrayList(checkedLabels))
                                }
                                notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                                dismiss()
                            }) {
                                Text(stringResource(android.R.string.yes))
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
            return SearchCondDialogFragment()
        }
    }
}
