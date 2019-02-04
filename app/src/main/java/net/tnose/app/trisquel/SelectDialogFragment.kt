package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog

/**
 * Created by user on 2018/06/24.
 */

class SelectDialogFragment : AbstractDialogFragment() {
    //final String[] items = {"item_0", "item_1", "item_2"};

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray("items")
        val ids = arguments?.getIntegerArrayList("ids")
        return AlertDialog.Builder(context!!)
                //.setTitle(getArguments().getString("title",""))
                .setItems(items) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id") ?: -1)
                    data.putExtra("which", which)
                    data.putExtra("which_id", ids?.get(which) ?: -1)
                    data.putExtra("which_str", items?.get(which) ?: "")
                    notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                }
                .setCancelable(true)
                .create()
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return SelectDialogFragment()
        }
    }
}
