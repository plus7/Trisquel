package net.tnose.app.trisquel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ReplacementSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.tnose.app.trisquel.PhotoFragment.OnListFragmentInteractionListener
import java.io.File

// Based on https://stackoverflow.com/a/34623367
class RoundedBackgroundSpan(private val context: Context,
                            private val mBackgroundColor: Int,
                            private val mTextColor: Int,
                            private val mTextSize: Float) : ReplacementSpan() {

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        var paint = Paint(paint) // make a copy for not editing the referenced paint
        paint.textSize = mTextSize
        paint.color = mBackgroundColor
        val textHeightWrapping = convertDpToPx(4f)
        val tagBottom = top.toFloat() + textHeightWrapping + PADDING_Y + mTextSize + PADDING_Y + textHeightWrapping
        val tagRight = x + getTagWidth(text, start, end, paint)
        val rect = RectF(x, top.toFloat(), tagRight, tagBottom)
        canvas.drawRoundRect(rect, CORNER_RADIUS.toFloat(), CORNER_RADIUS.toFloat(), paint)

        paint.color = mTextColor
        canvas.drawText(text, start, end, x + PADDING_X, tagBottom - PADDING_Y - textHeightWrapping - MAGIC_NUMBER, paint)
    }

    private fun getTagWidth(text: CharSequence, start: Int, end: Int, paint: Paint): Int {
        return Math.round(PADDING_X + paint.measureText(text.subSequence(start, end).toString()) + PADDING_X)
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        var paint = paint
        paint = Paint(paint) // make a copy for not editing the referenced paint
        paint.textSize = mTextSize
        return getTagWidth(text, start, end, paint)
    }

    fun convertDpToPx(dp: Float): Float {
        val metrics = context.resources.displayMetrics
        return dp * metrics.density
    }

    private val PADDING_X
        get() = convertDpToPx(6.toFloat())
    private val PADDING_Y
        get() = convertDpToPx(0f)
    private val MAGIC_NUMBER
        get() = convertDpToPx(2.toFloat())

    companion object {
        private val CORNER_RADIUS = 8
    }
}

class MyPhotoRecyclerViewAdapter(diffCallback: DiffUtil.ItemCallback<Pair<String, PhotoEntity>>,
                                 private val mFilmRollId: Int,
                                 private val mListener: OnListFragmentInteractionListener?
) : androidx.recyclerview.widget.ListAdapter<
            Pair<String, PhotoEntity>,
            MyPhotoRecyclerViewAdapter.ViewHolder
            >(diffCallback)
{
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
            val rb = if(paths[0].startsWith("/")){
                Glide.with(view.context)
                        .load(File(paths[0]))
            }else{
                Glide.with(view.context)
                        .load(Uri.parse(paths[0]))
            }
            rb.apply(RequestOptions()
                    .placeholder(R.drawable.general_image_gray)
                    .centerCrop()
                    .error(R.drawable.ic_error_circle))
                    .into(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mPrevDate = getItem(position).first
        holder.mItem = getItem(position).second
        val p = Photo.fromEntity(holder.mItem!!)
        val dao = TrisquelDao(holder.mView.context) //これでいいのか？？？
        dao.connection()
        val l = dao.getLens(p.lensid)
        val tags = dao.getTagsByPhoto(p.id)
        dao.close()
        holder.mIdView.text = Integer.toString(p.frameIndex + 1)
        if(holder.mPrevDate == p.date){
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

        val contentsb = StringBuilder()
        val ranges = ArrayList<Pair<Int, Int>>(3)
        tags.sortByDescending { it.refcnt }
        var previousStartPt = p.memo.length+1
        if(tags.size > 3){
            val two = tags.take(2)
            for(t in two){
                contentsb.append(" "+t.label)
                ranges.add(Pair(previousStartPt, previousStartPt+t.label.length))
                previousStartPt = previousStartPt + t.label.length + 1
            }
            contentsb.append(" +" + (tags.size - 2).toString())
            ranges.add(Pair(previousStartPt, previousStartPt + (tags.size - 2).toString().length + 1))
        }else{
            val three = tags.take(3)
            for(t in three){
                contentsb.append(" " + t.label)
                ranges.add(Pair(previousStartPt, previousStartPt+t.label.length))
                previousStartPt = previousStartPt + t.label.length + 1
            }
        }

        val spannable = SpannableString(p.memo + contentsb.toString())
        for(r in ranges) {
            val rspan = RoundedBackgroundSpan(holder.mContentView.context, Color.LTGRAY, Color.BLACK, holder.mContentView.textSize * 0.7f)
            spannable.setSpan(rspan, r.first, r.second, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        holder.mContentView.text = spannable

        setThumbnail(p.supplementalImages, holder.mThumbnailView)

        if(p.favorite)
            holder.mFavorite.setImageResource(R.drawable.ic_fav)
        else
            holder.mFavorite.setImageResource(R.drawable.ic_fav_border)

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(Photo.fromEntity(holder.mItem!!), false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(Photo.fromEntity(holder.mItem!!), true)
            true
        }

        holder.mIdView.setOnClickListener{
            mListener?.onIndexClick(Photo.fromEntity(holder.mItem!!))
        }

        holder.mIdView.setOnLongClickListener{
            mListener?.onIndexLongClick(Photo.fromEntity(holder.mItem!!))
            true
        }

        holder.mThumbnailView.setOnClickListener{
            mListener?.onThumbnailClick(Photo.fromEntity(holder.mItem!!))
        }

        holder.mFavorite.setOnClickListener {
            mListener?.onFavoriteClick(Photo.fromEntity(holder.mItem!!))
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

    internal class PhotoDiff : DiffUtil.ItemCallback<Pair<String, PhotoEntity>>() {
        override fun areItemsTheSame(oldItem: Pair<String, PhotoEntity>, newItem: Pair<String, PhotoEntity>): Boolean {
            val result = oldItem.second.id == newItem.second.id
            return result
        }

        override fun areContentsTheSame(oldItem: Pair<String, PhotoEntity>, newItem: Pair<String, PhotoEntity>): Boolean {
            val ret = (oldItem.first == newItem.first) && (oldItem.second == newItem.second)
            return ret
        }
    }

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView
        val mDateView: TextView
        val mLensView: TextView
        val mParamsView: TextView
        val mContentView: TextView
        val mThumbnailView: ImageView
        var mItem: PhotoEntity? = null
        var mPrevDate : String = "" // 日付の切り替わりタイミングだけで日付ラベルを表示する仕掛けのため、アイテムと直前のアイテムのDateは組で持つ必要がある
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
