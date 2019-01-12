package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
class LensFragment : Fragment() {
    // TODO: Customize parameters
    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mView: RecyclerViewEmptySupport? = null
    private var list: ArrayList<LensSpec>? = null
    private var lensRecyclerViewAdapter: MyLensRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_lens_list, container, false)
        val dao = TrisquelDao(this.context)
        dao.connection()
        list = dao.allVisibleLenses
        dao.close()

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.context)
        val key  = pref.getInt("lens_sortkey", 0)
        changeSortKey(key)

        // Set the adapter
        if (view is RecyclerViewEmptySupport) {
            mView = view
            val context = view.getContext()
            view.setEmptyMessage(getString(R.string.warning_lens_not_registered))
            view.setEmptyView(container!!.findViewById(R.id.empty_view))
            if (mColumnCount <= 1) {
                view.layoutManager = LinearLayoutManager(context)
            } else {
                view.layoutManager = GridLayoutManager(context, mColumnCount)
            }

            this.lensRecyclerViewAdapter = MyLensRecyclerViewAdapter(list!!, mListener)
            view.adapter = lensRecyclerViewAdapter
        }
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
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
                3 -> {list!!.sortBy { it.focalLengthRange.first }}
                else -> {}
            }
            lensRecyclerViewAdapter?.notifyDataSetChanged()
        }
    }

    fun insertLens(lens: LensSpec) {
        if (list != null) {
            val index = list!!.indexOf(lens)
            if (-1 == index) {
                list!!.add(0, lens)
                val dao = TrisquelDao(this.context)
                dao.connection()
                val id = dao.addLens(lens)
                dao.close()
                lens.id = id.toInt()
                lensRecyclerViewAdapter!!.notifyItemInserted(0)
                mView?.layoutManager?.scrollToPosition(0)
            }
        }
    }

    fun updateLens(lens: LensSpec) {
        if (list != null) {
            for (i in list!!.indices) {
                val c = list!![i]
                if (list!![i].id == lens.id) {
                    list!!.removeAt(i)
                    list!!.add(i, lens)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.updateLens(lens)
                    dao.close()
                    lensRecyclerViewAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    fun deleteLens(id: Int) {
        if (list != null) {
            for (i in list!!.indices) {
                val l = list!![i]
                if (l.id == id) {
                    list!!.removeAt(i)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.deleteLens(id)
                    dao.close()
                    lensRecyclerViewAdapter!!.notifyItemRemoved(i)
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
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: LensSpec, isLong: Boolean)
    }

    companion object {

        // TODO: Customize parameter argument names
        private val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        fun newInstance(columnCount: Int): LensFragment {
            val fragment = LensFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
