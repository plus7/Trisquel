package net.tnose.app.trisquel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(initialPhoto: Photo, favList: List<Photo>) {
    val allImages = remember(favList) {
        favList.flatMap { photo ->
            photo.supplementalImages.map { imagePath ->
                photo.id to imagePath
            }
        }
    }

    val initialPage = remember(allImages, initialPhoto) {
        var pos = 0
        var found = false
        for (i in favList.indices) {
            if (favList[i].id == initialPhoto.id) {
                found = true
                break
            }
            pos += favList[i].supplementalImages.size
        }
        if (found && pos < allImages.size) pos else 0
    }

    if (allImages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { allImages.size }
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) { page ->
        val (_, imagePath) = allImages[page]
        ZoomableImage(imagePath = imagePath)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ZoomableImage(imagePath: String, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()

    val model = remember(imagePath) {
        if (imagePath.startsWith("/")) {
            File(imagePath)
        } else {
            imagePath.toUri()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (scale.value > 1f) {
                                launch { scale.animateTo(1f) }
                                launch { offset.animateTo(Offset.Zero) }
                            } else {
                                val targetScale = 3f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val tapFromCenter = tapOffset - center
                                val targetOffset = -(tapFromCenter * (targetScale - 1f))
                                
                                val maxX = (size.width * (targetScale - 1)) / 2f
                                val maxY = (size.height * (targetScale - 1)) / 2f
                                
                                val coercedX = targetOffset.x.coerceIn(-maxX, maxX)
                                val coercedY = targetOffset.y.coerceIn(-maxY, maxY)
                                
                                launch { scale.animateTo(targetScale) }
                                launch { offset.animateTo(Offset(coercedX, coercedY)) }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val isPinching = event.changes.size > 1
                            if (isPinching || scale.value > 1f) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                
                                val newScale = (scale.value * zoomChange).coerceIn(1f, 5f)
                                
                                val maxX = (size.width * (newScale - 1)) / 2f
                                val maxY = (size.height * (newScale - 1)) / 2f
                                
                                val targetX = offset.value.x + panChange.x
                                val targetY = offset.value.y + panChange.y
                                
                                val coercedX = targetX.coerceIn(-maxX, maxX)
                                val coercedY = targetY.coerceIn(-maxY, maxY)
                                
                                val atEdgeX = targetX != coercedX
                                
                                coroutineScope.launch {
                                    scale.snapTo(newScale)
                                    offset.snapTo(Offset(coercedX, coercedY))
                                }
                                
                                if (isPinching || !atEdgeX) {
                                    event.changes.forEach {
                                        if (it.positionChanged()) {
                                            it.consume()
                                        }
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            }
    ) {
        GlideImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offset.value.x,
                    translationY = offset.value.y
                )
        )
    }
}
