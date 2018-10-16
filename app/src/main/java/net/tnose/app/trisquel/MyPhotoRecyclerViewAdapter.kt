package net.tnose.app.trisquel

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.tnose.app.trisquel.PhotoFragment.OnListFragmentInteractionListener
import net.tnose.app.trisquel.dummy.DummyContent.DummyItem
import java.text.MessageFormat
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        val dao = TrisquelDao(holder.mView.context) //これでいいのか？？？
        dao.connection()
        val p = mValues[position]
        Log.d("modelname_of_lens", Integer.toString(p.lensid))
        val l = dao.getLens(p.lensid)
        dao.close()
        holder.mIdView.text = Integer.toString(p.index + 1)
        holder.mDateView.text = p.date
        if (l != null)
            holder.mLensView.text = l.manufacturer + " " + l.modelName
        val content = MessageFormat.format(
                "f/{0} {1}sec {2}",
                p.aperture, Util.doubleToStringShutterSpeed(p.shutterSpeed), p.memo)
        holder.mContentView.text = content
        //holder.mContentView.setText("f/" + p.aperture + " " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "sec" /*+ l.modelName*/);

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, false)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!, true)
            true
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView
        val mDateView: TextView
        val mLensView: TextView
        val mContentView: TextView
        var mItem: Photo? = null

        init {
            mIdView = mView.findViewById(R.id.id)
            mDateView = mView.findViewById(R.id.date)
            mLensView = mView.findViewById(R.id.lens)
            mContentView = mView.findViewById(R.id.content)
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
