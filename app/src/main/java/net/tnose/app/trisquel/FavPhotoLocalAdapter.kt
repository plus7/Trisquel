package net.tnose.app.trisquel

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.chuross.recyclerviewadapters.ItemAdapter
import kotlinx.android.synthetic.main.fav_photo_item.view.*
import java.io.File

class FavPhotoLocalAdapter(context: Context, @ColorInt val textColor: Int? = null) : ItemAdapter<Photo, FavPhotoLocalAdapter.ViewHolder>(context) {

    override fun getAdapterId(): Int = R.layout.fav_photo_item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavPhotoLocalAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(inflater.inflate(adapterId, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = get(position)

        if(photo.supplementalImages.size == 0){
            holder.mFocalLengthView.text = "%.0fmm".format(photo.focalLength)
            holder.mApertureView.text = "F%s".format(photo.aperture.toString())
            holder.mFocalLengthView.visibility = View.VISIBLE
            holder.mApertureView.visibility = View.VISIBLE
            if(photo.shutterSpeed > 0) {
                holder.mShutterSpeedView.text = Util.doubleToStringShutterSpeed(photo.shutterSpeed)
                holder.mShutterSpeedView.visibility = View.VISIBLE
            } else {
                holder.mShutterSpeedView.visibility = View.GONE
            }
            Glide.with(holder.mImageView.context)
                    .load(R.drawable.general_image_gray)
                    .into(holder.mImageView)
        }else {
            holder.mFocalLengthView.visibility = View.INVISIBLE
            holder.mApertureView.visibility = View.INVISIBLE
            holder.mShutterSpeedView.visibility = View.INVISIBLE

            val path = photo.supplementalImages[0]

            val rb = if(path.startsWith("/")){
                Glide.with(holder.mImageView.context)
                        .load(File(path))
            }else{
                Glide.with(holder.mImageView.context)
                        .load(Uri.parse(path))
            }
            rb.apply(RequestOptions()
                    .placeholder(R.drawable.general_image_gray)
                    .centerCrop()
                    .error(R.drawable.ic_error_circle)
            ).into(holder.mImageView)

            holder.mImageView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                // 大きさが確定してから描画しないといけない。
                // 当初はonCreateで追加されたCustomImageViewにpathが設定された瞬間に描画していたが、
                // それだと妙に小さくなる。なおこの半端な実装はgitの履歴には残していない。
                if(path.isNotEmpty() &&
                        oldBottom - oldTop != bottom - top &&
                        oldRight - oldLeft != right - left){
                    val file = File(path)
                    Glide.with(holder.mImageView.context)
                            .load(file)
                            .apply(RequestOptions()
                                    .placeholder(R.drawable.general_image_gray)
                                    .centerCrop()
                                    .error(R.drawable.ic_error_circle)
                            )
                            .into(holder.mImageView)
                }
            }
        }
    }

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mFocalLengthView: TextView = mView.focallength
        val mApertureView: TextView = mView.aperture
        val mShutterSpeedView: TextView = mView.shutterspeed
        val mImageView: ImageView = mView.photo

    }
}