package net.tnose.app.trisquel

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.tnose.app.trisquel.FilmRollFragment.OnListFragmentInteractionListener
import java.util.*

class MyFilmRollRecyclerViewAdapter(//private final List<DummyItem> mValues;
        private val mValues: ArrayList<FilmRoll>, private val mListener: OnListFragmentInteractionListener?) : RecyclerView.Adapter<MyFilmRollRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_filmroll, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        //holder.mIdView.setText(Integer.toString(mValues.get(position).id));
        if (mValues[position].name.isEmpty()) {
            holder.mNameView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
            holder.mNameView.setText(R.string.empty_name)
        } else {
            holder.mNameView.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            holder.mNameView.text = mValues[position].name
        }
        holder.mBrandView.text = mValues[position].manufacturer + " " + mValues[position].brand
        holder.mCameraView.text = mValues[position].camera.manufacturer + " " + mValues[position].camera.modelName

        holder.mDateView.text = mValues[position].dateRange
        holder.mShotView.text = Integer.toString(mValues[position].exposures) + " shot(s)"

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
        //public final TextView mIdView;
        val mNameView: TextView
        val mCameraView: TextView
        val mBrandView: TextView
        val mDateView: TextView
        val mShotView: TextView
        var mItem: FilmRoll? = null

        init {
            //mIdView = (TextView) view.findViewById(R.id.id);
            mNameView = mView.findViewById(R.id.name)
            mBrandView = mView.findViewById(R.id.brand)
            mCameraView = mView.findViewById(R.id.camera)
            mDateView = mView.findViewById(R.id.date)
            mShotView = mView.findViewById(R.id.shot)
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
            return super.toString() + " '" + mBrandView.text + "'"
        }
    }
}
