package net.tnose.app.trisquel

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import net.tnose.app.trisquel.AccessoryFragment.OnListFragmentInteractionListener
import net.tnose.app.trisquel.dummy.DummyContent.DummyItem

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyAccessoryRecyclerViewAdapter(private val mValues: List<Accessory>, private val mListener: OnListFragmentInteractionListener?) : RecyclerView.Adapter<MyAccessoryRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_accessory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mContentView.text = mValues[position].name

        when(holder.mItem?.type){
            Accessory.ACCESSORY_FILTER          ->{holder.mIconView.setImageResource(R.drawable.ic_filter_plane)}
            Accessory.ACCESSORY_EXT_TUBE        ->{holder.mIconView.setImageResource(R.drawable.ic_extenstion_tube)}
            Accessory.ACCESSORY_TELE_CONVERTER ->{holder.mIconView.setImageResource(R.drawable.ic_zoom_in_black_24dp)}
            Accessory.ACCESSORY_WIDE_CONVERTER ->{holder.mIconView.setImageResource(R.drawable.ic_zoom_out_black_24dp)}
            else -> {holder.mIconView.setImageResource(R.drawable.ic_unknown_accessory_plane)}
        }

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, true)
            true
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mContentView: TextView
        val mIconView: ImageView
        var mItem: Accessory? = null

        init {
            mContentView = mView.findViewById<View>(R.id.content) as TextView
            mIconView = mView.findViewById<View>(R.id.icon) as ImageView
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
