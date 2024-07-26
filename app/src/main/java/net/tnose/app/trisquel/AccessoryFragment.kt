package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager


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
class AccessoryFragment : androidx.fragment.app.Fragment() {
    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mView: RecyclerViewEmptySupport? = null
    private var accessoryRecyclerViewAdapter: MyAccessoryRecyclerViewAdapter? = null
    private var mAccessoryViewModel: AccessoryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_accessory_list, container, false)

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        val key  = pref.getInt("accessory_sortkey", 0)

        // Set the adapter
        if (view is RecyclerViewEmptySupport) {
            mView = view
            val context = view.getContext()
            view.setEmptyMessage(getString(R.string.warning_accessory_not_registered))
            val emptyView : View = container?.findViewById(R.id.empty_view)!!
            view.setEmptyView(emptyView)
            if (mColumnCount <= 1) {
                view.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            } else {
                view.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, mColumnCount)
            }
            this.accessoryRecyclerViewAdapter = MyAccessoryRecyclerViewAdapter(MyAccessoryRecyclerViewAdapter.AccessoryDiff(), mListener)
            view.adapter = accessoryRecyclerViewAdapter

            mAccessoryViewModel = ViewModelProvider(this).get(AccessoryViewModel::class.java)
            mAccessoryViewModel!!.allAccessories.observe(viewLifecycleOwner) { accessories ->
                accessoryRecyclerViewAdapter!!.submitList(accessories)
            }
            changeSortKey(key)
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
        mAccessoryViewModel!!.sortingRule.value = key
    }

    fun insertAccessory(accessory: Accessory) {
        mAccessoryViewModel!!.insert(accessory.toEntity())
    }

    fun updateAccessory(accessory: Accessory) {
        mAccessoryViewModel!!.update(accessory.toEntity())
    }

    fun deleteAccessory(id: Int) {
        mAccessoryViewModel!!.delete(id)
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
        fun onListFragmentInteraction(accessory: Accessory, isLong: Boolean)
    }

    companion object {
        private val ARG_COLUMN_COUNT = "column-count"

        fun newInstance(columnCount: Int): AccessoryFragment {
            val fragment = AccessoryFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
