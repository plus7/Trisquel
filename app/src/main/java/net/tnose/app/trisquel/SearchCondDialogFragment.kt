package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.chip.Chip
import android.support.design.chip.ChipGroup
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class SearchCondDialogFragment : AbstractDialogFragment() {
    var labels: Array<String>? = null
    var checkState: Array<Boolean>? = null

    fun createNewChip(label: String, chipGroup: ChipGroup): Chip {
        val newchip = Chip(activity)
        newchip.text = label
        val chipLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        newchip.layoutParams = chipLayoutParams
        newchip.isCheckable = true
        newchip.setOnCheckedChangeListener{
            buttonView, isChecked ->
            val i = labels?.indexOf(label) ?: -1
            if(i >= 0) {
                checkState!![i] = isChecked
            }
        }
        chipGroup.addView(newchip)
        return newchip
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val frame = LayoutInflater.from(context).inflate(R.layout.dialog_search_cond, view as ViewGroup?, false)
        val items = arguments?.getStringArray("labels") ?: arrayOf()
        labels = items
        if(items.size == 0){
            val textview = frame.findViewById<TextView>(R.id.errorMsg)
            textview.visibility = View.VISIBLE
            textview.text = getString(R.string.msg_error_no_tagged_items)
        }
        checkState = Array<Boolean>(items.size, {i -> false})
        val chip_group = frame.findViewById<ChipGroup>(R.id.chip_group)
        for(label in items) createNewChip(label, chip_group)
        return AlertDialog.Builder(context!!)
                .setTitle(arguments?.getString("title","") ?: "")
                .setView(frame)
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                    val data = Intent()
                    data.putExtra("which", which)
                    val checkedLabels = ArrayList<String>()
                    for((i,v) in checkState!!.withIndex()){
                        if(v) checkedLabels.add(labels!![i])
                    }
                    data.putExtra("checked_labels", checkedLabels)
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
            return SearchCondDialogFragment()
        }
    }
}
