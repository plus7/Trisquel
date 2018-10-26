package net.tnose.app.trisquel

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import java.util.*

/**
 * Created by user on 2018/06/24.
 */

class CheckListDialogFragment : AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray("items") ?: arrayOf()
        val checked_indices = arguments?.getIntegerArrayList("checked_indices") ?: arrayListOf()

        val checkedItems = BooleanArray(items.size)
        for (i in checked_indices.indices) {
            checkedItems[checked_indices[i]] = true
        }

        return AlertDialog.Builder(context!!)
                .setTitle(arguments?.getString("title", "") ?: "")
                .setMultiChoiceItems(items, checkedItems) { dialog, which, isChecked -> checkedItems[which] = isChecked }
                .setPositiveButton(arguments?.getString("positive", getString(android.R.string.yes))
                ) { dialog, which ->
                    val data = Intent()
                    data.putExtra("id", arguments?.getInt("id"))
                    data.putExtra("items", arguments?.getStringArray("items"))
                    val strArray = ArrayList<String>()
                    val idxArray = ArrayList<Int>()
                    val tagArray = ArrayList<Int>()
                    for (i in checkedItems.indices) {
                        if (checkedItems[i]) {
                            strArray.add(arguments?.getStringArray("items")!![i])
                            idxArray.add(i)
                            if (arguments?.getIntegerArrayList("tags") != null) {
                                tagArray.add(arguments?.getIntegerArrayList("tags")!![i])
                            }
                        }
                    }
                    data.putExtra("checked_items", strArray)
                    data.putExtra("checked_indices", idxArray)
                    data.putExtra("checked_tags", tagArray)
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
            return CheckListDialogFragment()
        }
    }
}
