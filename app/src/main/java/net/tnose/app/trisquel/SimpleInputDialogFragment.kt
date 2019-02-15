package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class SimpleInputDialogFragment : AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val frame = LayoutInflater.from(context).inflate(R.layout.dialog_text_input_layout, view as ViewGroup?, false)
        val layout = frame.findViewById(R.id.layout) as TextInputLayout
        layout.isHintEnabled = true
        layout.isHintAnimationEnabled = true
        layout.hint = arguments?.getString("hint", "")
        val input = frame.findViewById(R.id.input) as TextInputEditText
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(arguments?.getString("default_value", ""))
        val desc = frame.findViewById(R.id.desc) as TextView
        desc.text = arguments?.getString("message", "")
        if(desc.text.isEmpty()) desc.visibility = View.GONE
        return AlertDialog.Builder(activity!!)
                .setTitle(arguments?.getString("title", ""))
                .setView(frame)
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

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return SimpleInputDialogFragment()
        }
    }
}
