package net.tnose.app.trisquel


//import net.tnose.app.trisquel.dummy.DummyContent.DummyItem
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.chuross.recyclerviewadapters.ItemAdapter
import net.tnose.app.trisquel.SearchFragment.OnListFragmentInteractionListener
import java.io.File
import java.util.*

class SearchLocalAdapter(context: Context, mListener: OnListFragmentInteractionListener? = null) : ItemAdapter<Photo, SearchLocalAdapter.ViewHolder>(context) {

    //private val mValues: ArrayList<Photo> = arrayListOf()
    private val mListener: OnListFragmentInteractionListener? = mListener

    override fun getAdapterId(): Int = R.layout.fragment_photo

    //private val mOnClickListener: View.OnClickListener

    init {
        /*mOnClickListener = View.OnClickListener { v ->
            //val item = v.tag as DummyItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            //mListener?.onListFragmentInteraction(item)
        }*/
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dao = TrisquelDao(holder.mView.context) //これでいいのか？？？
        dao.connection()
        val p = get(position)
        holder.mItem = p
        val prev_p = if(position > 0) get(position - 1) else null
        val l = dao.getLens(p.lensid)
        val tags = dao.getTagsByPhoto(p.id)
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

        /*holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, true)
            true
        }*/

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

    // CompositeRecyclerAdapterの未実装部分の影響で機能してない。
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

    private fun setThumbnail(paths: ArrayList<String>, view: ImageView){
        if(paths.size == 0){
            Glide.with(view.context)
                    .load(R.drawable.ic_add_image_gray)
                    .into(view)
        }else {
            val file = File(paths[0])
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
                    .error(R.drawable.ic_error_circle)
            ).into(view)
        }
    }

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
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
