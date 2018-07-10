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
public class CameraFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private ArrayList<CameraSpec> list;
    private MyCameraRecyclerViewAdapter cameraRecyclerViewAdapter;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CameraFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static CameraFragment newInstance(int columnCount) {
        CameraFragment fragment = new CameraFragment();
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
        View view = inflater.inflate(R.layout.fragment_camera_list, container, false);
        TrisquelDao dao = new TrisquelDao(this.getContext());
        dao.connection();
        list = dao.getAllCameras();
        dao.close();

        // Set the adapter
        if (view instanceof RecyclerViewEmptySupport) {
            Context context = view.getContext();
            RecyclerViewEmptySupport recyclerView = (RecyclerViewEmptySupport) view;
            recyclerView.setEmptyMessage(getString(R.string.warning_cam_not_registered));
            recyclerView.setEmptyView(container.findViewById(R.id.empty_view));
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            this.cameraRecyclerViewAdapter = new MyCameraRecyclerViewAdapter(list, mListener);
            recyclerView.setAdapter(cameraRecyclerViewAdapter);
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

    public void insertCamera(CameraSpec camera) {
        if (list != null) {
            int index = list.indexOf(camera);
            if (-1 == index) {
                list.add(0, camera);
                TrisquelDao dao = new TrisquelDao(this.getContext());
                dao.connection();
                long id = dao.addCamera(camera);
                dao.close();
                camera.id = (int)id;
                cameraRecyclerViewAdapter.notifyItemInserted(0);
            }
        }
    }

    public void updateCamera(CameraSpec camera) {
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                if(list.get(i).id == camera.id){
                    list.remove(i);
                    list.add(i, camera);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    dao.updateCamera(camera);
                    dao.close();
                    cameraRecyclerViewAdapter.notifyItemChanged(i);
                }
            }
        }
    }

    public void deleteCamera(int id){
        if (list != null){
            for(int i = 0; i < list.size(); i++){
                CameraSpec c = list.get(i);
                if(c.id == id){
                    list.remove(i);
                    TrisquelDao dao = new TrisquelDao(this.getContext());
                    dao.connection();
                    if(c.type == 1){
                        dao.deleteLens(dao.getFixedLensIdByBody(id));
                    }
                    dao.deleteCamera(id);
                    dao.close();
                    cameraRecyclerViewAdapter.notifyItemRemoved(i);
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
        void onListFragmentInteraction(CameraSpec item, boolean isLong);
    }
}
