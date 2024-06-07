package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
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
class PhotoFragment : androidx.fragment.app.Fragment() {
    private var mColumnCount = 1
    private var mFilmRollId = -1
    private val mFilmRoll: FilmRoll? = null
    private var mListener: OnListFragmentInteractionListener? = null
    private var photoRecyclerViewAdapter: MyPhotoRecyclerViewAdapter? = null
    private var mPhotoViewModel: PhotoViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
        mFilmRollId = arguments?.getInt(ARG_FILMROLL_ID) ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_photo_list, container, false)

        // Set the adapter
        if (view is androidx.recyclerview.widget.RecyclerView) {
            val context = view.getContext()
            if (mColumnCount <= 1) {
                view.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            } else {
                view.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, mColumnCount)
            }
            this.photoRecyclerViewAdapter = MyPhotoRecyclerViewAdapter(MyPhotoRecyclerViewAdapter.PhotoDiff(), mFilmRollId, mListener)
            view.adapter = photoRecyclerViewAdapter
            mPhotoViewModel = ViewModelProvider(this).get(PhotoViewModel::class.java)
            mPhotoViewModel!!.filmRollId.value = mFilmRollId
            mPhotoViewModel!!.photosByFilmRollId.observe(viewLifecycleOwner) { photos ->
                photoRecyclerViewAdapter!!.submitList(photos)
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

    /*
    private fun getInsertPos(frameIndex: Int): Int{
        for(i in mPhotos.indices){
            if(mPhotos[i].frameIndex > frameIndex) return i
        }
        return mPhotos.size
    }

     */

    fun possibleDownShiftLimit(p: Photo): Int{
        var curpos = curPosOf(p)
        val list = mPhotoViewModel!!.photosByFilmRollId.value!!
        if(curpos > 0){
            val prev = list[curpos - 1]
            return prev.second._index!!
        }else{
            return 0
        }
    }

    private fun curPosOf(p: Photo): Int{
        var curpos = -1
        val list = mPhotoViewModel!!.photosByFilmRollId.value!!
        for(i in list.indices){
            if(list[i].second.id == p.id){
                curpos = i
                break
            }
        }
        return curpos
    }

    fun shiftFrameIndexFrom(p: Photo, amount: Int){
        if(p.frameIndex + amount < possibleDownShiftLimit(p)) throw Exception()

        val curpos = curPosOf(p)
        val list = mPhotoViewModel!!.photosByFilmRollId.value!!
        for(i in curpos..(list.size-1)){
            var p = list[i].second.copy(_index = list[i].second._index!! + amount)
            mPhotoViewModel!!.update(p)
        }
    }

    fun insertPhoto(p: Photo, tags: ArrayList<String>?) {
        if(tags != null){
            val dao = TrisquelDao(this.context)
            dao.connection()
            dao.updatePhoto(p)
            dao.tagPhoto(p.id, mFilmRollId, tags)
            dao.close()
        }
        if (p.frameIndex == -1) {
            val list = mPhotoViewModel!!.photosByFilmRollId.value
            p.frameIndex = if(list.isNullOrEmpty()) 0 else (list.last().second._index ?: 0) + 1
        }
        mPhotoViewModel!!.insert(p.toEntity())
    }

    fun toggleFavPhoto(p: Photo){
        mPhotoViewModel!!.update(p.toEntity())
    }

    fun updatePhoto(p: Photo, tags: ArrayList<String>?) {
        if(tags != null){
            val dao = TrisquelDao(this.context)
            dao.connection()
            dao.updatePhoto(p)
            dao.tagPhoto(p.id, mFilmRollId, tags)
            dao.close()
        }
        mPhotoViewModel!!.update(p.toEntity())
    }

    fun deletePhoto(id: Int) {
        mPhotoViewModel!!.delete(id)
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
        fun onListFragmentInteraction(item: Photo, isLong: Boolean)
        fun onThumbnailClick(item: Photo)
        fun onIndexClick(item: Photo)
        fun onIndexLongClick(item: Photo)
        fun onFavoriteClick(item: Photo)
    }

    companion object {

        private val ARG_COLUMN_COUNT = "column-count"
        private val ARG_FILMROLL_ID = "filmroll_id"

        fun newInstance(columnCount: Int, filmRollId: Int): PhotoFragment {
            val fragment = PhotoFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            args.putInt(ARG_FILMROLL_ID, filmRollId)
            fragment.arguments = args
            return fragment
        }
    }
}
