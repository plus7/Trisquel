package net.tnose.app.trisquel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil


//private val mValues: List<Accessory>, private val mListener: OnListFragmentInteractionListener?
class MyAccessoryRecyclerViewAdapter(diffCallback: DiffUtil.ItemCallback<AccessoryEntity>, private val mListener: AccessoryFragment.OnListFragmentInteractionListener?) : androidx.recyclerview.widget.ListAdapter<AccessoryEntity, MyAccessoryRecyclerViewAdapter.ViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_accessory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = getItem(position)
        holder.mContentView.text = getItem(position).name

        when(holder.mItem?.type){
            Accessory.ACCESSORY_FILTER          ->{holder.mIconView.setImageResource(R.drawable.ic_filter_plane)}
            Accessory.ACCESSORY_EXT_TUBE        ->{holder.mIconView.setImageResource(R.drawable.ic_extenstion_tube)}
            Accessory.ACCESSORY_TELE_CONVERTER ->{holder.mIconView.setImageResource(R.drawable.ic_zoom_in_black_24dp)}
            Accessory.ACCESSORY_WIDE_CONVERTER ->{holder.mIconView.setImageResource(R.drawable.ic_zoom_out_black_24dp)}
            else -> {holder.mIconView.setImageResource(R.drawable.ic_unknown_accessory_plane)}
        }

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(Accessory.fromEntity(holder.mItem!!), false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(Accessory.fromEntity(holder.mItem!!), true)
            true
        }
    }


    internal class AccessoryDiff : DiffUtil.ItemCallback<AccessoryEntity>() {
        override fun areItemsTheSame(oldItem: AccessoryEntity, newItem: AccessoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AccessoryEntity, newItem: AccessoryEntity): Boolean {
            //return oldItem.getWord().equals(newItem.getWord())
            return oldItem == newItem
        }
    }

    /*override fun getItemCount(): Int {
        return mValues.size
    }*/

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mContentView: TextView
        val mIconView: ImageView
        var mItem: AccessoryEntity? = null

        init {
            mContentView = mView.findViewById<View>(R.id.content) as TextView
            mIconView = mView.findViewById<View>(R.id.icon) as ImageView
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
