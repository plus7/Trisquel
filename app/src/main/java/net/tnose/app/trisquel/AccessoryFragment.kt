package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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
class AccessoryFragment : Fragment() {
    // TODO: Customize parameters
    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mView: RecyclerViewEmptySupport? = null
    private var accessoryRecyclerViewAdapter: MyAccessoryRecyclerViewAdapter? = null
    private var list:MutableList<Accessory>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_accessory_list, container, false)

        val dao = TrisquelDao(this.context)
        dao.connection()
        list = dao.accessories
        dao.close()

        // Set the adapter
        if (view is RecyclerViewEmptySupport) {
            mView = view
            val context = view.getContext()
            view.setEmptyMessage(getString(R.string.warning_accessory_not_registered))
            val emptyView : View = container?.findViewById(R.id.empty_view)!!
            view.setEmptyView(emptyView)
            if (mColumnCount <= 1) {
                view.layoutManager = LinearLayoutManager(context)
            } else {
                view.layoutManager = GridLayoutManager(context, mColumnCount)
            }
            this.accessoryRecyclerViewAdapter = MyAccessoryRecyclerViewAdapter(list!!, mListener)
            view.adapter = accessoryRecyclerViewAdapter
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

    fun insertAccessory(accessory: Accessory) {
        if (list != null) {
            val index = list!!.indexOf(accessory)
            if (-1 == index) {
                list!!.add(0, accessory)
                val dao = TrisquelDao(this.context)
                dao.connection()
                val id = dao.addAccessory(accessory)
                dao.close()
                accessory.id = id.toInt()
                accessoryRecyclerViewAdapter?.notifyItemInserted(0)
                mView?.layoutManager?.scrollToPosition(0)
            }
        }
    }

    fun updateAccessory(accessory: Accessory) {
        if (list != null) {
            for (i in list!!.indices) {
                val a = list!!.get(i)
                if (a.id == accessory.id) {
                    list!!.removeAt(i)
                    list!!.add(i, accessory)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.updateAccessory(accessory)
                    dao.close()
                    accessoryRecyclerViewAdapter?.notifyItemChanged(i)
                }
            }
        }
    }

    fun deleteAccessory(id: Int) {
        if (list != null) {
            for (i in list!!.indices) {
                val a = list!!.get(i)
                if (a.id == id) {
                    list!!.removeAt(i)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.deleteAccessory(id)
                    dao.close()
                    accessoryRecyclerViewAdapter?.notifyItemRemoved(i)
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
        fun onListFragmentInteraction(accessory: Accessory, isLong: Boolean)
    }

    companion object {

        // TODO: Customize parameter argument names
        private val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        fun newInstance(columnCount: Int): AccessoryFragment {
            val fragment = AccessoryFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
