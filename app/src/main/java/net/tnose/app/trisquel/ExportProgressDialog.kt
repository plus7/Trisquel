package net.tnose.app.trisquel

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

class ExportProgressDialog : AbstractDialogFragment() {
    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null){
                if(action == MainActivity.ACTION_CLOSE_PROGRESS_DIALOG) {
                    closeDialog()
                }else if(action == MainActivity.ACTION_UPDATE_PROGRESS_DIALOG){
                    val percentage = intent.getDoubleExtra("percentage", 0.0)
                    val status = intent.getStringExtra("status")
                    setPercentage(percentage, status)
                }
            }
        }
    }

    private fun closeDialog() {
        dismissAllowingStateLoss()
    }

    private fun setPercentage(percentage: Double, status: String){
        val pb = dialog.findViewById<ProgressBar>(R.id.progressBar)
        pb.progress = (10.0*percentage).toInt()
        val tv = dialog.findViewById<TextView>(R.id.textView)
        tv.text = "%5.2f%%: %s".format(percentage, status)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(MainActivity.ACTION_CLOSE_PROGRESS_DIALOG)
        intentFilter.addAction(MainActivity.ACTION_UPDATE_PROGRESS_DIALOG)
        activity!!.registerReceiver(br, intentFilter)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val frame = LayoutInflater.from(context).inflate(R.layout.dialog_export_progress, view as ViewGroup?, false)
        val dialog = AlertDialog.Builder(activity!!)
                .setView(frame)
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
            return ExportProgressDialog()
        }
    }
}
