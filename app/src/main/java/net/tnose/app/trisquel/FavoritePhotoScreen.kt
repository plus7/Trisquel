package net.tnose.app.trisquel

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import androidx.core.net.toUri

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
                        val model = if (path.startsWith("/")) java.io.File(path) else path.toUri()
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