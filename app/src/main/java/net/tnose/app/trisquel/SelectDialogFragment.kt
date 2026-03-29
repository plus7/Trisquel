package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

class SelectDialogFragment : AbstractDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray("items") ?: emptyArray()
        val ids = arguments?.getIntegerArrayList("ids")
        val id = arguments?.getInt("id") ?: -1

        val dialog = Dialog(requireContext())
        dialog.setContentView(ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AlertDialog(
                        onDismissRequest = {
                            notifyDialogCancelled()
                            dismiss()
                        },
                        title = arguments?.getString("title")?.let {
                            if (it.isNotEmpty()) {
                                { Text(text = it) }
                            } else null
                        },
                        text = {
                            LazyColumn {
                                itemsIndexed(items) { index, item ->
                                    Text(
                                        text = item,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val data = Intent().apply {
                                                    putExtra("id", id)
                                                    putExtra("which", index)
                                                    putExtra("which_id", ids?.get(index) ?: -1)
                                                    putExtra("which_str", item)
                                                }
                                                notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                                                dismiss()
                                            }
                                            .padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
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

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {
            return SelectDialogFragment()
        }
    }
}
