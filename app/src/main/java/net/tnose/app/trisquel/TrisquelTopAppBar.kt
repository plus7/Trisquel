package net.tnose.app.trisquel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrisquelTopAppBar(
    observedRoute: String,
    currentSubtitle: String,
    currentFilter: Pair<Int, ArrayList<String>>,
    drawerState: androidx.compose.material3.DrawerState,
    scope: CoroutineScope,
    onSortClick: () -> Unit,
    onFilterNoFilterClick: () -> Unit,
    onFilterByCameraClick: () -> Unit,
    onFilterByFilmBrandClick: () -> Unit,
    onPinnedFilterClick: (Pair<Int, ArrayList<String>>) -> Unit,
    onSearchClick: () -> Unit,
    onPinFilterClick: () -> Unit,
    onUnpinFilterClick: () -> Unit,
    getPinnedFilters: () -> List<Pair<Int, ArrayList<String>>>,
    getFilterLabel: (Pair<Int, ArrayList<String>>) -> String
) {
    var showFilterMenu by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    val titleRes = when(observedRoute) {
        MainActivity.ROUTE_CAMERAS -> R.string.title_activity_cam_list
        MainActivity.ROUTE_LENSES -> R.string.title_activity_lens_list
        MainActivity.ROUTE_ACCESSORIES -> R.string.title_activity_accessory_list
        MainActivity.ROUTE_FAVORITES -> R.string.title_activity_favorites
        else -> R.string.title_activity_filmroll_list
    }

    TopAppBar(
        title = {
            Column {
                Text(stringResource(titleRes))
                if (observedRoute == MainActivity.ROUTE_FILMROLLS && currentSubtitle.isNotEmpty()) {
                    Text(currentSubtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                Icon(Icons.Default.Menu, "Menu")
            }
        },
        actions = {
            if (observedRoute in listOf(MainActivity.ROUTE_FILMROLLS, MainActivity.ROUTE_CAMERAS, MainActivity.ROUTE_LENSES, MainActivity.ROUTE_ACCESSORIES)) {
                IconButton(onClick = onSortClick) {
                    Icon(painterResource(R.drawable.ic_sort_white_24dp), null)
                }
            }
            if (observedRoute == MainActivity.ROUTE_FILMROLLS) {
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(painterResource(R.drawable.ic_filter_white), null)
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        if (currentFilter.first != 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.label_no_filter)) },
                                onClick = {
                                    onFilterNoFilterClick()
                                    showFilterMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_filter_by_camera)) },
                            onClick = {
                                onFilterByCameraClick()
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_filter_by_film_brand)) },
                            onClick = {
                                onFilterByFilmBrandClick()
                                showFilterMenu = false
                            }
                        )
                        val pinnedFilters = getPinnedFilters()
                        if (pinnedFilters.isNotEmpty()) {
                            HorizontalDivider()
                            pinnedFilters.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(getFilterLabel(f)) },
                                    onClick = {
                                        onPinnedFilterClick(f)
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onSearchClick) {
                    Icon(painterResource(R.drawable.ic_search_white_24dp), null)
                }
                
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                        val pinnedFilters = getPinnedFilters()
                        val isPinned = pinnedFilters.any { it.first == currentFilter.first && it.second.containsAll(currentFilter.second) }
                        if (currentFilter.first != 0 && !isPinned) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_pin_current_filter)) },
                                onClick = { onPinFilterClick(); showOverflowMenu = false }
                            )
                        } else if (currentFilter.first != 0 && isPinned) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_unpin_current_filter)) },
                                onClick = { onUnpinFilterClick(); showOverflowMenu = false }
                            )
                        }
                    }
                }
            }
        }
    )
}
