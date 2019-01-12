package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
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
class FilmRollFragment : Fragment() {
    // TODO: Customize parameters
    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mRecyclerView: RecyclerViewEmptySupport? = null
    private var list: ArrayList<FilmRoll>? = null
    private var filmrollRecyclerViewAdapter: MyFilmRollRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_filmroll_list, container, false)
        val dao = TrisquelDao(this.context)
        dao.connection()
        list = dao.allFilmRolls
        dao.close()

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.context)
        val key  = pref.getInt("filmroll_sortkey", 0)
        changeSortKey(key)

        // Set the adapter
        if (view is RecyclerViewEmptySupport) {
            val context = view.getContext()
            mRecyclerView = view
            mRecyclerView!!.setEmptyMessage(getString(R.string.warning_filmroll_not_registered))
            mRecyclerView!!.setEmptyView(container!!.findViewById(R.id.empty_view))
            if (mColumnCount <= 1) {
                mRecyclerView!!.layoutManager = LinearLayoutManager(context)
            } else {
                mRecyclerView!!.layoutManager = GridLayoutManager(context, mColumnCount)
            }

            filmrollRecyclerViewAdapter = MyFilmRollRecyclerViewAdapter(list!!, mListener)
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRecyclerView!!.adapter = filmrollRecyclerViewAdapter
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
                1 -> {list!!.sortBy { it.name }}
                2 -> {list!!.sortBy { it.camera.manufacturer + " " + it.camera.modelName }}
                3 -> {list!!.sortBy { it.manufacturer + " " + it.brand }}
                else -> {}
            }
            filmrollRecyclerViewAdapter?.notifyDataSetChanged()
        }
    }

    fun insertFilmRoll(filmroll: FilmRoll) {
        if (list != null) {
            val index = list!!.indexOf(filmroll)
            if (-1 == index) {
                list!!.add(0, filmroll)
                val dao = TrisquelDao(this.context)
                dao.connection()
                val id = dao.addFilmRoll(filmroll)
                filmroll.id = id.toInt()
                dao.close()
                Log.d("FilmRollFragment", "notifyItemInserted")
                filmrollRecyclerViewAdapter!!.notifyItemInserted(0)
                mRecyclerView?.layoutManager?.scrollToPosition(0)
            }
        }
    }

    fun updateFilmRoll(filmroll: FilmRoll) {
        if (list != null) {
            for (i in list!!.indices) {
                val c = list!![i]
                if (list!![i].id == filmroll.id) {
                    list!!.removeAt(i)
                    list!!.add(i, filmroll)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.updateFilmRoll(filmroll)
                    val p = dao.getPhotosByFilmRollId(filmroll.id)
                    filmroll.photos = p
                    dao.close()
                    filmrollRecyclerViewAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    fun refreshFilmRoll(id: Int) {
        if (list != null) {
            for (i in list!!.indices) {
                val c = list!![i]
                if (list!![i].id == id) {
                    list!!.removeAt(i)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    val f = dao.getFilmRoll(id)
                    val p = dao.getPhotosByFilmRollId(id)
                    f!!.photos = p
                    dao.close()
                    list!!.add(i, f)
                    Log.d("refreshFilmRoll", Integer.toString(id))
                    filmrollRecyclerViewAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    fun deleteFilmRoll(id: Int) {
        if (list != null) {
            for (i in list!!.indices) {
                val c = list!![i]
                if (list!![i].id == id) {
                    list!!.removeAt(i)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.deleteFilmRoll(id)
                    dao.close()
                    Log.d("deleteFilmRoll", Integer.toString(id))
                    filmrollRecyclerViewAdapter!!.notifyItemRemoved(i)
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
        fun onListFragmentInteraction(item: FilmRoll, isLong: Boolean)
    }

    companion object {

        // TODO: Customize parameter argument names
        private val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        fun newInstance(columnCount: Int): FilmRollFragment {
            val fragment = FilmRollFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
