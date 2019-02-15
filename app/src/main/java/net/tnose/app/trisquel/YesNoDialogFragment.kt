package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog

class YesNoDialogFragment : AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
                .setTitle(arguments?.getString("title", ""))
                .setMessage(arguments?.getString("message", ""))
                .setPositiveButton(arguments?.getString("positive", getString(android.R.string.yes))
                ) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id"))
                    notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                }
                .setNegativeButton(arguments?.getString("negative", getString(android.R.string.cancel))
                ) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id"))
                    notifyDialogResult(DialogInterface.BUTTON_NEGATIVE, data)
                }
                .setCancelable(true)
                .create()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return YesNoDialogFragment()
        }
    }
}
