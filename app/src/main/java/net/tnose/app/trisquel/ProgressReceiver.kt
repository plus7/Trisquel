package net.tnose.app.trisquel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ProgressReceiver(private val mActivity: MainActivity) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val percentage = intent.getDoubleExtra(ExportIntentService.PARAM_PERCENTAGE, -1.0)
        val status = intent.getStringExtra(ExportIntentService.PARAM_STATUS)
        val error = intent.getBooleanExtra(DbConvIntentService.PARAM_ERROR, false)

        mActivity.runOnUiThread {
            mActivity.setProgressPercentage(percentage, status!!, error)
        }
    }
}