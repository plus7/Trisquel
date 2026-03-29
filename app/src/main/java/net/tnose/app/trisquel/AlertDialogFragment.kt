package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource

class AlertDialogFragment : AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString("title", "") ?: ""
        val message = arguments?.getString("message", "") ?: ""

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
                        text = if (message.isNotEmpty()) {
                            { Text(text = message) }
                        } else null,
                        confirmButton = {
                            TextButton(onClick = {
                                val data = Intent()
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

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {
            return AlertDialogFragment()
        }
    }
}
