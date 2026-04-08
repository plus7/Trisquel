package net.tnose.app.trisquel

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SwipeBackWrapper(content: @Composable () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 150f) {
                            dispatcher?.onBackPressed()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0 || offsetX > 0) {
                            offsetX += dragAmount
                        }
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun TrisquelNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mainTopBar: @Composable (NavDestination?) -> Unit,
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
    val repo = remember { TrisquelRepo(context.applicationContext as android.app.Application) }

    NavHost(
        navController = navController,
        startDestination = FilmRollsRoute,
        modifier = modifier
    ) {
        composable<FilmRollsRoute> {
            val filmrolls by filmRollViewModel.allFilmRollAndRels.collectAsStateWithLifecycle()
            val isFilmRollsLoading by filmRollViewModel.isLoading.collectAsStateWithLifecycle()
            
            Scaffold(topBar = { mainTopBar(it.destination) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    FilmRollListScreen(
                        filmrolls = filmrolls,
                        onItemClick = { item ->
                            val f = FilmRoll.fromEntity(item)
                            navController.navigate(PhotoListRoute(id = f.id))
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
                            navController.navigate(EditFilmRollRoute(
                                id = -1,
                                defaultCamera = defaultCameraId,
                                defaultManufacturer = defaultManufacturer,
                                defaultBrand = defaultBrand
                            ))
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_filmroll_vector_white), null, tint = Color.White)
                    }
                }
            }
        }
        composable<CamerasRoute> {
            val cameras by cameraViewModel.cameras.collectAsStateWithLifecycle()
            val isCamerasLoading by cameraViewModel.isLoading.collectAsStateWithLifecycle()
            var isFabExpanded by rememberSaveable { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            
            Scaffold(topBar = { mainTopBar(it.destination) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    CameraListScreen(
                        cameras = cameras,
                        onItemClick = { item ->
                            navController.navigate(EditCameraRoute(type = item.type, id = item.id))
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
                                            navController.navigate(EditCameraRoute(type = 1, id = -1))
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
                                            navController.navigate(EditCameraRoute(type = 0, id = -1))
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
        composable<LensesRoute> {
            val lenses by lensViewModel.lenses.collectAsStateWithLifecycle()
            val isLensesLoading by lensViewModel.isLoading.collectAsStateWithLifecycle()
            
            Scaffold(topBar = { mainTopBar(it.destination) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    LensListScreen(
                        lenses = lenses,
                        onItemClick = { item ->
                            navController.navigate(EditLensRoute(id = item.id))
                        },
                        onItemLongClick = { onLensDeleteRequest(it) },
                        emptyMessage = stringResource(R.string.warning_lens_not_registered),
                        scrollTargetIndex = null,
                        onScrollConsumed = {},
                        isLoading = isLensesLoading
                    )
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(EditLensRoute(id = -1))
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_lens_white), null, tint = Color.White)
                    }
                }
            }
        }
        composable<AccessoriesRoute> {
            val accessories by accessoryViewModel.allAccessories.collectAsStateWithLifecycle()
            val isAccessoriesLoading by accessoryViewModel.isLoading.collectAsStateWithLifecycle()
            
            Scaffold(topBar = { mainTopBar(it.destination) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    AccessoryListScreen(
                        accessories = accessories,
                        onItemClick = { item ->
                            val a = Accessory.fromEntity(item)
                            navController.navigate(EditAccessoryRoute(id = a.id))
                        },
                        onItemLongClick = { onAccessoryDeleteRequest(Accessory.fromEntity(it)) },
                        emptyMessage = stringResource(R.string.warning_accessory_not_registered),
                        isLoading = isAccessoriesLoading
                    )
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(EditAccessoryRoute(id = -1))
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_extension_white), null, tint = Color.White)
                    }
                }
            }
        }
        composable<EditAccessoryRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<EditAccessoryRoute>()
            SwipeBackWrapper {
                EditAccessoryRoute(id = route.id, onCancel = { navController.popBackStack() })
            }
        }
        composable<FavoritesRoute> {
            val groupedPhotos = remember { mutableStateOf<List<Pair<String, List<Photo>>>>(emptyList()) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val list = repo.getAllFavedPhotosRaw().map { Photo.fromEntity(it) }
                    val map = list.groupBy { it.filmrollid }
                    val list2 = map.values.sortedByDescending { it[0].date }
                    val result = list2.map { l ->
                        val sortedList = l.sortedBy { it.frameIndex }
                        val filmrollName = repo.getFilmRollRaw(l[0].filmrollid)?.name ?: ""
                        Pair(filmrollName, sortedList)
                    }
                    withContext(Dispatchers.Main) { groupedPhotos.value = result }
                }
            }
            Scaffold(topBar = { mainTopBar(it.destination) }) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    FavoritePhotoScreen(
                        groupedPhotos = groupedPhotos.value,
                        columnCount = 3,
                        onItemClick = { photo, list -> onPhotoInteraction(photo, list) }
                    )
                }
            }
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<LicenseRoute> {
            LicenseScreen(onBack = { navController.popBackStack() })
        }
        composable<EditCameraRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<EditCameraRoute>()
            SwipeBackWrapper {
                EditCameraRoute(
                    id = route.id, 
                    type = route.type, 
                    onSaveSuccess = { 
                        cameraViewModel.load()
                        navController.popBackStack() 
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable<EditFilmRollRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<EditFilmRollRoute>()
            SwipeBackWrapper {
                EditFilmRollRoute(
                    id = route.id,
                    onCancel = { navController.popBackStack() },
                    onNavigateToEditCamera = { navController.navigate(EditCameraRoute(type = 0, id = -1)) }
                )
            }
        }
        composable<PhotoListRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<PhotoListRoute>()
            SwipeBackWrapper {
                EditPhotoListRoute(
                    id = route.id,
                    onBack = { navController.popBackStack() },
                    onNavigateToEditFilmRoll = { frId -> navController.navigate(EditFilmRollRoute(id = frId)) },
                    onNavigateToEditPhoto = { frId, pId, fIdx ->
                        navController.navigate(
                            EditPhotoRoute(
                                filmroll = frId,
                                id = pId,
                                frameIndex = fIdx
                            )
                        )
                    },
                    onNavigateToGallery = { photo, list ->
                        onPhotoInteraction(photo, list)
                    }
                )
            }
        }
        composable<EditPhotoRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<EditPhotoRoute>()
            SwipeBackWrapper {
                EditPhotoRoute(
                    id = route.id,
                    filmRollId = route.filmroll,
                    frameIndex = route.frameIndex,
                    onCancel = { navController.popBackStack() },
                    onNavigateToEditLens = {
                        navController.navigate(EditLensRoute(id = -1))
                    }
                )
            }
        }
        composable<EditLensRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<EditLensRoute>()
            SwipeBackWrapper {
                EditLensRoute(
                    id = route.id,
                    onSaveSuccess = {
                        lensViewModel.load()
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable<SearchRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SearchRoute>()
            val tags = route.tags.split(",").filter { it.isNotEmpty() }
            SearchRoute(
                tags = tags,
                onBack = { navController.popBackStack() },
                onNavigateToEditPhoto = { frId, pId, fIdx ->
                    navController.navigate(EditPhotoRoute(filmroll = frId, id = pId, frameIndex = fIdx))
                },
                onNavigateToGallery = { photo, list ->
                    onPhotoInteraction(photo, list)
                },
                mainViewModel = mainViewModel
            )
        }
        composable<GalleryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GalleryRoute>()
            val initialPhotoId = route.initialPhotoId
            val photoIds = route.photoIds
            
            var initialPhoto by remember { mutableStateOf<Photo?>(null) }
            var photoList by remember { mutableStateOf<List<Photo>?>(null) }
            
            LaunchedEffect(initialPhotoId, photoIds) {
                withContext(Dispatchers.IO) {
                    val list = photoIds.mapNotNull { id ->
                        repo.getPhoto(id)?.let { Photo.fromEntity(it) }
                    }
                    val initial = list.find { it.id == initialPhotoId }
                    withContext(Dispatchers.Main) {
                        photoList = list
                        initialPhoto = initial
                    }
                }
            }
            
            if (initialPhoto != null && photoList != null) {
                GalleryScreen(initialPhoto = initialPhoto!!, favList = photoList!!)
            } else {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }
    }
}
