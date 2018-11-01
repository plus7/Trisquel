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
    private var mPhotos = ArrayList<Photo>()
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
            this.photoRecyclerViewAdapter = MyPhotoRecyclerViewAdapter(mPhotos, mFilmRollId, mListener)
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

    private fun getInsertPos(frameIndex: Int): Int{
        for(i in mPhotos.indices){
            if(mPhotos[i].frameIndex > frameIndex) return i
        }
        return mPhotos.size
    }

    fun possibleDownShiftLimit(p: Photo): Int{
        var curpos = curPosOf(p)
        if(curpos > 0){
            val prev = mPhotos[curpos - 1]
            return prev.frameIndex
        }else{
            return 0
        }
    }

    private fun curPosOf(p: Photo): Int{
        var curpos = -1
        for(i in mPhotos.indices){
            if(mPhotos[i].id == p.id){
                curpos = i
                break
            }
        }
        return curpos
    }

    fun shiftFrameIndexFrom(p: Photo, amount: Int){
        if(p.frameIndex + amount < possibleDownShiftLimit(p)) throw Exception()
        val curpos = curPosOf(p)
        val dao = TrisquelDao(this.context)
        dao.connection()
        for(i in curpos..(mPhotos.size-1)){
            mPhotos[i].frameIndex += amount
            dao.updatePhoto(mPhotos[i])
            photoRecyclerViewAdapter!!.notifyItemChanged(i)
        }
        dao.close()
    }

    fun insertPhoto(p: Photo) {
            val pos: Int
            val dao = TrisquelDao(this.context)
            dao.connection()
            //TODO: ここがまずい
            if (p.frameIndex == -1) {
                pos = mPhotos.size
                p.frameIndex = if(mPhotos.isEmpty()) 0 else mPhotos.last().frameIndex + 1
            } else {
                pos = getInsertPos(p.frameIndex)
                //index = p.frameIndex
                // 挿入する前にこれ以降のインデックスをずらす
                /*
                for (i in mPhotos!!.size - 1 downTo index) {
                    val followingPhoto = mPhotos!![i]
                    followingPhoto.frameIndex = followingPhoto.frameIndex + 1
                    dao.updatePhoto(followingPhoto)
                    photoRecyclerViewAdapter!!.notifyItemChanged(i)
                }
                */
            }
            mPhotos.add(pos, p)
            val id = dao.addPhoto(p)
            p.id = id.toInt()
            photoRecyclerViewAdapter!!.notifyItemInserted(pos)
            dao.close()
    }

    fun updatePhoto(p: Photo) {
        for (i in mPhotos.indices) {
            if (mPhotos[i].id == p.id) {
                val newpos = getInsertPos(p.frameIndex)
                val curpos = i
                mPhotos.removeAt(curpos)
                if(newpos > curpos)
                    mPhotos.add(newpos - 1, p)
                else
                    mPhotos.add(newpos, p)
                val dao = TrisquelDao(this.context)
                dao.connection()
                dao.updatePhoto(p)
                dao.close()
                if(newpos != curpos) {
                    photoRecyclerViewAdapter!!.notifyItemMoved(curpos, newpos)
                    photoRecyclerViewAdapter!!.notifyItemChanged(newpos)
                    //日付をグルーピングしているように見える小細工がある都合で一個下にも通知が必要
                    if (newpos != mPhotos.lastIndex) photoRecyclerViewAdapter!!.notifyItemChanged(newpos + 1)
                }
                photoRecyclerViewAdapter!!.notifyItemChanged(curpos)
                //ここも同様
                if(curpos != mPhotos.lastIndex) photoRecyclerViewAdapter!!.notifyItemChanged(curpos + 1)
            }
        }
    }

    fun deletePhoto(id: Int) {
        var deletedPos = -1
        val dao = TrisquelDao(this.context)
        dao.connection()
        for (i in mPhotos.indices) {
            if (mPhotos[i].id == id) {
                deletedPos = i
                mPhotos.removeAt(i)
                dao.deletePhoto(id)
                photoRecyclerViewAdapter!!.notifyItemRemoved(i)
                //日付をグルーピングしているように見える小細工がある都合で一個下にも通知が必要
                //上でremoveAtしてるから以下の判定式ではmPhoto.size-1ではなくmPhoto.sizeを使わなければならない
                if(i != mPhotos.size) photoRecyclerViewAdapter!!.notifyItemChanged(i)
                break
            }
        }
        //削除した写真以降のインデックスをずらす
        //TODO: ここもよろしくない
        /*for (i in deletedIndex until mPhotos!!.size) {
            val followingPhoto = mPhotos!![i]
            followingPhoto.frameIndex = followingPhoto.frameIndex - 1
            dao.updatePhoto(followingPhoto)
            photoRecyclerViewAdapter!!.notifyItemChanged(i)
        }*/
        dao.close()
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
        fun onThumbnailClick(item: Photo)
        fun onIndexClick(item: Photo)
        fun onIndexLongClick(item: Photo)
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
