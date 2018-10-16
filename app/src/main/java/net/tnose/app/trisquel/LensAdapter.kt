package net.tnose.app.trisquel

/**
 * Created by user on 2018/02/12.
 */

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class LensAdapter : ArrayAdapter<LensSpec> {
    constructor(context: Context, resource: Int) : super(context, resource)
    constructor(context: Context, resource: Int, items: List<LensSpec>) : super(context, resource, items)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val l = getItem(position)
        view.text = l!!.modelName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        val l = getItem(position)
        view.text = l!!.modelName
        return view
    }

    fun getPosition(id: Int): Int {
        var position = -1
        for (i in 0 until this.count) {
            if (this.getItem(i)!!.id == id) {
                position = i
                break
            }
        }
        return position
    }
}