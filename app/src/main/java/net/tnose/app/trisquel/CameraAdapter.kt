package net.tnose.app.trisquel

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Created by user on 2018/02/12.
 */

class CameraAdapter : ArrayAdapter<CameraSpec> {
    constructor(context: Context, resource: Int) : super(context, resource)
    constructor(context: Context, resource: Int, items: List<CameraSpec>) : super(context, resource, items)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val c = getItem(position)
        view.text = c!!.manufacturer + " " + c.modelName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        val c = getItem(position)
        view.text = c!!.manufacturer + " " + c.modelName
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