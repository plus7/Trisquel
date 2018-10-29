package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText

class SimpleInputDialogFragment : AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val input = EditText(activity!!)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(arguments?.getString("default_value", ""))
        return AlertDialog.Builder(activity!!)
                .setTitle(arguments?.getString("title", ""))
                .setView(input)
                .setPositiveButton(android.R.string.yes
                ) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id"))
                    data.putExtra("which", which)
                    data.putExtra("value", input.text.toString())
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
            return SimpleInputDialogFragment()
        }
    }
}
