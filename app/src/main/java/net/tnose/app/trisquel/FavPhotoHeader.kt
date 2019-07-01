package net.tnose.app.trisquel

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.chuross.recyclerviewadapters.BaseLocalAdapter

class FavPhotoHeader @JvmOverloads constructor(context: Context,
                                               private val filmrollName: String,
                                               private val clickListener: View.OnClickListener? = null)
    : BaseLocalAdapter<FavPhotoHeader.ViewHolder>(context), Cloneable {

    override fun getAdapterId(): Int {
        return R.layout.fav_photo_header
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavPhotoHeader.ViewHolder {
        val view = LayoutInflater.from(context)
                    .inflate(R.layout.fav_photo_header, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavPhotoHeader.ViewHolder, position: Int) {
        holder.mLabelView.text = filmrollName
        holder.itemView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (clickListener != null) clickListener.onClick(v)
            }
        })
    }

    override fun getItemCount(): Int = 1

    public override fun clone(): FavPhotoHeader {
        return FavPhotoHeader(context, filmrollName, clickListener)
    }

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mLabelView: TextView = mView.findViewById(R.id.label_filmrollname)

        override fun toString(): String {
            return mLabelView.text.toString()
        }
    }
}