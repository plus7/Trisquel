package net.tnose.app.trisquel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun getDateRange(dates: List<String>): String {
    if (dates.isEmpty()) return ""

    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC)
    var minDate: LocalDate? = null
    var maxDate: LocalDate? = null

    for (date in dates) {
        try {
            val d = LocalDate.parse(date, formatter)
            if (minDate == null || d.isBefore(minDate)) minDate = d
            if (maxDate == null || d.isAfter(maxDate)) maxDate = d
        } catch (e: DateTimeParseException) {
        }
    }

    if (minDate == null || maxDate == null) return ""

    return if (minDate == maxDate) {
        minDate.format(formatter)
    } else {
        "${minDate.format(formatter)}-${maxDate.format(formatter)}"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmRollListScreen(
    filmrolls: List<FilmRollAndRels>,
    onItemClick: (FilmRollAndRels) -> Unit,
    onItemLongClick: (FilmRollAndRels) -> Unit,
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
    } else if (filmrolls.isEmpty()) {
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
            items(filmrolls, key = { it.filmRoll.id }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    if (item.filmRoll.name.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.empty_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        Text(
                            text = item.filmRoll.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    val cameraBrandStr = "${item.camera?.manufacturer ?: ""} ${item.camera?.modelName ?: ""}   ${item.filmRoll.manufacturer} ${item.filmRoll.brand}"
                    Text(
                        text = cameraBrandStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val exp = item.photoDates.size
                    val array = arrayListOf<String>()
                    val dateRange = getDateRange(item.photoDates)
                    if (dateRange.isNotEmpty()) array.add(dateRange)
                    array.add(if (exp == 1) "%d shot".format(exp) else "%d shots".format(exp))
                    
                    Text(
                        text = array.joinToString("   "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
