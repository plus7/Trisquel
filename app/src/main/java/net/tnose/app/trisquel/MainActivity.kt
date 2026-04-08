package net.tnose.app.trisquel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    companion object {
        const val ROUTE_FILMROLLS = "filmrolls"
        const val ROUTE_CAMERAS = "cameras"
        const val ROUTE_LENSES = "lenses"
        const val ROUTE_ACCESSORIES = "accessories"
        const val RELEASE_NOTES_URL = "https://x.com/trisquel_app"
    }

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private val backupDirChosenForSlimExLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            mainViewModel.onBackupDirChosen(uri, 0)
        }
    }

    private val backupDirChosenForFullExLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            mainViewModel.onBackupDirChosen(uri, 1)
        }
    }

    private val zipFileChosenForMergeIpLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mainViewModel.onImportFileChosen(uri, 0)
        }
    }

    private val zipFileChosenForReplIpLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mainViewModel.onImportFileChosen(uri, 1)
        }
    }

    private val dbFileChosenForReplDbLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mainViewModel.onDbFileChosen(uri, contentResolver)
        }
    }

    fun handleExportPermissionsResult(permissions: Map<String, Boolean>) {
        val granted = permissions.entries.all { it.value }
        if (granted) {
            mainViewModel.pendingExportMode?.let { mainViewModel.requestBackup(it, true) }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
        mainViewModel.pendingExportMode = null
    }

    fun handleImportPermissionsResult(permissions: Map<String, Boolean>) {
        val granted = permissions.entries.all { it.value }
        if (granted) {
            mainViewModel.pendingImportMode?.let { mainViewModel.requestImport(it, true) }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
        mainViewModel.pendingImportMode = null
    }

    private val requestExportPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        handleExportPermissionsResult(permissions)
    }

    private val requestImportPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        handleImportPermissionsResult(permissions)
    }

    internal val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.events.collect { event ->
                        when (event) {
                            is MainEvent.RestoreDatabaseResult -> {
                                if (event.success) {
                                    val intent = RestartActivity.createIntent(applicationContext)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@MainActivity, getString(R.string.error_not_sqlite3_db), Toast.LENGTH_LONG).show()
                                }
                            }
                            is MainEvent.RequireDbConvAction -> {
                                val uri = "https://pentax.tnose.net/trisquel-for-android/db_conv_on_recent_android/".toUri()
                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }

                            is MainEvent.ShowReleaseNotesConfirm -> {
                                mainViewModel.showDialog(ActiveDialog.Confirm(
                                    title = "Trisquel",
                                    message = getString(R.string.warning_newversion),
                                    positive = getString(R.string.show_release_notes),
                                    negative = getString(R.string.close),
                                    onConfirm = {
                                        val uri = RELEASE_NOTES_URL.toUri()
                                        val i = Intent(Intent.ACTION_VIEW, uri)
                                        startActivity(i)
                                    }
                                ))
                            }
                            is MainEvent.ShowToast -> {
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                            }
                            is MainEvent.RequestExportPermissions -> {
                                requestExportPermissionsLauncher.launch(PERMISSIONS)
                            }
                            is MainEvent.RequestImportPermissions -> {
                                requestImportPermissionsLauncher.launch(PERMISSIONS)
                            }
                            is MainEvent.LaunchExportDocumentTree -> {
                                if(event.mode == 0) backupDirChosenForSlimExLauncher.launch(event.fileName)
                                else backupDirChosenForFullExLauncher.launch(event.fileName)
                            }
                            is MainEvent.LaunchImportDocumentPicker -> {
                                val mimeType = if(event.mode == 2) arrayOf("*/*") else arrayOf("application/zip")
                                when(event.mode){
                                    0 -> zipFileChosenForMergeIpLauncher.launch(mimeType)
                                    1 -> zipFileChosenForReplIpLauncher.launch(mimeType)
                                    else -> dbFileChosenForReplDbLauncher.launch(mimeType)
                                }
                            }
                        }
                    }
                }
            }
        }

        setContent {
            TrisquelTheme {
                MainAppScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.checkAppStartupState(Util.TRISQUEL_VERSION)
    }

    private fun checkPermissions(): Boolean {
        val readDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val mediaLocDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
        val notificationDenied = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) false
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        return !(readDenied || mediaLocDenied || notificationDenied)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppScreen() {
        val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()

        val cameraViewModel: CameraViewModel = viewModel()
        val lensViewModel: LensViewModel = viewModel()
        val accessoryViewModel: AccessoryViewModel = viewModel()
        val filmRollViewModel: FilmRollViewModel = viewModel()
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        LaunchedEffect(Unit) {
            launch {
                cameraViewModel.events.collect { event ->
                    when (event) {
                        is CameraEvent.ShowCannotDeleteAlert -> mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(event.modelName)))
                        is CameraEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.modelName), onConfirm = { cameraViewModel.deleteCamera(event.id) }))
                    }
                }
            }
            launch {
                lensViewModel.events.collect { event ->
                    when (event) {
                        is LensEvent.ShowCannotDeleteAlert -> mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(event.modelName)))
                        is LensEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.modelName), onConfirm = { lensViewModel.deleteLens(event.id) }))
                    }
                }
            }
            launch {
                accessoryViewModel.events.collect { event ->
                    when (event) {
                        is AccessoryEvent.ShowCannotDeleteAlert -> mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(event.name)))
                        is AccessoryEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.name), onConfirm = { accessoryViewModel.delete(event.id) }))
                    }
                }
            }
            launch {
                filmRollViewModel.events.collect { event ->
                    when (event) {
                        is FilmRollEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.name), onConfirm = { filmRollViewModel.delete(event.id) }))
                    }
                }
            }
        }

        TrisquelDialogManager(
            activeDialog = mainViewModel.activeDialog,
            onDismiss = { mainViewModel.dismissDialog() }
        )

        val isTopLevel = currentDestination?.hasRoute(FilmRollsRoute::class) == true ||
                         currentDestination?.hasRoute(CamerasRoute::class) == true ||
                         currentDestination?.hasRoute(LensesRoute::class) == true ||
                         currentDestination?.hasRoute(AccessoriesRoute::class) == true ||
                         currentDestination?.hasRoute(FavoritesRoute::class) == true

        TrisquelNavigationDrawer(
            drawerState = drawerState,
            navController = navController,
            currentDestination = currentDestination,
            scope = scope,
            gesturesEnabled = isTopLevel,
            onSettingsClick = { navController.navigate(SettingsRoute) },
            onBackupClick = {
                mainViewModel.showDialog(ActiveDialog.RichSelection(
                    title = getString(R.string.title_backup_mode_selection),
                    icons = listOf(R.drawable.ic_export, R.drawable.ic_export_img, R.drawable.ic_help),
                    titles = arrayOf(getString(R.string.title_slim_backup), getString(R.string.title_whole_backup), getString(R.string.title_backup_help)),
                    descs = arrayOf(getString(R.string.description_slim_backup), getString(R.string.description_whole_backup), getString(R.string.description_backup_help)),
                    onSelected = { mode ->
                        if (mode < 2) mainViewModel.requestBackup(mode, checkPermissions())
                        else startActivity(Intent(Intent.ACTION_VIEW, "https://pentax.tnose.net/trisquel-for-android/import_export/".toUri()))
                    }
                ))
            },
            onImportClick = {
                mainViewModel.showDialog(ActiveDialog.RichSelection(
                    title = getString(R.string.title_import_mode_selection),
                    icons = listOf(R.drawable.ic_merge, R.drawable.ic_restore, R.drawable.ic_help),
                    titles = arrayOf(getString(R.string.title_merge_zip), getString(R.string.title_import_zip), getString(R.string.title_backup_help)),
                    descs = arrayOf(getString(R.string.description_merge_zip), getString(R.string.description_import_zip), getString(R.string.description_backup_help)),
                    onSelected = { mode ->
                        if (mode < 2) mainViewModel.requestImport(mode, checkPermissions())
                        else startActivity(Intent(Intent.ACTION_VIEW, "https://pentax.tnose.net/trisquel-for-android/import_export/".toUri()))
                    }
                ))
            },
            onReleaseNotesClick = {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        RELEASE_NOTES_URL.toUri())) },
            onLicenseClick = { navController.navigate(LicenseRoute) }
        ) {
            val mainTopBar: @Composable (NavDestination?) -> Unit = { destination ->
                TrisquelTopAppBar(
                    currentDestination = destination,
                    currentSubtitle = mainViewModel.currentSubtitle,
                    currentFilter = mainViewModel.currentFilter,
                    drawerState = drawerState,
                    scope = scope,
                    onSortClick = {
                        val arr = when {
                            destination?.hasRoute(FilmRollsRoute::class) == true -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_camera), getString(R.string.label_brand))
                            destination?.hasRoute(CamerasRoute::class) == true -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_format))
                            destination?.hasRoute(LensesRoute::class) == true -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_focal_length))
                            destination?.hasRoute(AccessoriesRoute::class) == true -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_accessory_type))
                            else -> arrayOf()
                        }
                        
                        val routeKey = when {
                            destination?.hasRoute(FilmRollsRoute::class) == true -> "filmrolls"
                            destination?.hasRoute(CamerasRoute::class) == true -> "cameras"
                            destination?.hasRoute(LensesRoute::class) == true -> "lenses"
                            destination?.hasRoute(AccessoriesRoute::class) == true -> "accessories"
                            else -> ""
                        }
                        
                        val key = userPreferencesRepository.getSortKey(routeKey)
                        mainViewModel.showDialog(ActiveDialog.SingleChoice(
                            title = getString(R.string.label_sort_by),
                            items = arr,
                            selected = key,
                            onConfirm = { handleSort(routeKey, it, filmRollViewModel, cameraViewModel, lensViewModel, accessoryViewModel) }
                        ))
                    },
                    onFilterNoFilterClick = {
                        mainViewModel.currentFilter = Pair(0, arrayListOf())
                        filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey("filmrolls"), Pair(0, "")))
                    },
                    onFilterByCameraClick = {
                        mainViewModel.requestFilterByCamera { _, id ->
                            if (id != null) {
                                mainViewModel.currentFilter = Pair(1, arrayListOf(id.toString()))
                                filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey("filmrolls"), Pair(1, id.toString())))
                            }
                        }
                    },
                    onFilterByFilmBrandClick = {
                        mainViewModel.requestFilterByFilmBrand { fb ->
                            mainViewModel.currentFilter = Pair(2, arrayListOf(fb.first, fb.second))
                            filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey("filmrolls"), Pair(2, fb.second)))
                        }
                    },
                    onPinnedFilterClick = { f ->
                        mainViewModel.currentFilter = f
                        val searchStr = if(f.first == 1) f.second[0].toInt().toString() else f.second[1]
                        filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey("filmrolls"), Pair(f.first, searchStr)))
                    },
                    onSearchClick = {
                        val title = getString(R.string.title_dialog_search_by_tags)
                        mainViewModel.requestSearch(title) { checkedLabels ->
                            if (checkedLabels.isNotEmpty()) {
                                navController.navigate(SearchRoute(tags = checkedLabels.joinToString(",")))
                            }
                        }
                    },
                    onPinFilterClick = { mainViewModel.addPinnedFilter(mainViewModel.currentFilter) },
                    onUnpinFilterClick = { mainViewModel.removePinnedFilter(mainViewModel.currentFilter) },
                    getPinnedFilters = { mainViewModel.getPinnedFilters() },
                    getFilterLabel = { mainViewModel.getFilterLabel(it) }
                )
            }

            TrisquelNavHost(
                navController = navController,
                modifier = Modifier,
                mainTopBar = mainTopBar,
                mainViewModel = mainViewModel,
                filmRollViewModel = filmRollViewModel,
                cameraViewModel = cameraViewModel,
                lensViewModel = lensViewModel,
                accessoryViewModel = accessoryViewModel,
                onFilmRollDeleteRequest = { filmRollViewModel.requestDelete(it) },
                onCameraDeleteRequest = { cameraViewModel.requestDeleteCamera(it) },
                onLensDeleteRequest = { lensViewModel.requestDeleteLens(it) },
                onAccessoryDeleteRequest = { accessoryViewModel.requestDeleteAccessory(it) },
                onPhotoInteraction = { photo, list ->
                    if (photo != null) {
                        navController.navigate(GalleryRoute(initialPhotoId = photo.id, photoIds = list.mapNotNull { it?.id }))
                    }
                }
            )
        }
    }

    private fun handleSort(
        routeKey: String,
        which: Int,
        filmRollViewModel: FilmRollViewModel,
        cameraViewModel: CameraViewModel,
        lensViewModel: LensViewModel,
        accessoryViewModel: AccessoryViewModel
    ) {
        userPreferencesRepository.setSortKey(routeKey, which)

        when (routeKey) {
            "filmrolls" -> filmRollViewModel.updateViewRule(Pair(which, filmRollViewModel.viewRule.value.second))
            "cameras" -> cameraViewModel.changeSortKey(which)
            "lenses" -> lensViewModel.changeSortKey(which)
            "accessories" -> accessoryViewModel.updateSortingRule(which)
        }
    }
}
