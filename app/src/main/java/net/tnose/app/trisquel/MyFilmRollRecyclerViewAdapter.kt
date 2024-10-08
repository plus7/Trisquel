package net.tnose.app.trisquel

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import net.tnose.app.trisquel.FilmRollFragment.OnListFragmentInteractionListener
import java.lang.Long
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class MyFilmRollRecyclerViewAdapter(
    diffCallback: DiffUtil.ItemCallback<FilmRollAndRels>,
    private val mListener: OnListFragmentInteractionListener?
) :
    androidx.recyclerview.widget.ListAdapter<
            FilmRollAndRels,
            MyFilmRollRecyclerViewAdapter.ViewHolder
            >(diffCallback)
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_filmroll, parent, false)
        return ViewHolder(view)
    }

    fun getDateRange(dates : List<String>): String {
        if (dates.size == 0) return ""

        var minDate = Date(Long.MAX_VALUE)
        var maxDate = Date(0)
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        for (date in dates) {
            var d = Date(0)
            try {
                d = sdf.parse(date)
            } catch (e: ParseException) {

            }

            if (minDate.after(d)) {
                minDate = d
            }
            if (maxDate.before(d)) {
                maxDate = d
            }
        }

        return if (minDate == maxDate) {
            sdf.format(minDate)
        } else {
            sdf.format(minDate) + "-" + sdf.format(maxDate)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = getItem(position)
        //holder.mIdView.setText(Integer.toString(mValues.get(position).id));
        if (holder.mItem!!.filmRoll.name.isEmpty()) {
            holder.mNameView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
            holder.mNameView.setText(R.string.empty_name)
        } else {
            holder.mNameView.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            holder.mNameView.text = holder.mItem!!.filmRoll.name
        }
        holder.mCameraAndBrandView.text =
            holder.mItem!!.camera.manufacturer + " " + holder.mItem!!.camera.modelName + "   " +
                    holder.mItem!!.filmRoll.manufacturer + " " + holder.mItem!!.filmRoll.brand

        val exp = holder.mItem!!.photoDates.size
        val array = arrayListOf<String>()
        val f = FilmRoll.fromEntity(holder.mItem!!)
        val dateRange = getDateRange(holder.mItem!!.photoDates)
        if(dateRange.isNotEmpty())
            array.add(dateRange)
        array.add(
                if(exp == 1) "%d shot".format(exp)
                else         "%d shots".format(exp)
        )
        holder.mDateAndShotView.text = array.joinToString("   ")

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(FilmRoll.fromEntity(holder.mItem!!), false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(FilmRoll.fromEntity(holder.mItem!!), true)
            true
        }
    }

    internal class FilmRollDiff : DiffUtil.ItemCallback<FilmRollAndRels>() {
        override fun areItemsTheSame(oldItem: FilmRollAndRels, newItem: FilmRollAndRels): Boolean {
            return oldItem.filmRoll.id == newItem.filmRoll.id
        }

        override fun areContentsTheSame(oldItem: FilmRollAndRels, newItem: FilmRollAndRels): Boolean {
            //return oldItem.getWord().equals(newItem.getWord())
            return oldItem == newItem
        }
    }

    inner class ViewHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        //public final TextView mIdView;
        val mNameView: TextView
        val mCameraAndBrandView: TextView
        val mDateAndShotView: TextView
        var mItem: FilmRollAndRels? = null

        init {
            //mIdView = (TextView) view.findViewById(R.id.id);
            mNameView = mView.findViewById(R.id.name)
            mCameraAndBrandView = mView.findViewById(R.id.camera_and_brand)
            mDateAndShotView = mView.findViewById(R.id.date_and_shot)
            // http://blog.teamtreehouse.com/contextual-action-bars-removing-items-recyclerview
            /*mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((AppCompatActivity)view.getContext()).startSupportActionMode(actionModeCallbacks);
                    return true;
                }
            });*/
        }

        override fun toString(): String {
            return super.toString() + " '" + mCameraAndBrandView.text + "'"
        }
    }
}
