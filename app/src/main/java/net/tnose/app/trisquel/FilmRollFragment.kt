package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
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
class FilmRollFragment : androidx.fragment.app.Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null
    private var mRecyclerView: RecyclerViewEmptySupport? = null
    private var filmrollRecyclerViewAdapter: MyFilmRollRecyclerViewAdapter? = null
    private var mFilmRollViewModel: FilmRollViewModel? = null
    private var _currentFilter:Pair<Int, ArrayList<String>> = Pair(0, arrayListOf(""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filtertype = arguments?.getInt(ARG_FILTER_TYPE) ?: 0
        val filterstr = arguments?.getStringArrayList(ARG_FILTER_VALUE) ?: arrayListOf("")
        _currentFilter = Pair(filtertype, filterstr)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_filmroll_list, container, false)

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.context)
        val key  = pref.getInt("filmroll_sortkey", 0)

        // Set the adapter
        if (view is RecyclerViewEmptySupport) {
            val context = view.getContext()
            mRecyclerView = view
            mRecyclerView!!.setEmptyMessage(getString(R.string.warning_filmroll_not_registered))
            mRecyclerView!!.setEmptyView(container!!.findViewById(R.id.empty_view))
            mRecyclerView!!.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

            filmrollRecyclerViewAdapter = MyFilmRollRecyclerViewAdapter(MyFilmRollRecyclerViewAdapter.FilmRollDiff(), mListener)
            view.adapter = filmrollRecyclerViewAdapter

            mFilmRollViewModel = ViewModelProvider(this).get(FilmRollViewModel::class.java)
            mFilmRollViewModel!!.allFilmRollAndRels.observe(viewLifecycleOwner) { filmrollandrels ->
                filmrollRecyclerViewAdapter!!.submitList(filmrollandrels)
            }
            changeSortKey(key)
        }
        return view
    }

    //これ消さないと落ちる
    /*override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRecyclerView!!.adapter = filmrollRecyclerViewAdapter
    }*/

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

    // 0: No filtering, 1: Filter by camera, 2: Filter by film brand
    // そのうち詳細なフィルタリングにも対応する
    var currentFilter:Pair<Int, ArrayList<String>>
        get() = _currentFilter //filmrollRecyclerViewAdapter?.currentFilter ?: Pair<Int, String>(0, "")
        set(value) {
            /*
            TODO: 当分触らない
            val dao = TrisquelDao(this.context)
            dao.connection()
            list = when(value.first){
                0 -> dao.allFilmRolls
                1 -> dao.getFilmRollsByCamera(value.second[0].toInt())
                2 -> dao.getFilmRollsByFilmBrand(value.second[0], value.second[1])
                else -> dao.allFilmRolls
            }
            dao.close()

            val pref = PreferenceManager.getDefaultSharedPreferences(this.context)
            val key  = pref.getInt("filmroll_sortkey", 0)
            changeSortKey(key)

            filmrollRecyclerViewAdapter = MyFilmRollRecyclerViewAdapter(list!!, mListener)

            mRecyclerView?.adapter = filmrollRecyclerViewAdapter
            _currentFilter = value
            //if(list != null){
            //    filmrollRecyclerViewAdapter?.setFilter(value.first, value.second)
            //}
            
             */
        }

    fun changeSortKey(key: Int){
        /*
        TODO: 当分触らない
        if(list != null){
            when(key){
                0 -> {list!!.sortByDescending { it.created }}
                1 -> {list!!.sortBy { it.name }}
                2 -> {list!!.sortBy { it.camera.manufacturer + " " + it.camera.modelName }}
                3 -> {list!!.sortBy { it.manufacturer + " " + it.brand }}
                else -> {}
            }
            filmrollRecyclerViewAdapter?.notifyDataSetChanged()
        }*
        
         */
    }
    fun insertFilmRoll(FilmRoll: FilmRoll) {
        mFilmRollViewModel!!.insert(FilmRoll.toEntity())
    }

    fun updateFilmRoll(FilmRoll: FilmRoll) {
        mFilmRollViewModel!!.update(FilmRoll.toEntity())
    }

    fun deleteFilmRoll(id: Int) {
        mFilmRollViewModel!!.delete(id)
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
        fun onListFragmentInteraction(item: FilmRoll, isLong: Boolean)
    }

    companion object {
        private val ARG_FILTER_TYPE = "filter-type"
        private val ARG_FILTER_VALUE = "filter-val"

        fun newInstance(filterType: Int, filterValues: ArrayList<String>): FilmRollFragment {
            val fragment = FilmRollFragment()
            val args = Bundle()
            args.putInt(ARG_FILTER_TYPE, filterType)
            args.putStringArrayList(ARG_FILTER_VALUE, filterValues)
            fragment.arguments = args
            return fragment
        }
    }
}
