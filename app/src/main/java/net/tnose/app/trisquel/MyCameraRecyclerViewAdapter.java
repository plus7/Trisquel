package net.tnose.app.trisquel;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tnose.app.trisquel.CameraFragment.OnListFragmentInteractionListener;

import java.util.ArrayList;

public class MyCameraRecyclerViewAdapter extends RecyclerView.Adapter<MyCameraRecyclerViewAdapter.ViewHolder> {

    //private final List<DummyItem> mValues;
    private final ArrayList<CameraSpec> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyCameraRecyclerViewAdapter(ArrayList<CameraSpec> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_camera, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        //holder.mManufacturerView.setText(mValues.get(position).manufacturer);
        holder.mModelNameView.setText(mValues.get(position).manufacturer +" "+ mValues.get(position).modelName);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem, false);
                }
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(holder.mItem, true);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mManufacturerView;
        public final TextView mModelNameView;
        public CameraSpec mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mManufacturerView = view.findViewById(R.id.manufacturer);
            mModelNameView = view.findViewById(R.id.model_name);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mModelNameView.getText() + "'";
        }
    }
}
