package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView


class ShutterSpeedCustomizeDialogFragment : AbstractDialogFragment() {
    var mydialog: AlertDialog? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val frame = LayoutInflater.from(context).inflate(R.layout.dialog_shutter_speed_customize, view as ViewGroup?, false)

        val multi = frame.findViewById(R.id.editText2) as EditText
        multi.setText(arguments?.getString("default_value", ""))
        multi.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val spans = s.getSpans(0, s.length, ForegroundColorSpan::class.java)
                for(span in spans){
                    s.removeSpan(span)
                }
                val list = s.split("\n")
                var start = 0
                var okButtonEnabled = s.isNotEmpty()
                for(line in list){
                    val r = Regex("(1/\\d+|\\d+\\.\\d+|\\d+)")
                    if(!line.matches(r)){
                        val fspan = ForegroundColorSpan(Color.RED)
                        s.setSpan(fspan, start, start+line.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        okButtonEnabled = false
                    }
                    start += line.length+1
                }
                val b = mydialog?.getButton(AlertDialog.BUTTON_POSITIVE)
                b?.isEnabled = okButtonEnabled
            }
        })

        val desc = frame.findViewById(R.id.desc) as TextView
        desc.text = arguments?.getString("message", "")
        if(desc.text.isEmpty()) desc.visibility = View.GONE

        mydialog = AlertDialog.Builder(activity!!)
                .setTitle(arguments?.getString("title", ""))
                .setView(frame)
                .setPositiveButton(android.R.string.yes
                ) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id"))
                    data.putExtra("which", which)
                    data.putExtra("value", multi.text.toString())
                    notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
                }
                .setCancelable(true)
                .create()
        return mydialog!!
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return ShutterSpeedCustomizeDialogFragment()
        }
    }
}
