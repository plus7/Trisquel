package net.tnose.app.trisquel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ProgressReceiver(private val mActivity: MainActivity) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val percentage = intent.getDoubleExtra(ExportWorker.PARAM_PERCENTAGE, -1.0)
        val status = intent.getStringExtra(ExportWorker.PARAM_STATUS)
        val error = intent.getBooleanExtra(ExportWorker.PARAM_ERROR, false)

        mActivity.runOnUiThread {
            mActivity.setProgressPercentage(percentage, status!!, error)
        }
    }
}