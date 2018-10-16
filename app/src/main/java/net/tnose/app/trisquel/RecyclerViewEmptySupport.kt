package net.tnose.app.trisquel

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

/**
 * Created by user on 2018/02/17.
 */
// based on https://stackoverflow.com/questions/28217436/how-to-show-an-empty-view-with-a-recyclerview
//  - added event handler for onItemRangeInserted and onItemRangeRemoved
//  - added setEmptyMessage setter function
class RecyclerViewEmptySupport : RecyclerView {
    private var emptyView: View? = null
    private var emptyMessage: String? = null

    private val emptyObserver = object : RecyclerView.AdapterDataObserver() {

        private fun doEmptyCheck() {
            val adapter = adapter
            if (adapter != null && emptyView != null) {
                if (adapter.itemCount == 0) {
                    if (emptyView != null && emptyView is TextView) {
                        val tv = emptyView as TextView?
                        tv!!.text = emptyMessage
                    }
                    emptyView!!.visibility = View.VISIBLE
                    this@RecyclerViewEmptySupport.visibility = View.GONE
                } else {
                    emptyView!!.visibility = View.GONE
                    this@RecyclerViewEmptySupport.visibility = View.VISIBLE
                }
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            doEmptyCheck()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            doEmptyCheck()
        }

        override fun onChanged() {
            doEmptyCheck()
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        super.setAdapter(adapter)

        adapter?.registerAdapterDataObserver(emptyObserver)

        emptyObserver.onChanged()
    }

    fun setEmptyView(emptyView: View) {
        this.emptyView = emptyView
    }

    fun setEmptyMessage(s: String) {
        this.emptyMessage = s
        if (this.emptyView != null && this.emptyView is TextView) {
            val tv = this.emptyView as TextView?
            tv!!.text = s
        }
    }
}