package net.tnose.app.trisquel

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class ProgressDialog : AbstractDialogFragment() {
    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null){
                if(action == MainActivity.ACTION_CLOSE_PROGRESS_DIALOG) {
                    val data = Intent()
                    notifyDialogResult(DialogInterface.BUTTON_NEUTRAL, data)
                    closeDialog()
                }else if(action == MainActivity.ACTION_UPDATE_PROGRESS_DIALOG){
                    val percentage = intent.getDoubleExtra("percentage", 0.0)
                    val status = intent.getStringExtra("status")
                    setPercentage(percentage, status!!)
                }
            }
        }
    }

    private fun closeDialog() {
        dismissAllowingStateLoss()
    }

    private fun setPercentage(percentage: Double, status: String){
        val pb = dialog?.findViewById<ProgressBar>(R.id.progressBar)
        pb?.progress = (10.0*percentage).toInt()
        val tv = dialog?.findViewById<TextView>(R.id.textView)
        tv?.text = "%5.2f%%: %s".format(percentage, status)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(MainActivity.ACTION_CLOSE_PROGRESS_DIALOG)
        intentFilter.addAction(MainActivity.ACTION_UPDATE_PROGRESS_DIALOG)
        activity!!.registerReceiver(br, intentFilter, RECEIVER_EXPORTED)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val frame = LayoutInflater.from(context).inflate(R.layout.dialog_progress, view as ViewGroup?, false)
        val btnCancel = frame.findViewById<Button>(R.id.btnCancel)
        val isCancellableByBtn = arguments?.getBoolean("cancellable", true) ?: true
        btnCancel.setOnClickListener {
            val data = Intent()
            notifyDialogResult(DialogInterface.BUTTON_NEGATIVE, data)
            closeDialog()
        }
        btnCancel.isEnabled = isCancellableByBtn
        btnCancel.visibility = if(isCancellableByBtn) View.VISIBLE else View.GONE
        val dialog = AlertDialog.Builder(activity!!)
                .setView(frame)
                .setTitle(arguments?.getString("title", "") ?: "")
                .create()
        isCancelable = false
        return dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        activity!!.unregisterReceiver(br)
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return ProgressDialog()
        }
    }
}
