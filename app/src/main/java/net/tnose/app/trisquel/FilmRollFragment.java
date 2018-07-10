package net.tnose.app.trisquel;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
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
public class FilmRollFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    private RecyclerViewEmptySupport mRecyclerView;
    private ArrayList<FilmRoll> list;
    private MyFilmRollRecyclerViewAdapter filmrollRecyclerViewAdapter;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilmRollFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FilmRollFragment newInstance(int columnCount) {
        FilmRollFragment fragment = new FilmRollFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filmroll_list, container, false);
        TrisquelDao dao = new TrisquelDao(this.getContext());
        dao.connection();
        list = dao.getAllFilmRolls();
        dao.close();

        // Set the adapter
        if (view instanceof RecyclerViewEmptySupport) {
            Context context = view.getContext();
            mRecyclerView = (RecyclerViewEmptySupport) view;
            mRecyclerView.setEmptyMessage(getString(R.string.warning_filmroll_not_registered));
            mRecyclerView.setEmptyView(container.findViewById(R.id.empty_view));
            if (mColumnCount <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            filmrollRecyclerViewAdapter = new MyFilmRollRecyclerViewAdapter(list, mListener);
        }
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(filmrollRecyclerViewAdapter);
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

    public void insertFilmRoll(FilmRoll filmroll) {
        if (list != null) {
            int index = list.indexOf(filmroll);
            if (-1 == index) {
                list.add(0, filmroll);
                TrisquelDao dao = new TrisquelDao(this.getContext());
                dao.connection();
                long id = dao.addFilmRoll(filmroll);
                filmroll.id = (int)id;
                dao.close();
                Log.d("FilmRollFragment", "notifyItemInserted");
                filmrollRecyclerViewAdapter.notifyItemInserted(0);
            }
        }
    }

    public void updateFilmRoll(FilmRoll filmroll) {
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                FilmRoll c = list.get(i);
                if(list.get(i).id == filmroll.id){
                    list.remove(i);
                    list.add(i, filmroll);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    dao.updateFilmRoll(filmroll);
                    ArrayList<Photo> p = dao.getPhotosByFilmRollId(filmroll.id);
                    filmroll.photos = p;
                    dao.close();
                    filmrollRecyclerViewAdapter.notifyItemChanged(i);
                }
            }
        }
    }

    public void refreshFilmRoll(int id) {
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                FilmRoll c = list.get(i);
                if(list.get(i).id == id){
                    list.remove(i);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    FilmRoll f = dao.getFilmRoll(id);
                    ArrayList<Photo> p = dao.getPhotosByFilmRollId(id);
                    f.photos = p;
                    dao.close();
                    list.add(i, f);
                    Log.d("refreshFilmRoll", Integer.toString(id));
                    filmrollRecyclerViewAdapter.notifyItemChanged(i);
                }
            }
        }
    }

    public void deleteFilmRoll(int id){
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                FilmRoll c = list.get(i);
                if(list.get(i).id == id){
                    list.remove(i);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    dao.deleteFilmRoll(id);
                    dao.close();
                    Log.d("deleteFilmRoll", Integer.toString(id));
                    filmrollRecyclerViewAdapter.notifyItemRemoved(i);
                }
            }
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
        void onListFragmentInteraction(FilmRoll item, boolean isLong);
    }
}
