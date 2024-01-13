package net.tnose.app.trisquel


import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.tnose.app.trisquel.FavoritePhotoFragment.OnListFragmentInteractionListener
import net.tnose.app.trisquel.databinding.FragmentFavoritePhotoBinding
import net.tnose.app.trisquel.dummy.DummyContent.DummyItem
import java.io.File

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
        //val view = LayoutInflater.from(parent.context)
        ///        .inflate(R.layout.fragment_favorite_photo, parent, false)
        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentFavoritePhotoBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]

        val dao = TrisquelDao(holder.binding.cardView.context)
        dao.connection()
        val l = dao.getLens(item.lensid)
        val f = dao.getFilmRoll(item.filmrollid)
        val c = f!!.camera
        dao.close()

        holder.binding.title.text = "%s #%d".format(f.name, item.frameIndex + 1)
        holder.binding.lens.text = "%s %s / %s %s".format(c.manufacturer, c.modelName, l!!.manufacturer, l.modelName)

        val params = arrayListOf<String>()
        if(item.aperture != 0.0) params.add("f/%.1f".format(item.aperture))
        if(item.shutterSpeed != 0.0) params.add("%ssec".format(Util.doubleToStringShutterSpeed(item.shutterSpeed)))
        holder.binding.params.text = params.joinToString(" ")

        if(item.memo.isEmpty()){
            holder.binding.content.visibility = View.GONE
        }else{
            holder.binding.content.text = item.memo
            holder.binding.content.visibility = View.VISIBLE
        }

        if(item.supplementalImages.size == 0){
            Glide.with(holder.binding.photo.context)
                    .load(R.drawable.general_image_gray)
                    .into(holder.binding.photo)
        }else {
            val rb = if(item.supplementalImages[0].startsWith("/")){ //旧仕様
                Glide.with(holder.binding.photo.context)
                        .load(File(item.supplementalImages[0]))
            }else{ // content:// Android11以降対応
                Glide.with(holder.binding.photo.context)
                        .load(Uri.parse(item.supplementalImages[0]))
            }
            rb.apply(RequestOptions()
                        .placeholder(R.drawable.general_image_gray)
                        .centerCrop()
                        .error(R.drawable.ic_error_circle))
                    .into(holder.binding.photo)
        }

        with(holder.binding.cardView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val binding: FragmentFavoritePhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        /*val mTitleView: TextView = mView.title
        val mLensView: TextView = mView.lens
        val mParamsView: TextView = mView.params
        val mContentView: TextView = mView.content
        val mImageView: ImageView = mView.photo*/

        override fun toString(): String {
            return super.toString() + " '" + binding.content.text + "'"
        }
    }
}
