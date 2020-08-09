package net.tnose.app.trisquel

/**
 * Based on https://github.com/nukka123/DialogFragmentDemo
 */

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.DatePicker
import java.util.*

class DatePickerFragment : AbstractDialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(context!!, this, year, month, dayOfMonth)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val data = Intent()
        data.putExtra(EXTRA_YEAR, year)
        data.putExtra(EXTRA_MONTH, month)
        data.putExtra(EXTRA_DAY, dayOfMonth)
        notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return DatePickerFragment()
        }
    }

    companion object {

        val EXTRA_YEAR = "year"
        val EXTRA_MONTH = "month"
        val EXTRA_DAY = "dayOfMonth"
    }
}