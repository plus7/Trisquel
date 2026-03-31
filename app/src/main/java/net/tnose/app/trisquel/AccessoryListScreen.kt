package net.tnose.app.trisquel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccessoryListScreen(
    accessories: List<AccessoryEntity>,
    onItemClick: (AccessoryEntity) -> Unit,
    onItemLongClick: (AccessoryEntity) -> Unit,
    emptyMessage: String,
    isLoading: Boolean = false
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (accessories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(accessories, key = { it.id }) { accessory ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(accessory) },
                            onLongClick = { onItemLongClick(accessory) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconRes = when (accessory.type) {
                        Accessory.ACCESSORY_FILTER -> R.drawable.ic_filter_plane
                        Accessory.ACCESSORY_EXT_TUBE -> R.drawable.ic_extenstion_tube
                        Accessory.ACCESSORY_TELE_CONVERTER -> R.drawable.ic_zoom_in_black_24dp
                        Accessory.ACCESSORY_WIDE_CONVERTER -> R.drawable.ic_zoom_out_black_24dp
                        else -> R.drawable.ic_unknown_accessory_plane
                    }
                    
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = accessory.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
