package net.tnose.app.trisquel

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.tnose.app.trisquel.PhotoFragment.OnListFragmentInteractionListener
import java.io.File
import java.util.*

class MyPhotoRecyclerViewAdapter(private val mValues: ArrayList<Photo>,
                                 private val mFilmRollId: Int, private val mListener: OnListFragmentInteractionListener?) : RecyclerView.Adapter<MyPhotoRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_photo, parent, false)
        return ViewHolder(view)
    }

    private fun setThumbnail(paths: ArrayList<String>, view: ImageView){
        if(paths.size == 0){
            Glide.with(view.context)
                    .load(R.drawable.ic_add_image_gray)
                    .into(view)
        }else {
            val file = File(paths[0])
            Glide.with(view.context)
                    .load(file)
                    .apply(RequestOptions()
                            .placeholder(R.drawable.general_image_gray)
                            .centerCrop()
                            .error(R.drawable.ic_error_circle)
                    )
                    .into(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        val dao = TrisquelDao(holder.mView.context) //これでいいのか？？？
        dao.connection()
        val p = mValues[position]
        val prev_p = if(position > 0) mValues[position - 1] else null
        val l = dao.getLens(p.lensid)
        dao.close()
        holder.mIdView.text = Integer.toString(p.frameIndex + 1)
        if(prev_p != null && prev_p.date.equals(p.date)){
            holder.mDateView.visibility = View.GONE
        }else {
            holder.mDateView.text = p.date
            holder.mDateView.visibility = View.VISIBLE
        }
        if (l != null)
            holder.mLensView.text = l.manufacturer + " " + l.modelName
        val params = arrayListOf<String>()
        if(p.aperture != 0.0) params.add("f/%.1f".format(p.aperture))
        if(p.shutterSpeed != 0.0) params.add("%ssec".format(Util.doubleToStringShutterSpeed(p.shutterSpeed)))
        holder.mParamsView.text = params.joinToString(" ")
        holder.mContentView.text = p.memo
        /*if(p.memo.isNotEmpty()){
            holder.mContentView.visibility = View.VISIBLE
        }*/
        setThumbnail(p.supplementalImages, holder.mThumbnailView)
        //holder.mContentView.setText("f/" + p.aperture + " " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "sec" /*+ l.modelName*/);

        if(p.favorite)
            holder.mFavorite.setImageResource(R.drawable.ic_fav)
        else
            holder.mFavorite.setImageResource(R.drawable.ic_fav_border)

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, true)
            true
        }

        holder.mIdView.setOnClickListener{
            mListener?.onIndexClick(holder.mItem!!)
        }

        holder.mIdView.setOnLongClickListener{
            mListener?.onIndexLongClick(holder.mItem!!)
            true
        }

        holder.mThumbnailView.setOnClickListener{
            mListener?.onThumbnailClick(holder.mItem!!)
        }

        holder.mFavorite.setOnClickListener {
            mListener?.onFavoriteClick(holder.mItem!!)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            this.onBindViewHolder(holder, position)
        } else {
            // 部分的に更新する
            if(payloads.isNotEmpty()) {
                val flag = payloads[0]
                if(flag is Boolean)
                    holder.toggleFav(flag)
            }
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView
        val mDateView: TextView
        val mLensView: TextView
        val mParamsView: TextView
        val mContentView: TextView
        val mThumbnailView: ImageView
        var mItem: Photo? = null
        val mFavorite: ImageView

        init {
            mIdView = mView.findViewById(R.id.id)
            mDateView = mView.findViewById(R.id.date)
            mLensView = mView.findViewById(R.id.lens)
            mParamsView = mView.findViewById(R.id.params)
            mContentView = mView.findViewById(R.id.content)
            mThumbnailView = mView.findViewById(R.id.thumbnail)
            mFavorite = mView.findViewById(R.id.favorite)
        }

        fun toggleFav(fav: Boolean){
            if(fav)
                mFavorite.setImageResource(R.drawable.ic_fav)
            else
                mFavorite.setImageResource(R.drawable.ic_fav_border)
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
