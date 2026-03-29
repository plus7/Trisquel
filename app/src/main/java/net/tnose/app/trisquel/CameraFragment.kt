package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager

class CameraFragment : androidx.fragment.app.Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null
    private val cameraList = mutableStateListOf<CameraSpec>()
    private var scrollTargetIndex: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val dao = TrisquelDao(this.context)
        dao.connection()
        val list = dao.allCameras
        dao.close()

        cameraList.clear()
        cameraList.addAll(list)

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        val key  = pref.getInt("camera_sortkey", 0)
        changeSortKey(key)

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    CameraListScreen(
                        cameras = cameraList,
                        onItemClick = { mListener?.onListFragmentInteraction(it, false) },
                        onItemLongClick = { mListener?.onListFragmentInteraction(it, true) },
                        emptyMessage = getString(R.string.warning_cam_not_registered),
                        scrollTargetIndex = scrollTargetIndex,
                        onScrollConsumed = { scrollTargetIndex = null }
                    )
                }
            }
        }
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

    fun changeSortKey(key: Int) {
        val sortedList = when (key) {
            0 -> cameraList.sortedByDescending { it.created }
            1 -> cameraList.sortedBy { it.manufacturer + " " + it.modelName }
            2 -> cameraList.sortedBy { it.mount }
            3 -> cameraList.sortedBy { it.format }
            else -> cameraList.toList()
        }
        cameraList.clear()
        cameraList.addAll(sortedList)
    }

    fun insertCamera(camera: CameraSpec) {
        val index = cameraList.indexOf(camera)
        if (-1 == index) {
            cameraList.add(0, camera)
            val dao = TrisquelDao(this.context)
            dao.connection()
            val id = dao.addCamera(camera)
            dao.close()
            camera.id = id.toInt()
            scrollTargetIndex = 0
        }
    }

    fun updateCamera(camera: CameraSpec) {
        val i = cameraList.indexOfFirst { it.id == camera.id }
        if (i != -1) {
            cameraList[i] = camera
            val dao = TrisquelDao(this.context)
            dao.connection()
            dao.updateCamera(camera)
            dao.close()
        }
    }

    fun deleteCamera(id: Int) {
        val i = cameraList.indexOfFirst { it.id == id }
        if (i != -1) {
            val c = cameraList[i]
            cameraList.removeAt(i)
            val dao = TrisquelDao(this.context)
            dao.connection()
            if (c.type == 1) {
                dao.deleteLens(dao.getFixedLensIdByBody(id))
            }
            dao.deleteCamera(id)
            dao.close()
        }
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: CameraSpec, isLong: Boolean)
    }

    companion object {
        fun newInstance(columnCount: Int): CameraFragment {
            val fragment = CameraFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CameraListScreen(
    cameras: List<CameraSpec>,
    onItemClick: (CameraSpec) -> Unit,
    onItemLongClick: (CameraSpec) -> Unit,
    emptyMessage: String,
    scrollTargetIndex: Int?,
    onScrollConsumed: () -> Unit
) {
    if (cameras.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val listState = rememberLazyListState()

        LaunchedEffect(scrollTargetIndex) {
            if (scrollTargetIndex != null && scrollTargetIndex < cameras.size) {
                listState.animateScrollToItem(scrollTargetIndex)
                onScrollConsumed()
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(cameras, key = { it.id }) { camera ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(camera) },
                            onLongClick = { onItemLongClick(camera) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "${camera.manufacturer} ${camera.modelName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
