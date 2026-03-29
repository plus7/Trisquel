package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoFragment : androidx.fragment.app.Fragment() {
    private var mColumnCount = 1
    private var mFilmRollId = -1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mPhotoViewModel: PhotoViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mColumnCount = arguments?.getInt(ARG_COLUMN_COUNT) ?: 1
        mFilmRollId = arguments?.getInt(ARG_FILMROLL_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mPhotoViewModel = ViewModelProvider(this).get(PhotoViewModel::class.java)
        mPhotoViewModel!!.filmRollId.value = mFilmRollId

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val photos by mPhotoViewModel!!.photosByFilmRollId.observeAsState(initial = emptyList())
                    PhotoListScreen(
                        photos = photos,
                        onItemClick = { mListener?.onListFragmentInteraction(it, false) },
                        onItemLongClick = { mListener?.onListFragmentInteraction(it, true) },
                        onIndexClick = { mListener?.onIndexClick(it) },
                        onIndexLongClick = { mListener?.onIndexLongClick(it) },
                        onThumbnailClick = { mListener?.onThumbnailClick(it) },
                        onFavoriteClick = { mListener?.onFavoriteClick(it) }
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

    fun possibleDownShiftLimit(p: Photo): Int {
        var curpos = curPosOf(p)
        val list = mPhotoViewModel!!.photosByFilmRollId.value!!
        return if (curpos > 0) {
            val prev = list[curpos - 1]
            prev.second.photo._index!!
        } else {
            0
        }
    }

    private fun curPosOf(p: Photo): Int {
        var curpos = -1
        val list = mPhotoViewModel!!.photosByFilmRollId.value!!
        for (i in list.indices) {
            if (list[i].second.photo.id == p.id) {
                curpos = i
                break
            }
        }
        return curpos
    }

    fun shiftFrameIndexFrom(p: Photo, amount: Int) {
        if (p.frameIndex + amount < possibleDownShiftLimit(p)) throw Exception()

        val curpos = curPosOf(p)
        val list = mPhotoViewModel!!.photosByFilmRollId.value!!
        for (i in curpos until list.size) {
            val np = list[i].second.photo.copy(_index = list[i].second.photo._index!! + amount)
            mPhotoViewModel!!.update(np)
        }
    }

    fun insertPhoto(p: Photo, tags: ArrayList<String>?) {
        if (p.frameIndex == -1) {
            val list = mPhotoViewModel!!.photosByFilmRollId.value
            p.frameIndex = if (list.isNullOrEmpty()) 0 else (list.last().second.photo._index ?: 0) + 1
        }
        if (tags != null) {
            mPhotoViewModel!!.insertWithTag(p.toEntity(), mFilmRollId, tags)
        } else {
            mPhotoViewModel!!.insert(p.toEntity())
        }
    }

    fun toggleFavPhoto(p: Photo) {
        mPhotoViewModel!!.update(p.toEntity())
    }

    fun updatePhoto(p: Photo, tags: ArrayList<String>?) {
        if (tags != null) {
            mPhotoViewModel!!.tagPhoto(p.id, mFilmRollId, tags)
        }
        mPhotoViewModel!!.update(p.toEntity())
    }

    fun deletePhoto(id: Int) {
        mPhotoViewModel!!.delete(id)
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Photo, isLong: Boolean)
        fun onThumbnailClick(item: Photo)
        fun onIndexClick(item: Photo)
        fun onIndexLongClick(item: Photo)
        fun onFavoriteClick(item: Photo)
    }

    companion object {
        private const val ARG_COLUMN_COUNT = "column-count"
        private const val ARG_FILMROLL_ID = "filmroll_id"

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

@Composable
fun PhotoListScreen(
    photos: List<Pair<String, PhotoAndTagIds>>,
    onItemClick: (Photo) -> Unit,
    onItemLongClick: (Photo) -> Unit,
    onIndexClick: (Photo) -> Unit,
    onIndexLongClick: (Photo) -> Unit,
    onThumbnailClick: (Photo) -> Unit,
    onFavoriteClick: (Photo) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(photos, key = { it.second.photo.id }) { item ->
            val prevDate = item.first
            val photoEntity = item.second.photo
            val photo = Photo.fromEntity(photoEntity)
            PhotoItemCompose(
                photo = photo,
                prevDate = prevDate,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                onIndexClick = onIndexClick,
                onIndexLongClick = onIndexLongClick,
                onThumbnailClick = onThumbnailClick,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun PhotoItemCompose(
    photo: Photo,
    prevDate: String,
    onItemClick: (Photo) -> Unit,
    onItemLongClick: (Photo) -> Unit,
    onIndexClick: (Photo) -> Unit,
    onIndexLongClick: (Photo) -> Unit,
    onThumbnailClick: (Photo) -> Unit,
    onFavoriteClick: (Photo) -> Unit
) {
    val context = LocalContext.current
    var lens by remember { mutableStateOf<LensSpec?>(null) }
    var tags by remember { mutableStateOf<List<Tag>>(emptyList()) }

    LaunchedEffect(photo.id) {
        withContext(Dispatchers.IO) {
            val dao = TrisquelDao(context)
            dao.connection()
            val l = dao.getLens(photo.lensid)
            val t = dao.getTagsByPhoto(photo.id)
            dao.close()
            withContext(Dispatchers.Main) {
                lens = l
                tags = t.apply { sortByDescending { it.refcnt } }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onItemClick(photo) },
                onLongClick = { onItemLongClick(photo) }
            )
    ) {
        if (photo.date != prevDate) {
            Text(
                text = photo.date,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(96.dp)
                    .height(64.dp)
            ) {
                val model = if (photo.supplementalImages.isNotEmpty()) {
                    val path = photo.supplementalImages[0]
                    if (path.startsWith("/")) File(path) else android.net.Uri.parse(path)
                } else null

                if (model != null) {
                    GlideImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD6D7D7))
                            .clickable { onThumbnailClick(photo) },
                        loading = placeholder(R.drawable.general_image_gray),
                        failure = placeholder(R.drawable.ic_error_circle)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD6D7D7))
                            .clickable { onThumbnailClick(photo) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_image_gray),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }
                }

                Text(
                    text = (photo.frameIndex + 1).toString(),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.DarkGray)
                        .padding(horizontal = 4.dp)
                        .combinedClickable(
                            onClick = { onIndexClick(photo) },
                            onLongClick = { onIndexLongClick(photo) }
                        )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = lens?.let { "${it.manufacturer} ${it.modelName}" } ?: stringResource(id = R.string.label_lens),
                    style = MaterialTheme.typography.bodyMedium
                )

                val params = buildList {
                    if (photo.aperture != 0.0) add("f/%.1f".format(photo.aperture))
                    if (photo.shutterSpeed != 0.0) add("${Util.doubleToStringShutterSpeed(photo.shutterSpeed)}sec")
                }
                Text(
                    text = params.joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val annotatedMemo = buildAnnotatedString {
                        append(photo.memo)
                        if (tags.isNotEmpty()) {
                            val displayTags = if (tags.size > 3) {
                                tags.take(2).map { it.label } + "+${tags.size - 2}"
                            } else {
                                tags.take(3).map { it.label }
                            }
                            displayTags.forEach { label ->
                                append(" ")
                                withStyle(
                                    style = SpanStyle(
                                        background = Color.LightGray,
                                        color = Color.Black,
                                        fontSize = 12.sp
                                    )
                                ) {
                                    append(" $label ")
                                }
                            }
                        }
                    }
                    Text(
                        text = annotatedMemo,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        painter = painterResource(id = if (photo.favorite) R.drawable.ic_fav else R.drawable.ic_fav_border),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onFavoriteClick(photo) },
                        tint = Color.Unspecified
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    }
}
