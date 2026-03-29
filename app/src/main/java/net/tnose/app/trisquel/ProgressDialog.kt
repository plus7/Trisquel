package net.tnose.app.trisquel

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class ProgressDialog : AbstractDialogFragment() {
    private var progressState by mutableStateOf(0.0)
    private var statusState by mutableStateOf("")

    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null){
                if(action == MainActivity.ACTION_CLOSE_PROGRESS_DIALOG) {
                    val data = Intent()
                    notifyDialogResult(DialogInterface.BUTTON_NEUTRAL, data)
                    closeDialog()
                }else if(action == MainActivity.ACTION_UPDATE_PROGRESS_DIALOG){
                    progressState = intent.getDoubleExtra("percentage", 0.0)
                    statusState = intent.getStringExtra("status") ?: ""
                }
            }
        }
    }

    private fun closeDialog() {
        dismissAllowingStateLoss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(MainActivity.ACTION_CLOSE_PROGRESS_DIALOG)
        intentFilter.addAction(MainActivity.ACTION_UPDATE_PROGRESS_DIALOG)
        requireActivity().registerReceiver(br, intentFilter, RECEIVER_EXPORTED)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isCancellableByBtn = arguments?.getBoolean("cancellable", true) ?: true
        val title = arguments?.getString("title", "") ?: ""

        val dialog = Dialog(requireContext())
        dialog.setContentView(ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AlertDialog(
                        onDismissRequest = {
                            if (isCancellableByBtn) {
                                val data = Intent()
                                notifyDialogResult(DialogInterface.BUTTON_NEGATIVE, data)
                                closeDialog()
                            }
                        },
                        title = if (title.isNotEmpty()) {
                            { Text(text = title) }
                        } else null,
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    text = "%5.2f%%: %s".format(progressState, statusState),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { (progressState / 100.0).toFloat() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                        confirmButton = {
                            if (isCancellableByBtn) {
                                TextButton(onClick = {
                                    val data = Intent()
                                    notifyDialogResult(DialogInterface.BUTTON_NEGATIVE, data)
                                    closeDialog()
                                }) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                            }
                        },
                        dismissButton = {}
                    )
                }
            }
        })
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(br)
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {
            return ProgressDialog()
        }
    }
}
