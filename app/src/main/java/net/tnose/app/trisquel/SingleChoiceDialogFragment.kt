package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog

/**
 * Created by user on 2018/06/24.
 */

class SingleChoiceDialogFragment : AbstractDialogFragment() {
    //final String[] items = {"item_0", "item_1", "item_2"};
    var choice = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray("items")
        val selected = arguments?.getInt("selected") ?: 0
        choice = selected
        return AlertDialog.Builder(context!!)
                .setTitle(arguments?.getString("title",""))
                .setSingleChoiceItems(items, selected) { dialog, which ->
                    choice = which
                }
                .setPositiveButton(arguments?.getString("positive", getString(android.R.string.yes))
                ) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id"))
                    data.putExtra("which", choice)
                    notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                }
                .setCancelable(true)
                .create()
        /*
        *
                .setSingleChoiceItems(items, selected) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id") ?: -1)
                    data.putExtra("which", which)
                    notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                }
        * */
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return SingleChoiceDialogFragment()
        }
    }
}
