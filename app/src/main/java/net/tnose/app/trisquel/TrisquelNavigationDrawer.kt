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
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

data class DrawerItem(val routeClass: KClass<*>, val routeObj: Any, val title: String, val iconRes: Int)
data class ActionDrawerItem(val actionId: String, val title: String, val iconRes: Int)

@Composable
fun TrisquelNavigationDrawer(
    drawerState: DrawerState,
    navController: NavHostController,
    currentDestination: NavDestination?,
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
        DrawerItem(FilmRollsRoute::class, FilmRollsRoute, stringResource(R.string.title_activity_filmroll_list), R.drawable.ic_filmroll_vector),
        DrawerItem(FavoritesRoute::class, FavoritesRoute, stringResource(R.string.title_activity_favorites), R.drawable.ic_fav_border_black)
    )
    val gearItems = listOf(
        DrawerItem(CamerasRoute::class, CamerasRoute, stringResource(R.string.title_activity_cam_list), R.drawable.ic_menu_camera),
        DrawerItem(LensesRoute::class, LensesRoute, stringResource(R.string.title_activity_lens_list), R.drawable.ic_lens),
        DrawerItem(AccessoriesRoute::class, AccessoriesRoute, stringResource(R.string.title_activity_accessory_list), R.drawable.ic_extension_black_24dp)
    )
    val infoItems = listOf(
        ActionDrawerItem("settings", stringResource(R.string.action_settings), R.drawable.ic_settings_black_24dp),
        ActionDrawerItem("backup", stringResource(R.string.title_backup), R.drawable.ic_export),
        ActionDrawerItem("import", stringResource(R.string.title_import), R.drawable.ic_import)
    )
    val otherItems = listOf(
        ActionDrawerItem("release_notes", stringResource(R.string.action_releasenotes), 0),
        ActionDrawerItem("license", stringResource(R.string.action_license), 0)
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
                            selected = currentDestination?.hasRoute(item.routeClass) == true,
                            icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                            onClick = {
                                navController.navigate(item.routeObj) { launchSingleTop = true }
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
                            selected = currentDestination?.hasRoute(item.routeClass) == true,
                            icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                            onClick = {
                                navController.navigate(item.routeObj) { launchSingleTop = true }
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
                                when (item.actionId) {
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
                                when (item.actionId) {
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
