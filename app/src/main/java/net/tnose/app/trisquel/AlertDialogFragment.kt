package net.tnose.app.trisquel

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog

class AlertDialogFragment : AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle(arguments?.getString("title", ""))
                .setMessage(arguments?.getString("message", ""))
                .setPositiveButton(android.R.string.yes
                ) { dialog, which -> }
                .setCancelable(true)
                .create()
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return AlertDialogFragment()
        }
    }
}
