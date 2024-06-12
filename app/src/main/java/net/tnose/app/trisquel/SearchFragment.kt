package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [FavoritePhotoFragment.OnListFragmentInteractionListener] interface.
 */
class SearchFragment : androidx.fragment.app.Fragment() {
    private var mTags: ArrayList<String> = arrayListOf()
    private var mListener: OnListFragmentInteractionListener? = null
    private var searchRecyclerViewAdapter: MySearchRecyclerViewAdapter? = null
    private var mSearchViewModel: SearchViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mTags = it.getStringArrayList("tags") ?: arrayListOf()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search_list, container, false)

        // Set the adapter
        if (view is androidx.recyclerview.widget.RecyclerView) {
            val context = view.getContext()
            view.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            this.searchRecyclerViewAdapter = MySearchRecyclerViewAdapter(MySearchRecyclerViewAdapter.PhotoDiff(), mListener)
            view.adapter = searchRecyclerViewAdapter
            mSearchViewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
            mSearchViewModel!!.searchTags.value = mTags.toList()
            mSearchViewModel!!.photosByAndQuery.observe(viewLifecycleOwner) { photos ->
                searchRecyclerViewAdapter!!.submitList(photos)
            }
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

    interface OnListFragmentInteractionListener {
        //fun onListFragmentInteraction(item: Photo?, list: List<Photo?>)
        fun onListFragmentInteraction(item: Photo, isLong: Boolean)
        fun onThumbnailClick(item: Photo)
        fun onIndexClick(item: Photo)
        fun onIndexLongClick(item: Photo)
        fun onFavoriteClick(item: Photo)
    }

    fun toggleFavPhoto(p: Photo){
        mSearchViewModel!!.update(p.toEntity())
    }

    // いろいろ面倒そうなのでとりあえずインデックスの編集を許さないことにする
    fun updatePhoto(p: Photo, tags: java.util.ArrayList<String>?) {
        if(tags != null){
            mSearchViewModel!!.tagPhoto(p.id, p.filmrollid, tags)
        }
        mSearchViewModel!!.update(p.toEntity())
    }

    fun deletePhoto(id: Int) {
        mSearchViewModel!!.delete(id)
    }

    companion object {
        @JvmStatic
        fun newInstance(tags: ArrayList<String>) =
                SearchFragment().apply {
                    arguments = Bundle().apply {
                        putStringArrayList("tags", tags)
                    }
                }
    }
}
