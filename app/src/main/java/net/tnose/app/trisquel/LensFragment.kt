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

class LensFragment : androidx.fragment.app.Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null
    private val lensList = mutableStateListOf<LensSpec>()
    private var scrollTargetIndex: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dao = TrisquelDao(this.context)
        dao.connection()
        val list = dao.allVisibleLenses
        dao.close()

        lensList.clear()
        lensList.addAll(list)

        //ここでいいのか？
        val pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        val key = pref.getInt("lens_sortkey", 0)
        changeSortKey(key)

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    LensListScreen(
                        lenses = lensList,
                        onItemClick = { mListener?.onListFragmentInteraction(it, false) },
                        onItemLongClick = { mListener?.onListFragmentInteraction(it, true) },
                        emptyMessage = getString(R.string.warning_lens_not_registered),
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
            0 -> lensList.sortedByDescending { it.created }
            1 -> lensList.sortedBy { it.manufacturer + " " + it.modelName }
            2 -> lensList.sortedBy { it.mount }
            3 -> lensList.sortedBy { it.focalLengthRange.first }
            else -> lensList.toList()
        }
        lensList.clear()
        lensList.addAll(sortedList)
    }

    fun insertLens(lens: LensSpec) {
        val index = lensList.indexOf(lens)
        if (-1 == index) {
            lensList.add(0, lens)
            val dao = TrisquelDao(this.context)
            dao.connection()
            val id = dao.addLens(lens)
            dao.close()
            lens.id = id.toInt()
            scrollTargetIndex = 0
        }
    }

    fun updateLens(lens: LensSpec) {
        val i = lensList.indexOfFirst { it.id == lens.id }
        if (i != -1) {
            lensList[i] = lens
            val dao = TrisquelDao(this.context)
            dao.connection()
            dao.updateLens(lens)
            dao.close()
        }
    }

    fun deleteLens(id: Int) {
        val i = lensList.indexOfFirst { it.id == id }
        if (i != -1) {
            lensList.removeAt(i)
            val dao = TrisquelDao(this.context)
            dao.connection()
            dao.deleteLens(id)
            dao.close()
        }
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: LensSpec, isLong: Boolean)
    }

    companion object {
        fun newInstance(columnCount: Int): LensFragment {
            val fragment = LensFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LensListScreen(
    lenses: List<LensSpec>,
    onItemClick: (LensSpec) -> Unit,
    onItemLongClick: (LensSpec) -> Unit,
    emptyMessage: String,
    scrollTargetIndex: Int?,
    onScrollConsumed: () -> Unit
) {
    if (lenses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val listState = rememberLazyListState()

        LaunchedEffect(scrollTargetIndex) {
            if (scrollTargetIndex != null && scrollTargetIndex < lenses.size) {
                listState.animateScrollToItem(scrollTargetIndex)
                onScrollConsumed()
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(lenses, key = { it.id }) { lens ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(lens) },
                            onLongClick = { onItemLongClick(lens) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "${lens.manufacturer} ${lens.modelName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
