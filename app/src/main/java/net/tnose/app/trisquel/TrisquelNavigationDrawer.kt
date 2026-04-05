package net.tnose.app.trisquel

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class DrawerItem(val route: String, val title: String, val iconRes: Int)

@Composable
fun TrisquelNavigationDrawer(
    drawerState: DrawerState,
    navController: NavHostController,
    observedRoute: String,
    scope: CoroutineScope,
    gesturesEnabled: Boolean = true,
    onSettingsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onImportClick: () -> Unit,
    onReleaseNotesClick: () -> Unit,
    onLicenseClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerItems = listOf(
        DrawerItem(MainActivity.ROUTE_FILMROLLS, stringResource(R.string.title_activity_filmroll_list), R.drawable.ic_filmroll_vector),
        DrawerItem(MainActivity.ROUTE_FAVORITES, stringResource(R.string.title_activity_favorites), R.drawable.ic_fav_border_black)
    )
    val gearItems = listOf(
        DrawerItem(MainActivity.ROUTE_CAMERAS, stringResource(R.string.title_activity_cam_list), R.drawable.ic_menu_camera),
        DrawerItem(MainActivity.ROUTE_LENSES, stringResource(R.string.title_activity_lens_list), R.drawable.ic_lens),
        DrawerItem(MainActivity.ROUTE_ACCESSORIES, stringResource(R.string.title_activity_accessory_list), R.drawable.ic_extension_black_24dp)
    )
    val infoItems = listOf(
        DrawerItem("settings", stringResource(R.string.action_settings), R.drawable.ic_settings_black_24dp),
        DrawerItem("backup", stringResource(R.string.title_backup), R.drawable.ic_export),
        DrawerItem("import", stringResource(R.string.title_import), R.drawable.ic_import)
    )
    val otherItems = listOf(
        DrawerItem("release_notes", stringResource(R.string.action_releasenotes), 0),
        DrawerItem("license", stringResource(R.string.action_license), 0)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                LazyColumn {
                    items(drawerItems) { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = observedRoute == item.route,
                            icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                            onClick = {
                                navController.navigate(item.route) { launchSingleTop = true }
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    item { Text(stringResource(R.string.menu_category_gears), Modifier.padding(16.dp, 8.dp)) }
                    items(gearItems) { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = observedRoute == item.route,
                            icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                            onClick = {
                                navController.navigate(item.route) { launchSingleTop = true }
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    items(infoItems) { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = false,
                            icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                            onClick = {
                                scope.launch { drawerState.close() }
                                when (item.route) {
                                    "settings" -> onSettingsClick()
                                    "backup" -> onBackupClick()
                                    "import" -> onImportClick()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    items(otherItems) { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                when (item.route) {
                                    "release_notes" -> onReleaseNotesClick()
                                    "license" -> onLicenseClick()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        },
        content = content
    )
}
