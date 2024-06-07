package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.chuross.recyclerviewadapters.CompositeRecyclerAdapter
import com.github.chuross.recyclerviewadapters.SpanSizeLookupBuilder


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [FavoritePhotoFragment.OnListFragmentInteractionListener] interface.
 */
class FavoritePhotoFragment : androidx.fragment.app.Fragment() {
    private var columnCount = 3

    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_photo_list, container, false)

        // Set the adapter
        if (view is androidx.recyclerview.widget.RecyclerView) {
            with(view) {
                val dao = TrisquelDao(this.context)
                dao.connection()
                val list = dao.getAllFavedPhotos()

                val map = list.groupBy { it.filmrollid }
                val list2 = map.values.sortedByDescending { it[0].date }
                val compositeAdapter = CompositeRecyclerAdapter()

                val glm = androidx.recyclerview.widget.GridLayoutManager(context, columnCount)
                val spans = SpanSizeLookupBuilder(compositeAdapter)

                for(l in list2) {
                    val localAdapter = FavPhotoLocalAdapter(context)
                    val sortedList = l.sortedBy { it.frameIndex }
                    localAdapter.addAll(sortedList)
                    localAdapter.setOnItemClickListener { viewHolder, i, photo ->
                        listener?.onListFragmentInteraction(photo, sortedList)
                    }
                    val filmrollName = dao.getFilmRoll(l[0].filmrollid)!!.name
                    val header = FavPhotoHeader(context, filmrollName)
                    compositeAdapter.add(header)
                    spans.register(header, columnCount)
                    compositeAdapter.add(localAdapter)
                }
                dao.close()
                glm.spanSizeLookup = spans.build()
                layoutManager = glm
                adapter = compositeAdapter //MyFavPhotoRecyclerViewAdapter(list, listener)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Photo?, list: List<Photo?>)
    }

    companion object {
        const val ARG_COLUMN_COUNT = "column-count"
        @JvmStatic
        fun newInstance(columnCount: Int) =
                FavoritePhotoFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }
}
