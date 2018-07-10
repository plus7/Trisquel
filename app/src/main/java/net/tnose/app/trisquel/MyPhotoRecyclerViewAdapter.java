package net.tnose.app.trisquel;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tnose.app.trisquel.PhotoFragment.OnListFragmentInteractionListener;
import net.tnose.app.trisquel.dummy.DummyContent.DummyItem;

import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyPhotoRecyclerViewAdapter extends RecyclerView.Adapter<MyPhotoRecyclerViewAdapter.ViewHolder> {

    //private final List<DummyItem> mValues;
    private final int mFilmRollId;
    private final ArrayList<Photo> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyPhotoRecyclerViewAdapter(ArrayList<Photo> items, int filmrollid, OnListFragmentInteractionListener listener) {
        mValues = items;
        mFilmRollId = filmrollid;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        TrisquelDao dao = new TrisquelDao(holder.mView.getContext()); //これでいいのか？？？
        dao.connection();
        Photo p = mValues.get(position);
        Log.d("modelname_of_lens", Integer.toString(p.lensid));
        LensSpec l = dao.getLens(p.lensid);
        dao.close();
        holder.mIdView.setText(Integer.toString(p.index+1));
        holder.mDateView.setText(p.date);
        if(l != null)
          holder.mLensView.setText(l.manufacturer + " " + l.modelName);
        String content = MessageFormat.format(
                "f/{0} {1}sec {2}",
                p.aperture, Util.doubleToStringShutterSpeed(p.shutterSpeed), p.memo);
        holder.mContentView.setText(content);
        //holder.mContentView.setText("f/" + p.aperture + " " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "sec" /*+ l.modelName*/);

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
        public final TextView mIdView;
        public final TextView mDateView;
        public final TextView mLensView;
        public final TextView mContentView;
        public Photo mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = view.findViewById(R.id.id);
            mDateView = view.findViewById(R.id.date);
            mLensView = view.findViewById(R.id.lens);
            mContentView = view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
