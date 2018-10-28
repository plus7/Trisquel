package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
class PhotoFragment : Fragment() {
    //private val id: Int = 0
    // TODO: Customize parameters
    private var mColumnCount = 1
    private var mFilmRollId = -1
    private val mFilmRoll: FilmRoll? = null
    private var mPhotos: ArrayList<Photo>? = null
    private var mListener: OnListFragmentInteractionListener? = null
    private var photoRecyclerViewAdapter: MyPhotoRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
        mFilmRollId = arguments?.getInt(ARG_FILMROLL_ID) ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_photo_list, container, false)

        val dao = TrisquelDao(this.context)
        dao.connection()
        //mFilmRoll = dao.getFilmRoll(mFilmRollId);
        mPhotos = dao.getPhotosByFilmRollId(mFilmRollId)
        dao.close()

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            if (mColumnCount <= 1) {
                view.layoutManager = LinearLayoutManager(context)
            } else {
                view.layoutManager = GridLayoutManager(context, mColumnCount)
            }
            this.photoRecyclerViewAdapter = MyPhotoRecyclerViewAdapter(mPhotos!!, mFilmRollId, mListener)
            view.adapter = photoRecyclerViewAdapter
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

    fun insertPhoto(p: Photo) {
        if (mPhotos != null) {
            val index: Int
            val dao = TrisquelDao(this.context)
            dao.connection()
            if (p.index == -1) {
                index = mPhotos!!.size
                p.index = index
            } else {
                index = p.index
                // 挿入する前にこれ以降のインデックスをずらす
                for (i in mPhotos!!.size - 1 downTo index) {
                    val followingPhoto = mPhotos!![i]
                    followingPhoto.index = followingPhoto.index + 1
                    dao.updatePhoto(followingPhoto)
                    photoRecyclerViewAdapter!!.notifyItemChanged(i)
                }
            }
            mPhotos!!.add(index, p)
            val id = dao.addPhoto(p)
            p.id = id.toInt()
            photoRecyclerViewAdapter!!.notifyItemInserted(index)
            dao.close()
        }
    }

    fun updatePhoto(p: Photo) {
        if (mPhotos != null) {
            for (i in mPhotos!!.indices) {
                if (mPhotos!![i].id == p.id) {
                    mPhotos!!.removeAt(i)
                    mPhotos!!.add(i, p)
                    val dao = TrisquelDao(this.context)
                    dao.connection()
                    dao.updatePhoto(p)
                    dao.close()
                    photoRecyclerViewAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    fun deletePhoto(id: Int) {
        var deletedIndex = -1
        if (mPhotos != null) {
            val dao = TrisquelDao(this.context)
            dao.connection()
            for (i in mPhotos!!.indices) {
                if (mPhotos!![i].id == id) {
                    deletedIndex = i
                    mPhotos!!.removeAt(i)
                    dao.deletePhoto(id)
                    photoRecyclerViewAdapter!!.notifyItemRemoved(i)
                    break
                }
            }
            //削除した写真以降のインデックスをずらす
            for (i in deletedIndex until mPhotos!!.size) {
                val followingPhoto = mPhotos!![i]
                followingPhoto.index = followingPhoto.index - 1
                dao.updatePhoto(followingPhoto)
                photoRecyclerViewAdapter!!.notifyItemChanged(i)
            }
            dao.close()
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
        fun onListFragmentInteraction(item: Photo, isLong: Boolean)
    }

    companion object {

        // TODO: Customize parameter argument names
        private val ARG_COLUMN_COUNT = "column-count"
        private val ARG_FILMROLL_ID = "filmroll_id"

        // TODO: Customize parameter initialization
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
