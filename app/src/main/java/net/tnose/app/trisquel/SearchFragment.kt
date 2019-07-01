package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.chuross.recyclerviewadapters.CompositeRecyclerAdapter


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [FavoritePhotoFragment.OnListFragmentInteractionListener] interface.
 */
class SearchFragment : androidx.fragment.app.Fragment() {
    private var mTags: ArrayList<String> = arrayListOf()
    private var listener: OnListFragmentInteractionListener? = null
    private var mCompositeAdapter: CompositeRecyclerAdapter? = null
    private var mLocalAdapters: HashMap<Int, SearchLocalAdapter> = HashMap()

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
            with(view) {
                val dao = TrisquelDao(this.context)
                dao.connection()
                val mytags = ArrayList(mTags.map{ dao.getTagByLabel(it) }.filterNotNull())
                val list = dao.getPhotosByAndQuery(mytags)

                val map = list.groupBy { it.filmrollid }
                val list2 = map.values.sortedByDescending { it[0].date }
                val compositeAdapter = CompositeRecyclerAdapter()

                val llm = androidx.recyclerview.widget.LinearLayoutManager(context)

                for(l in list2) {
                    val localAdapter = SearchLocalAdapter(context, listener)
                    val sortedList = l.sortedBy{ it.frameIndex }
                    localAdapter.addAll(sortedList)
                    mLocalAdapters.put(l[0].filmrollid, localAdapter)
                    localAdapter.setOnItemClickListener { viewHolder, i, photo ->
                        listener?.onListFragmentInteraction(photo, false)
                    }
                    localAdapter.setOnItemLongPressListener { viewHolder, i, photo ->
                        listener?.onListFragmentInteraction(photo, true)
                    }
                    val filmrollName = dao.getFilmRoll(l[0].filmrollid)!!.name
                    val header = SearchHeader(context, filmrollName)
                    compositeAdapter.add(header)
                    compositeAdapter.add(localAdapter)
                }
                dao.close()
                layoutManager = llm
                adapter = compositeAdapter
                mCompositeAdapter = compositeAdapter
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

    interface OnListFragmentInteractionListener {
        //fun onListFragmentInteraction(item: Photo?, list: List<Photo?>)
        fun onListFragmentInteraction(item: Photo, isLong: Boolean)
        fun onThumbnailClick(item: Photo)
        fun onIndexClick(item: Photo)
        fun onIndexLongClick(item: Photo)
        fun onFavoriteClick(item: Photo)
    }

    fun toggleFavPhoto(p: Photo){
        var curpos = -1
        val localAdapter = mLocalAdapters[p.filmrollid]
        if(localAdapter == null) return
        for (i in 0.until(localAdapter.itemCount)) {
            if(localAdapter[i].id == p.id){
                curpos = i
                break
            }
        }
        val dao = TrisquelDao(this.context)
        dao.connection()
        dao.updatePhoto(p)
        dao.close()
        localAdapter.notifyItemChanged(curpos, p.favorite)
    }

    private fun getInsertPos(frameIndex: Int): Int{
        /*for(i in mPhotos.indices){
            if(mPhotos[i].frameIndex > frameIndex) return i
        }
        return mPhotos.size*/
        return -1
    }

    // いろいろ面倒そうなのでとりあえずインデックスの編集を許さないことにする
    fun updatePhoto(p: Photo, tags: java.util.ArrayList<String>?) {
        val localAdapter = mLocalAdapters[p.filmrollid]
        if(localAdapter == null) return
        for (i in 0.until(localAdapter.itemCount)) {
            if (localAdapter[i].id == p.id) {
                //val newpos = getInsertPos(p.frameIndex)
                val curpos = i
                localAdapter.remove(localAdapter[i])
                localAdapter.add(curpos, p)
                /*if(newpos > curpos)
                    localAdapter.add(newpos - 1, p)
                else
                    localAdapter.add(newpos, p)*/
                val dao = TrisquelDao(this.context)
                dao.connection()
                dao.updatePhoto(p)
                if(tags != null) dao.tagPhoto(p.id, p.filmrollid, tags)
                dao.close()
                /*if(newpos < curpos || newpos > curpos+1) {
                    localAdapter.notifyItemMoved(curpos, newpos)
                    localAdapter.notifyItemChanged(newpos)
                    //日付をグルーピングしているように見える小細工がある都合で一個下にも通知が必要
                    if (newpos != mPhotos.lastIndex) localAdapter.notifyItemChanged(newpos + 1)
                }*/
                localAdapter.notifyItemChanged(curpos)
                //ここも同様
                //if(curpos != mPhotos.lastIndex) localAdapter.notifyItemChanged(curpos + 1)
            }
        }
    }

    fun deletePhoto(id: Int) {
        var deletedPos = -1
        val dao = TrisquelDao(this.context)
        dao.connection()

        val p = dao.getPhoto(id)
        if(p == null) { dao.close(); return }

        val localAdapter = mLocalAdapters[p.filmrollid]
        if(localAdapter == null) return

        for (i in 0.until(localAdapter.itemCount)) {
            if (localAdapter[i].id == id) {
                deletedPos = i
                localAdapter.remove(localAdapter[i])
                dao.deletePhoto(id)
                // localAdapter.notifyItemRemoved(i) //不要？
                // 日付をグルーピングしているように見える小細工がある都合で一個下にも通知が必要
                // 上でremoveしてるから以下の判定式ではlocalAdapter.itemCount-1ではなく
                // localAdapter.itemCountを使わなければならない
                if(i != localAdapter.itemCount) localAdapter.notifyItemChanged(i)
                break
            }
        }

        dao.close()
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
