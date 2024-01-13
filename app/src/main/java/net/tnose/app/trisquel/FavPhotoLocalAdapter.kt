package net.tnose.app.trisquel

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.chuross.recyclerviewadapters.ItemAdapter
import net.tnose.app.trisquel.databinding.FavPhotoItemBinding
import java.io.File

class FavPhotoLocalAdapter(context: Context, @ColorInt val textColor: Int? = null) : ItemAdapter<Photo, FavPhotoLocalAdapter.ViewHolder>(context) {

    override fun getAdapterId(): Int = R.layout.fav_photo_item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavPhotoLocalAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)
        //return ViewHolder(inflater.inflate(adapterId, parent, false))

        //val layoutInflater = LayoutInflater.from(parent.context)
        val binding = FavPhotoItemBinding.inflate(inflater, parent, false)
        //val binding = SampleViewBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = get(position)

        if(photo.supplementalImages.size == 0){
            holder.binding.focallength.text = "%.0fmm".format(photo.focalLength)
            holder.binding.aperture.text = "F%s".format(photo.aperture.toString())
            holder.binding.focallength.visibility = View.VISIBLE
            holder.binding.aperture.visibility = View.VISIBLE
            if(photo.shutterSpeed > 0) {
                holder.binding.shutterspeed.text = Util.doubleToStringShutterSpeed(photo.shutterSpeed)
                holder.binding.shutterspeed.visibility = View.VISIBLE
            } else {
                holder.binding.shutterspeed.visibility = View.GONE
            }
            Glide.with(holder.binding.photo.context)
                    .load(R.drawable.general_image_gray)
                    .into(holder.binding.photo)
        }else {
            holder.binding.focallength.visibility = View.INVISIBLE
            holder.binding.aperture.visibility = View.INVISIBLE
            holder.binding.shutterspeed.visibility = View.INVISIBLE

            val path = photo.supplementalImages[0]

            val rb = if(path.startsWith("/")){
                Glide.with(holder.binding.photo.context)
                        .load(File(path))
            }else{
                Glide.with(holder.binding.photo.context)
                        .load(Uri.parse(path))
            }
            rb.apply(RequestOptions()
                    .placeholder(R.drawable.general_image_gray)
                    .centerCrop()
                    .error(R.drawable.ic_error_circle)
            ).into(holder.binding.photo)

            holder.binding.photo.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                // 大きさが確定してから描画しないといけない。
                // 当初はonCreateで追加されたCustomImageにpathが設定された瞬間に描画していたが、
                // それだと妙に小さくなる。なおこの半端な実装はgitの履歴には残していない。
                if(path.isNotEmpty() &&
                        oldBottom - oldTop != bottom - top &&
                        oldRight - oldLeft != right - left){
                    val rb = if(path.startsWith("/")){
                        Glide.with(holder.binding.photo.context)
                                .load(File(path))
                    }else{
                        Glide.with(holder.binding.photo.context)
                                .load(Uri.parse(path))
                    }
                    rb.apply(RequestOptions()
                                    .placeholder(R.drawable.general_image_gray)
                                    .centerCrop()
                                    .error(R.drawable.ic_error_circle)
                            )
                            .into(holder.binding.photo)
                }
            }
        }
    }

    inner class ViewHolder(val binding: FavPhotoItemBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        /*val mFocalLengthView: TextView = mView.focallength
        val mApertureView: TextView = mView.aperture
        val mShutterSpeedView: TextView = mView.shutterspeed
        val mImageView: ImageView = mView.photo*/

    }
}