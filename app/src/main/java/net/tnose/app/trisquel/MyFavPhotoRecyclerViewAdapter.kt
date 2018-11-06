package net.tnose.app.trisquel


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_favorite_photo.view.*
import net.tnose.app.trisquel.FavoritePhotoFragment.OnListFragmentInteractionListener
import net.tnose.app.trisquel.dummy.DummyContent.DummyItem
import java.io.File
import java.util.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyFavPhotoRecyclerViewAdapter(
        private val mValues: ArrayList<Photo>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyFavPhotoRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            //val item = v.tag as DummyItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            //mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_favorite_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]

        val dao = TrisquelDao(holder.mView.context)
        dao.connection()
        val l = dao.getLens(item.lensid)
        val f = dao.getFilmRoll(item.filmrollid)
        val c = f!!.camera
        dao.close()

        holder.mTitleView.text = "%s #%d".format(f.name, item.frameIndex + 1)
        holder.mLensView.text = "%s %s / %s %s".format(c.manufacturer, c.modelName, l!!.manufacturer, l.modelName)

        val params = arrayListOf<String>()
        if(item.aperture != 0.0) params.add("f/%.1f".format(item.aperture))
        if(item.shutterSpeed != 0.0) params.add("%ssec".format(Util.doubleToStringShutterSpeed(item.shutterSpeed)))
        holder.mParamsView.text = params.joinToString(" ")

        if(item.memo.isEmpty()){
            holder.mContentView.visibility = View.GONE
        }else{
            holder.mContentView.text = item.memo
            holder.mContentView.visibility = View.VISIBLE
        }

        if(item.supplementalImages.size == 0){
            Glide.with(holder.mImageView.context)
                    .load(R.drawable.general_image_gray)
                    .into(holder.mImageView)
        }else {
            val file = File(item.supplementalImages[0])
            Glide.with(holder.mImageView.context)
                    .load(file)
                    .apply(RequestOptions()
                            .placeholder(R.drawable.general_image_gray)
                            .centerCrop()
                            .error(R.drawable.ic_error_circle)
                    )
                    .into(holder.mImageView)
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitleView: TextView = mView.title
        val mLensView: TextView = mView.lens
        val mParamsView: TextView = mView.params
        val mContentView: TextView = mView.content
        val mImageView: ImageView = mView.photo

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
