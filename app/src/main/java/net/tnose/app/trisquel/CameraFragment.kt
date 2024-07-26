package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import java.util.*

/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the [OnListFragmentInteractionListener]
 * interface.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class CameraFragment : androidx.fragment.app.Fragment() {
    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mView: RecyclerViewEmptySupport? = null
    private var list: ArrayList<CameraSpec>? = null
    private var cameraRecyclerViewAdapter: MyCameraRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_camera_list, container, false)
        val dao = TrisquelDao(this.context)
        dao.connection()
        list = dao.allCameras
        dao.close()

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        val key  = pref.getInt("camera_sortkey", 0)
        changeSortKey(key)

        // Set the adapter
        if (view is RecyclerViewEmptySupport) {
            mView = view
            val context = view.getContext()
            view.setEmptyMessage(getString(R.string.warning_cam_not_registered))
            view.setEmptyView(container!!.findViewById(R.id.empty_view))
            if (mColumnCount <= 1) {
                view.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            } else {
                view.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, mColumnCount)
            }

            this.cameraRecyclerViewAdapter = MyCameraRecyclerViewAdapter(list!!, mListener)
            view.adapter = cameraRecyclerViewAdapter
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    fun changeSortKey(key: Int){
        if(list != null){
            when(key){
                0 -> {list!!.sortByDescending { it.created }}
                1 -> {list!!.sortBy { it.manufacturer + " " + it.modelName }}
                2 -> {list!!.sortBy { it.mount }}
                3 -> {list!!.sortBy { it.format }}
                else -> {}
            }
            cameraRecyclerViewAdapter?.notifyDataSetChanged()
        }
    }

    fun insertCamera(camera: CameraSpec) {
        if (list != null) {
            val index = list!!.indexOf(camera)
            if (-1 == index) {
                list!!.add(0, camera)
                val dao = TrisquelDao(this.context)
                dao.connection()
                val id = dao.addCamera(camera)
                dao.close()
                camera.id = id.toInt()
                cameraRecyclerViewAdapter!!.notifyItemInserted(0)
                mView?.layoutManager?.scrollToPosition(0)
            }
        }
    }

    fun updateCamera(camera: CameraSpec) {
        if (list != null) {
            for (i in list!!.indices) {
                if (list!![i].id == camera.id) {
                    list!!.removeAt(i)
                    list!!.add(i, camera)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.updateCamera(camera)
                    dao.close()
                    cameraRecyclerViewAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    fun deleteCamera(id: Int) {
        if (list != null) {
            for (i in list!!.indices) {
                val c = list!![i]
                if (c.id == id) {
                    list!!.removeAt(i)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    if (c.type == 1) {
                        dao.deleteLens(dao.getFixedLensIdByBody(id))
                    }
                    dao.deleteCamera(id)
                    dao.close()
                    cameraRecyclerViewAdapter!!.notifyItemRemoved(i)
                    break
                }
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: CameraSpec, isLong: Boolean)
    }

    companion object {
        private val ARG_COLUMN_COUNT = "column-count"
        fun newInstance(columnCount: Int): CameraFragment {
            val fragment = CameraFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
