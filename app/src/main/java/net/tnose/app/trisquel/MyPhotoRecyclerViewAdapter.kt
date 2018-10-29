package net.tnose.app.trisquel

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.tnose.app.trisquel.PhotoFragment.OnListFragmentInteractionListener
import net.tnose.app.trisquel.dummy.DummyContent.DummyItem
import java.io.File
import java.util.*


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyPhotoRecyclerViewAdapter(private val mValues: ArrayList<Photo>, //private final List<DummyItem> mValues;
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
                            .placeholder(R.drawable.ic_add_image_gray)
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
        Log.d("modelname_of_lens", Integer.toString(p.lensid))
        val l = dao.getLens(p.lensid)
        dao.close()
        holder.mIdView.text = Integer.toString(p.index + 1)
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
        if(p.memo.isEmpty()){
            holder.mContentView.visibility = View.GONE
        }else {
            holder.mContentView.text = p.memo
            holder.mContentView.visibility = View.VISIBLE
        }
        setThumbnail(p.supplementalImages, holder.mThumbnailView)
        //holder.mContentView.setText("f/" + p.aperture + " " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "sec" /*+ l.modelName*/);

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, true)
            true
        }

        holder.mIdView.setOnClickListener{
            Log.d("PhotoRecyclerView", "mIdView.OnClick")
            mListener?.onIndexClick(holder.mItem!!)
        }

        holder.mThumbnailView.setOnClickListener{
            Log.d("PhotoRecyclerView", "mThumbnailView.OnClick")
            mListener?.onThumbnailClick(holder.mItem!!)
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

        init {
            mIdView = mView.findViewById(R.id.id)
            mDateView = mView.findViewById(R.id.date)
            mLensView = mView.findViewById(R.id.lens)
            mParamsView = mView.findViewById(R.id.params)
            mContentView = mView.findViewById(R.id.content)
            mThumbnailView = mView.findViewById(R.id.thumbnail)
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
