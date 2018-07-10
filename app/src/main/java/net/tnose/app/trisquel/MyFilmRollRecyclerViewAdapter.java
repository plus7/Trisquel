package net.tnose.app.trisquel;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tnose.app.trisquel.FilmRollFragment.OnListFragmentInteractionListener;

import java.util.ArrayList;

public class MyFilmRollRecyclerViewAdapter extends RecyclerView.Adapter<MyFilmRollRecyclerViewAdapter.ViewHolder> {

    //private final List<DummyItem> mValues;
    private final ArrayList<FilmRoll> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyFilmRollRecyclerViewAdapter(ArrayList<FilmRoll> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_filmroll, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        //holder.mIdView.setText(Integer.toString(mValues.get(position).id));
        if(mValues.get(position).name.isEmpty()){
            holder.mNameView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
            holder.mNameView.setText(R.string.empty_name);
        }else{
            holder.mNameView.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            holder.mNameView.setText(mValues.get(position).name);
        }
        holder.mBrandView.setText(mValues.get(position).manufacturer + " " + mValues.get(position).brand);
        holder.mCameraView.setText(mValues.get(position).camera.manufacturer + " " + mValues.get(position).camera.modelName);

        holder.mDateView.setText(mValues.get(position).getDateRange());
        holder.mShotView.setText(Integer.toString(mValues.get(position).getExposures()) + " shot(s)");

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
        //public final TextView mIdView;
        public final TextView mNameView;
        public final TextView mCameraView;
        public final TextView mBrandView;
        public final TextView mDateView;
        public final TextView mShotView;
        public FilmRoll mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            //mIdView = (TextView) view.findViewById(R.id.id);
            mNameView = view.findViewById(R.id.name);
            mBrandView = view.findViewById(R.id.brand);
            mCameraView = view.findViewById(R.id.camera);
            mDateView = view.findViewById(R.id.date);
            mShotView = view.findViewById(R.id.shot);
            // http://blog.teamtreehouse.com/contextual-action-bars-removing-items-recyclerview
            /*mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((AppCompatActivity)view.getContext()).startSupportActionMode(actionModeCallbacks);
                    return true;
                }
            });*/
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mBrandView.getText() + "'";
        }
    }
}
