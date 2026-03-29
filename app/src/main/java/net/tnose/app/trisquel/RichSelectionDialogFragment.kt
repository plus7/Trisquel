package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

data class RichSelectionDialogItem(var iconrsc: Int, var subject: String, var desc: String)

class RichSelectionDialogFragment: AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemsIcon = arguments?.getIntegerArrayList("items_icon") ?: arrayListOf()
        val itemsTitle = arguments?.getStringArray("items_title") ?: arrayOf()
        val itemsDesc = arguments?.getStringArray("items_desc") ?: arrayOf()
        val title = arguments?.getString("title") ?: ""
        val id = arguments?.getInt("id") ?: -1

        val items = itemsIcon.zip(itemsTitle).zip(itemsDesc) { a, b ->
            RichSelectionDialogItem(a.first, a.second, b)
        }

        val dialog = Dialog(requireContext())
        dialog.setContentView(ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
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
                                            .clickable {
                                                val data = Intent().apply {
                                                    putExtra("id", id)
                                                    putExtra("which", index)
                                                }
                                                notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                                                dismiss()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = item.iconrsc),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = item.subject,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (item.desc.isNotEmpty()) {
                                                Text(
                                                    text = item.desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }
            }
        })
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = true
        return dialog
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {
            return RichSelectionDialogFragment()
        }
    }
}
