package net.tnose.app.trisquel

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
