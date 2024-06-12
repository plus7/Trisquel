package net.tnose.app.trisquel

import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.tnose.app.trisquel.SearchFragment.OnListFragmentInteractionListener
import java.io.File

// class RoundedBackgroundSpanはMyPhotoRecyclerViewAdapter.ktと同じ定義を流用
class MySearchRecyclerViewAdapter(diffCallback: DiffUtil.ItemCallback<Pair<Pair<String, Int>, PhotoAndRels>>,
                                 //private val mFilmRollId: Int,
                                 private val mListener: OnListFragmentInteractionListener?
) : androidx.recyclerview.widget.ListAdapter<
            Pair<Pair<String, Int>, PhotoAndRels>,
            MySearchRecyclerViewAdapter.ViewHolder
            >(diffCallback)
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_photo_search, parent, false)
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
        holder.mPrevDate = getItem(position).first.first
        holder.mPrevFilmRoll = getItem(position).first.second
        holder.mItem = getItem(position).second
        val p = Photo.fromEntity(holder.mItem!!.photo)
        val dao = TrisquelDao(holder.mView.context) //これでいいのか？？？
        dao.connection()
        val l = dao.getLens(p.lensid)
        val tags = dao.getTagsByPhoto(p.id)
        dao.close()
        holder.mIdView.text = Integer.toString(p.frameIndex + 1)
        if(holder.mPrevFilmRoll == p.filmrollid){
            holder.mFilmRollView.visibility = View.GONE
        }else {
            holder.mFilmRollView.text = getItem(position).second.filmRoll
            holder.mFilmRollView.visibility = View.VISIBLE
        }
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
            mListener?.onListFragmentInteraction(Photo.fromEntity(holder.mItem!!.photo), false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(Photo.fromEntity(holder.mItem!!.photo), true)
            true
        }

        holder.mIdView.setOnClickListener{
            mListener?.onIndexClick(Photo.fromEntity(holder.mItem!!.photo))
        }

        holder.mIdView.setOnLongClickListener{
            mListener?.onIndexLongClick(Photo.fromEntity(holder.mItem!!.photo))
            true
        }

        holder.mThumbnailView.setOnClickListener{
            mListener?.onThumbnailClick(Photo.fromEntity(holder.mItem!!.photo))
        }

        holder.mFavorite.setOnClickListener {
            mListener?.onFavoriteClick(Photo.fromEntity(holder.mItem!!.photo))
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

    internal class PhotoDiff : DiffUtil.ItemCallback<Pair<Pair<String, Int>, PhotoAndRels>>() {
        override fun areItemsTheSame(oldItem: Pair<Pair<String, Int>, PhotoAndRels>, newItem: Pair<Pair<String, Int>, PhotoAndRels>): Boolean {
            val result = oldItem.second.photo.id == newItem.second.photo.id
            return result
        }

        override fun areContentsTheSame(oldItem: Pair<Pair<String, Int>, PhotoAndRels>, newItem: Pair<Pair<String, Int>, PhotoAndRels>): Boolean {
            val ret = (oldItem.first == newItem.first) && (oldItem.second == newItem.second)
            return ret
        }
    }

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView
        val mFilmRollView: TextView
        val mDateView: TextView
        val mLensView: TextView
        val mParamsView: TextView
        val mContentView: TextView
        val mThumbnailView: ImageView
        var mItem: PhotoAndRels? = null
        var mPrevDate : String = "" // 日付の切り替わりタイミングだけで日付ラベルを表示する仕掛けのため、アイテムと直前のアイテムのDateは組で持つ必要がある
        var mPrevFilmRoll : Int = 0 // 日付の切り替わりタイミングだけで日付ラベルを表示する仕掛けのため、アイテムと直前のアイテムのFilmRollは組で持つ必要がある
        val mFavorite: ImageView

        init {
            mIdView = mView.findViewById(R.id.id)
            mFilmRollView = mView.findViewById(R.id.label_filmrollname)
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
