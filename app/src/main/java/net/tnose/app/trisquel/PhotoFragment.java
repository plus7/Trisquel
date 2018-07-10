package net.tnose.app.trisquel;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class PhotoFragment extends Fragment {
    private int id;

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_FILMROLL_ID = "filmroll_id";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private int mFilmRollId = -1;
    private FilmRoll mFilmRoll;
    private ArrayList<Photo> mPhotos;
    private OnListFragmentInteractionListener mListener;
    private MyPhotoRecyclerViewAdapter photoRecyclerViewAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PhotoFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static PhotoFragment newInstance(int columnCount, int filmRollId) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putInt(ARG_FILMROLL_ID, filmRollId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            mFilmRollId = getArguments().getInt(ARG_FILMROLL_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_list, container, false);

        TrisquelDao dao = new TrisquelDao(this.getContext());
        dao.connection();
        //mFilmRoll = dao.getFilmRoll(mFilmRollId);
        mPhotos = dao.getPhotosByFilmRollId(mFilmRollId);
        dao.close();

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            this.photoRecyclerViewAdapter = new MyPhotoRecyclerViewAdapter(mPhotos, mFilmRollId, mListener);
            recyclerView.setAdapter(photoRecyclerViewAdapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void insertPhoto(Photo p) {
        if (mPhotos != null) {
            int index;
            TrisquelDao dao = new TrisquelDao(this.getContext());
            dao.connection();
            if(p.index == -1){
                index = mPhotos.size();
                p.index = index;
            }else{
                index = p.index;
                // 挿入する前にこれ以降のインデックスをずらす
                for(int i = mPhotos.size()-1; i >= index; i--){
                    Photo followingPhoto = mPhotos.get(i);
                    followingPhoto.index++;
                    dao.updatePhoto(followingPhoto);
                    photoRecyclerViewAdapter.notifyItemChanged(i);
                }
            }
            mPhotos.add(index, p);
            long id = dao.addPhoto(p);
            p.id = (int)id;
            photoRecyclerViewAdapter.notifyItemInserted(index);
            dao.close();
        }
    }

    public void updatePhoto(Photo p) {
        if (mPhotos != null) {
            for(int i = 0; i < mPhotos.size(); i++) {
                if (mPhotos.get(i).id == p.id) {
                    mPhotos.remove(i);
                    mPhotos.add(i, p);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    dao.updatePhoto(p);
                    dao.close();
                    photoRecyclerViewAdapter.notifyItemChanged(i);
                }
            }
        }
    }

    public void deletePhoto(int id) {
        int deletedIndex = -1;
        if (mPhotos != null) {
            TrisquelDao dao = new TrisquelDao(this.getContext());
            dao.connection();
            for(int i = 0; i < mPhotos.size(); i++) {
                if (mPhotos.get(i).id == id) {
                    deletedIndex = i;
                    mPhotos.remove(i);
                    dao.deletePhoto(id);
                    photoRecyclerViewAdapter.notifyItemRemoved(i);
                    break;
                }
            }
            //削除した写真以降のインデックスをずらす
            for(int i = deletedIndex; i < mPhotos.size(); i++){
                Photo followingPhoto = mPhotos.get(i);
                followingPhoto.index--;
                dao.updatePhoto(followingPhoto);
                photoRecyclerViewAdapter.notifyItemChanged(i);
            }
            dao.close();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Photo item, boolean isLong);
    }
}
