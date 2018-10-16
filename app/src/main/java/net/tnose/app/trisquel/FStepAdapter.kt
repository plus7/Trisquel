package net.tnose.app.trisquel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.GridView
import java.util.*

/**
 * Created by user on 2018/07/01.
 */

class FStepAdapter(private val mContext: Context) : BaseAdapter() {
    private val mLayoutInflater: LayoutInflater
    private val mFArray = arrayOf(0.95, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.2, 2.4, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 4.8, 5.0, 5.6, 6.3, 6.7, 7.1, 8.0, 9.0, 9.5, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0, 22.0)

    private val mFCheckedArray = booleanArrayOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false)
    internal var mCheckedList: MutableList<Boolean>

    val fStepsString: String
        get() {
            val sb = StringBuilder()
            for (i in mFArray.indices) {
                if (mCheckedList[i]) {
                    if (sb.length > 0) sb.append(", ")
                    sb.append(mFArray[i].toString())
                }
            }
            return sb.toString()
        }

    private class ViewHolder {
        var checkBox: CheckBox? = null
    }

    init {
        mLayoutInflater = LayoutInflater.from(mContext)
        mCheckedList = ArrayList()
        for (i in mFArray.indices) {
            mCheckedList.add(java.lang.Boolean.FALSE)
        }
    }

    override fun getCount(): Int {
        return mFArray.size
    }

    override fun getItem(position: Int): Any {
        return mFArray[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setCheckedState(s: String) {
        if (s.length > 0) {
            val fsAsArray = s.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val list = ArrayList<Double>()
            for (speed in fsAsArray) {
                list.add(java.lang.Double.parseDouble(speed))
            }
            setCheckedState(list.toTypedArray()) //無駄だけどしょうがない
        }
    }

    fun setCheckedState(ds: Array<Double>) {
        val fsList = Arrays.asList(*mFArray)
        val inputList = Arrays.asList(*ds)
        mCheckedList.clear()

        for (i in fsList.indices) {
            if (inputList.contains(fsList[i])) {
                mCheckedList.add(java.lang.Boolean.TRUE)
            } else {
                mCheckedList.add(java.lang.Boolean.FALSE)
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val holder: ViewHolder
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.grid_item_cb, null)
            holder = ViewHolder()
            holder.checkBox = convertView!!.findViewById(R.id.fval_checkbox)
            holder.checkBox!!.isChecked = mCheckedList[position]
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.checkBox!!.text = mFArray[position].toString()
        //なんでこれでOKなのかよくわからないがとりあえず動いている
        //http://falco.sakura.ne.jp/tech/2012/11/android-listview-onitemclick-%E3%82%A4%E3%83%99%E3%83%B3%E3%83%88%E3%81%8C%E7%99%BA%E7%94%9F%E3%81%97%E3%81%AA%E3%81%84%EF%BC%81/
        holder.checkBox!!.setOnClickListener { v -> (parent as GridView).performItemClick(v, position, v.id.toLong()) }
        holder.checkBox!!.setOnCheckedChangeListener { buttonView, isChecked ->
            val d = java.lang.Double.parseDouble(buttonView.text.toString())
            var i: Int
            i = 0
            while (i < this@FStepAdapter.mFArray.size) {
                if (this@FStepAdapter.mFArray[i] == d) break
                i++
            }
            if (i != this@FStepAdapter.mFArray.size) {
                this@FStepAdapter.mCheckedList[i] = isChecked
            }
        }

        return convertView
    }
}
