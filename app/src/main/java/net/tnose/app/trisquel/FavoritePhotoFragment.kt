package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val context = LocalContext.current
                    val groupedPhotos = remember { mutableStateOf<List<Pair<String, List<Photo>>>>(emptyList()) }

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            val dao = TrisquelDao(context)
                            dao.connection()
                            val list = dao.getAllFavedPhotos()

                            val map = list.groupBy { it.filmrollid }
                            val list2 = map.values.sortedByDescending { it[0].date }

                            val result = list2.map { l ->
                                val sortedList = l.sortedBy { it.frameIndex }
                                val filmrollName = dao.getFilmRoll(l[0].filmrollid)?.name ?: ""
                                Pair(filmrollName, sortedList)
                            }
                            dao.close()
                            
                            withContext(Dispatchers.Main) {
                                groupedPhotos.value = result
                            }
                        }
                    }

                    FavoritePhotoScreen(
                        groupedPhotos = groupedPhotos.value,
                        columnCount = columnCount,
                        onItemClick = { photo, list -> listener?.onListFragmentInteraction(photo, list) }
                    )
                }
            }
        }
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FavoritePhotoScreen(
    groupedPhotos: List<Pair<String, List<Photo>>>,
    columnCount: Int,
    onItemClick: (Photo, List<Photo>) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize()
    ) {
        groupedPhotos.forEach { (filmrollName, photos) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = filmrollName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            
            items(photos) { photo ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clickable { onItemClick(photo, photos) }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (photo.supplementalImages.isNotEmpty()) {
                        val path = photo.supplementalImages[0]
                        val model = if (path.startsWith("/")) java.io.File(path) else android.net.Uri.parse(path)
                        GlideImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = placeholder(R.drawable.general_image_gray),
                            failure = placeholder(R.drawable.ic_error_circle)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFDDDDDD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%.0fmm".format(photo.focalLength),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "F%s".format(photo.aperture.toString()),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                if (photo.shutterSpeed > 0) {
                                    Text(
                                        text = Util.doubleToStringShutterSpeed(photo.shutterSpeed),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}