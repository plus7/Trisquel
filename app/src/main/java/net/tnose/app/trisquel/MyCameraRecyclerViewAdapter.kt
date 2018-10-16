package net.tnose.app.trisquel

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.tnose.app.trisquel.CameraFragment.OnListFragmentInteractionListener
import java.util.*

class MyCameraRecyclerViewAdapter(//private final List<DummyItem> mValues;
        private val mValues: ArrayList<CameraSpec>, private val mListener: OnListFragmentInteractionListener?) : RecyclerView.Adapter<MyCameraRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_camera, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        //holder.mManufacturerView.setText(mValues.get(position).manufacturer);
        holder.mModelNameView.text = mValues[position].manufacturer + " " + mValues[position].modelName

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
        val mManufacturerView: TextView
        val mModelNameView: TextView
        var mItem: CameraSpec? = null

        init {
            mManufacturerView = mView.findViewById(R.id.manufacturer)
            mModelNameView = mView.findViewById(R.id.model_name)
        }

        override fun toString(): String {
            return super.toString() + " '" + mModelNameView.text + "'"
        }
    }
}
