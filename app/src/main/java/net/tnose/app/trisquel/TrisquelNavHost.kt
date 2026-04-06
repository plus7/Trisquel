package net.tnose.app.trisquel

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TrisquelNavHost(
    navController: NavHostController,
    initialRoute: String,
    modifier: Modifier = Modifier,
    mainTopBar: @Composable (String) -> Unit,
    mainViewModel: MainViewModel,
    filmRollViewModel: FilmRollViewModel,
    cameraViewModel: CameraViewModel,
    lensViewModel: LensViewModel,
    accessoryViewModel: AccessoryViewModel,
    onFilmRollDeleteRequest: (FilmRoll) -> Unit,
    onCameraDeleteRequest: (CameraSpec) -> Unit,
    onLensDeleteRequest: (LensSpec) -> Unit,
    onAccessoryDeleteRequest: (Accessory) -> Unit,
    onPhotoInteraction: (Photo?, List<Photo?>) -> Unit
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = initialRoute,
        modifier = modifier
    ) {
        composable(MainActivity.ROUTE_FILMROLLS) {
            val filmrolls by filmRollViewModel.allFilmRollAndRels.observeAsState(emptyList())
            val isFilmRollsLoading by filmRollViewModel.isLoading.observeAsState(false)
            
            Scaffold(topBar = { mainTopBar(MainActivity.ROUTE_FILMROLLS) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    FilmRollListScreen(
                        filmrolls = filmrolls,
                        onItemClick = { item ->
                            val f = FilmRoll.fromEntity(item)
                            navController.navigate("photo_list/${f.id}")
                        },
                        onItemLongClick = { onFilmRollDeleteRequest(FilmRoll.fromEntity(it)) },
                        emptyMessage = stringResource(R.string.warning_filmroll_not_registered),
                        isLoading = isFilmRollsLoading
                    )
                    FloatingActionButton(
                        onClick = {
                            var defaultCameraId = -1
                            var defaultManufacturer = ""
                            var defaultBrand = ""
                            if (mainViewModel.currentFilter.first == 1) {
                                defaultCameraId = mainViewModel.currentFilter.second[0].toInt()
                            } else if (mainViewModel.currentFilter.first == 2) {
                                defaultManufacturer = mainViewModel.currentFilter.second[0]
                                defaultBrand = mainViewModel.currentFilter.second[1]
                            }
                            navController.navigate("edit_filmroll?id=-1&default_camera=${defaultCameraId}&default_manufacturer=${defaultManufacturer}&default_brand=${defaultBrand}")
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_filmroll_vector_white), null, tint = Color.White)
                    }
                }
            }
        }
        composable(MainActivity.ROUTE_CAMERAS) {
            val cameras by cameraViewModel.cameras.observeAsState(emptyList())
            val isCamerasLoading by cameraViewModel.isLoading.observeAsState(false)
            var isFabExpanded by rememberSaveable { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            
            Scaffold(topBar = { mainTopBar(MainActivity.ROUTE_CAMERAS) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    CameraListScreen(
                        cameras = cameras,
                        onItemClick = { item ->
                            navController.navigate("edit_camera/${item.type}?id=${item.id}")
                        },
                        onItemLongClick = { onCameraDeleteRequest(it) },
                        emptyMessage = stringResource(R.string.warning_cam_not_registered),
                        scrollTargetIndex = null,
                        onScrollConsumed = {},
                        isLoading = isCamerasLoading
                    )

                    if (isFabExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { isFabExpanded = false }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        AnimatedVisibility(visible = isFabExpanded) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                    Text(
                                        text = stringResource(R.string.register_flc),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    SmallFloatingActionButton(
                                        onClick = {
                                            isFabExpanded = false
                                            navController.navigate("edit_camera/1?id=-1")
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ) {
                                        Icon(painterResource(R.drawable.ic_menu_camera_white), contentDescription = null, tint = Color.White)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                    Text(
                                        text = stringResource(R.string.register_ilc),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    SmallFloatingActionButton(
                                        onClick = {
                                            isFabExpanded = false
                                            navController.navigate("edit_camera/0?id=-1")
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ) {
                                        Icon(painterResource(R.drawable.ic_menu_camera_white), contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                        FloatingActionButton(
                            onClick = { isFabExpanded = !isFabExpanded },
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ) {
                            val rotation by animateFloatAsState(targetValue = if (isFabExpanded) 45f else 0f)
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.graphicsLayer(rotationZ = rotation)
                            )
                        }
                    }
                }
            }
        }
        composable(MainActivity.ROUTE_LENSES) {
            val addLensLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) lensViewModel.handleAddResult(result.data)
            }
            val editLensLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) lensViewModel.handleEditResult(result.data)
            }
            val lenses by lensViewModel.lenses.observeAsState(emptyList())
            val isLensesLoading by lensViewModel.isLoading.observeAsState(false)
            
            Scaffold(topBar = { mainTopBar(MainActivity.ROUTE_LENSES) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    LensListScreen(
                        lenses = lenses,
                        onItemClick = { item ->
                            val intent = Intent(context, EditLensActivity::class.java)
                            intent.putExtra("id", item.id)
                            editLensLauncher.launch(intent)
                        },
                        onItemLongClick = { onLensDeleteRequest(it) },
                        emptyMessage = stringResource(R.string.warning_lens_not_registered),
                        scrollTargetIndex = null,
                        onScrollConsumed = {},
                        isLoading = isLensesLoading
                    )
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, EditLensActivity::class.java)
                            addLensLauncher.launch(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_lens_white), null, tint = Color.White)
                    }
                }
            }
        }
        composable(MainActivity.ROUTE_ACCESSORIES) {
            val accessories by accessoryViewModel.allAccessories.observeAsState(emptyList())
            val isAccessoriesLoading by accessoryViewModel.isLoading.observeAsState(false)
            
            Scaffold(topBar = { mainTopBar(MainActivity.ROUTE_ACCESSORIES) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    AccessoryListScreen(
                        accessories = accessories,
                        onItemClick = { item ->
                            val a = Accessory.fromEntity(item)
                            navController.navigate("edit_accessory?id=${a.id}")
                        },
                        onItemLongClick = { onAccessoryDeleteRequest(Accessory.fromEntity(it)) },
                        emptyMessage = stringResource(R.string.warning_accessory_not_registered),
                        isLoading = isAccessoriesLoading
                    )
                    FloatingActionButton(
                        onClick = {
                            navController.navigate("edit_accessory?id=-1")
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_extension_white), null, tint = Color.White)
                    }
                }
            }
        }
        composable(
            route = "edit_accessory?id={id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType; defaultValue = -1 })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            EditAccessoryRoute(id = id, onCancel = { navController.popBackStack() })
        }
        composable(MainActivity.ROUTE_FAVORITES) {
            val groupedPhotos = remember { mutableStateOf<List<Pair<String, List<Photo>>>>(emptyList()) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val dao = TrisquelDao(context)
                    dao.connection()
                    val list = dao.getAllFavedPhotos()
                    val map = list.groupBy { it.filmrollid }
                    val list2 = map.values.sortedByDescending { it[0].date }
                    val result = list2.map { l ->
                        val sortedList = l.sortedBy { it.frameIndex }
                        val filmrollName = dao.getFilmRoll(l[0].filmrollid)?.name ?: ""
                        Pair(filmrollName, sortedList)
                    }
                    dao.close()
                    withContext(Dispatchers.Main) { groupedPhotos.value = result }
                }
            }
            Scaffold(topBar = { mainTopBar(MainActivity.ROUTE_FAVORITES) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    FavoritePhotoScreen(
                        groupedPhotos = groupedPhotos.value,
                        columnCount = 3,
                        onItemClick = { photo, list -> onPhotoInteraction(photo, list) }
                    )
                }
            }
        }
        composable(MainActivity.ROUTE_SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(MainActivity.ROUTE_LICENSE) {
            LicenseScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "edit_camera/{type}?id={id}",
            arguments = listOf(
                androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getInt("type") ?: 0
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            EditCameraRoute(
                id = id, 
                type = type, 
                onSaveSuccess = { 
                    cameraViewModel.load()
                    navController.popBackStack() 
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit_filmroll?id={id}&default_camera={default_camera}&default_manufacturer={default_manufacturer}&default_brand={default_brand}",
            arguments = listOf(
                androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType; defaultValue = -1 },
                androidx.navigation.navArgument("default_camera") { type = androidx.navigation.NavType.IntType; defaultValue = -1 },
                androidx.navigation.navArgument("default_manufacturer") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                androidx.navigation.navArgument("default_brand") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            EditFilmRollRoute(
                id = id,
                onCancel = { navController.popBackStack() },
                onNavigateToEditCamera = { navController.navigate("edit_camera/0?id=-1") }
            )
        }
        composable(
            route = "photo_list/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            EditPhotoListRoute(
                id = id,
                onBack = { navController.popBackStack() },
                onNavigateToEditFilmRoll = { frId -> navController.navigate("edit_filmroll?id=$frId") },
                onNavigateToEditPhoto = { frId, pId, fIdx -> 
                    navController.navigate("edit_photo?filmroll=$frId&id=$pId&frameIndex=$fIdx")
                }
            )
        }
        composable(
            route = "edit_photo?filmroll={filmroll}&id={id}&frameIndex={frameIndex}",
            arguments = listOf(
                androidx.navigation.navArgument("filmroll") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType; defaultValue = -1 },
                androidx.navigation.navArgument("frameIndex") { type = androidx.navigation.NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val filmroll = backStackEntry.arguments?.getInt("filmroll") ?: -1
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            val frameIndex = backStackEntry.arguments?.getInt("frameIndex") ?: -1
            EditPhotoRoute(
                id = id,
                filmRollId = filmroll,
                frameIndex = frameIndex,
                onCancel = { navController.popBackStack() },
                onNavigateToEditLens = {
                    // navController.navigate("edit_lens?id=-1")
                }
            )
        }
    }
}