package net.tnose.app.trisquel;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
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
public class LensFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private ArrayList<LensSpec> list;
    private MyLensRecyclerViewAdapter lensRecyclerViewAdapter;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LensFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static LensFragment newInstance(int columnCount) {
        LensFragment fragment = new LensFragment();
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
        View view = inflater.inflate(R.layout.fragment_lens_list, container, false);
        TrisquelDao dao = new TrisquelDao(this.getContext());
        dao.connection();
        list = dao.getAllVisibleLenses();
        dao.close();

        // Set the adapter
        if (view instanceof RecyclerViewEmptySupport) {
            Context context = view.getContext();
            RecyclerViewEmptySupport recyclerView = (RecyclerViewEmptySupport) view;
            recyclerView.setEmptyMessage(getString(R.string.warning_lens_not_registered));
            recyclerView.setEmptyView(container.findViewById(R.id.empty_view));
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            this.lensRecyclerViewAdapter = new MyLensRecyclerViewAdapter(list, mListener);
            recyclerView.setAdapter(lensRecyclerViewAdapter);
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

    public void insertLens(LensSpec lens) {
        if (list != null) {
            int index = list.indexOf(lens);
            if (-1 == index) {
                list.add(0, lens);
                TrisquelDao dao = new TrisquelDao(this.getContext());
                dao.connection();
                long id = dao.addLens(lens);
                dao.close();
                lens.id = (int)id;
                lensRecyclerViewAdapter.notifyItemInserted(0);
            }
        }
    }

    public void updateLens(LensSpec lens) {
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                LensSpec c = list.get(i);
                if(list.get(i).id == lens.id){
                    list.remove(i);
                    list.add(i, lens);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    dao.updateLens(lens);
                    dao.close();
                    lensRecyclerViewAdapter.notifyItemChanged(i);
                }
            }
        }
    }

    public void deleteLens(int id){
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                LensSpec l = list.get(i);
                if(l.id == id){
                    list.remove(i);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    dao.deleteLens(id);
                    dao.close();
                    lensRecyclerViewAdapter.notifyItemRemoved(i);
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
        void onListFragmentInteraction(LensSpec item, boolean isLong);
    }
}
