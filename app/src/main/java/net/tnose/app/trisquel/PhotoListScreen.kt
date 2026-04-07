package net.tnose.app.trisquel

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PhotoListScreen(
    photos: List<Pair<String, PhotoAndTagIds>>,
    onItemClick: (Photo) -> Unit,
    onItemLongClick: (Photo) -> Unit,
    onIndexClick: (Photo) -> Unit,
    onIndexLongClick: (Photo) -> Unit,
    onThumbnailClick: (Photo) -> Unit,
    onFavoriteClick: (Photo) -> Unit,
    isLoading: Boolean = false
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(photos, key = { it.second.photo.id }) { item ->
                val prevDate = item.first
                val photoAndTagIds = item.second
                val photoEntity = photoAndTagIds.photo
                val photo = Photo.fromEntity(photoEntity)
                PhotoItemCompose(
                    photo = photo,
                    tagIds = photoAndTagIds.tagIds,
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
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun PhotoItemCompose(
    photo: Photo,
    tagIds: List<Int>,
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
            val db = TrisquelRoomDatabase.getInstance(context)
            val dao = db.trisquelDao()
            val lEntity = dao.getLens(photo.lensid)
            val tagAndTagMaps = dao.getTagMapAndTagsByPhoto(photo.id)
            
            withContext(Dispatchers.Main) {
                lens = lEntity?.let { LensSpec.fromEntity(it) }
                tags = tagAndTagMaps.mapNotNull { it.tag?.let { tEntity -> Tag(tEntity.id, tEntity.label, tEntity.refcnt ?: 0) } }
                    .sortedByDescending { it.refcnt }
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
                    if (path.startsWith("/")) File(path) else path.toUri()
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
