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
