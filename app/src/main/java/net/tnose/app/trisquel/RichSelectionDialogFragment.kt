package net.tnose.app.trisquel
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

data class RichSelectionDialogItem(var iconrsc: Int, var subject: String, var desc: String)

class RichSelectionDialogItemAdapter(context: Context, resource: Int, items: List<RichSelectionDialogItem>) : ArrayAdapter<RichSelectionDialogItem>(context, resource, items) {
    var inflater: LayoutInflater

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, v: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val myview =
                if (null == v) inflater.inflate(R.layout.fragment_selection_dialog_item, null)
                else v
        val titleview: TextView = myview.findViewById(R.id.textview_title) as TextView
        titleview.text = item?.subject ?: ""
        val descview: TextView = myview.findViewById(R.id.textview_description) as TextView
        descview.text = item?.desc ?: ""
        val iconview: ImageView = myview.findViewById(R.id.item_icon) as ImageView
        iconview.setImageResource(item?.iconrsc ?: 0)
        return myview
    }
}

class RichSelectionDialogFragment: AbstractDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items_icon = arguments?.getIntegerArrayList("items_icon") ?: arrayListOf()
        val items_title = arguments?.getStringArray("items_title") ?: arrayOf()
        val items_desc = arguments?.getStringArray("items_desc") ?: arrayOf()
        val items = items_icon.zip(items_title).zip(items_desc, {a, b -> RichSelectionDialogItem(a.first, a.second, b)})

        val adapter = RichSelectionDialogItemAdapter(context!!, 0, items)
        val listview = ListView(context)
        listview.adapter = adapter
        listview.onItemClickListener = AdapterView.OnItemClickListener {
            parent, view, position, id ->

            val data = Intent()
            data.putExtra("id", arguments?.getInt("id"))
            data.putExtra("which", position)

            notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data)
            dismiss()
        }

        return AlertDialog.Builder(activity!!)
                .setTitle(arguments?.getString("title", ""))
                .setView(listview)
                .setCancelable(true)
                .create()
    }

    class Builder : AbstractDialogFragment.Builder() {
        override fun build(): AbstractDialogFragment {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return RichSelectionDialogFragment()
        }
    }
}