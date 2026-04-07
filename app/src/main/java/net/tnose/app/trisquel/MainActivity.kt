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
        const val ROUTE_FAVORITES = "favorites"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_LICENSE = "license"
        const val RELEASE_NOTES_URL = "https://x.com/trisquel_app"
        
        val TOP_LEVEL_ROUTES = listOf(ROUTE_FILMROLLS, ROUTE_CAMERAS, ROUTE_LENSES, ROUTE_ACCESSORIES, ROUTE_FAVORITES)
    }

    private val cameraViewModel: CameraViewModel by viewModels()
    private val lensViewModel: LensViewModel by viewModels()
    private val accessoryViewModel: AccessoryViewModel by viewModels()
    private val filmRollViewModel: FilmRollViewModel by viewModels()

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private val backupDirChosenForSlimExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            mainViewModel.onBackupDirChosen(result.data?.data, 0)
        }
    }

    private val backupDirChosenForFullExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            mainViewModel.onBackupDirChosen(result.data?.data, 1)
        }
    }

    private val zipFileChosenForMergeIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            mainViewModel.onImportFileChosen(result.data?.data, 0)
        }
    }

    private val zipFileChosenForReplIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            mainViewModel.onImportFileChosen(result.data?.data, 1)
        }
    }

    private val dbFileChosenForReplDbLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            mainViewModel.onDbFileChosen(result.data?.data, contentResolver)
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
                                val chooserIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                chooserIntent.addCategory(Intent.CATEGORY_OPENABLE)
                                chooserIntent.type = "application/zip"
                                chooserIntent.putExtra(Intent.EXTRA_TITLE, event.fileName)
                                if(event.mode == 0) backupDirChosenForSlimExLauncher.launch(chooserIntent)
                                else backupDirChosenForFullExLauncher.launch(chooserIntent)
                            }
                            is MainEvent.LaunchImportDocumentPicker -> {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                if(event.mode == 2){
                                    intent.type = "*/*"
                                }else {
                                    intent.type = "application/zip"
                                }
                                when(event.mode){
                                    0 -> zipFileChosenForMergeIpLauncher.launch(intent)
                                    1 -> zipFileChosenForReplIpLauncher.launch(intent)
                                    else -> dbFileChosenForReplDbLauncher.launch(intent)
                                }
                            }
                        }
                    }
                }
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
        }

        setContent {
            TrisquelTheme {
                MainAppScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        cameraViewModel.load()
        lensViewModel.load()

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

    fun onCameraDeleteRequest(item: CameraSpec) {
        cameraViewModel.requestDeleteCamera(item)
    }

    fun onLensDeleteRequest(item: LensSpec) {
        lensViewModel.requestDeleteLens(item)
    }

    fun onFilmRollDeleteRequest(item: FilmRoll) {
        filmRollViewModel.requestDelete(item)
    }

    fun onAccessoryDeleteRequest(accessory: Accessory) {
        accessoryViewModel.requestDeleteAccessory(accessory)
    }

    fun onPhotoInteraction(item: Photo?, list: List<Photo?>) {
        val intent = Intent(application, GalleryActivity::class.java)
        intent.putExtra("photo", item)
        intent.putParcelableArrayListExtra("favList", ArrayList(list))
        startActivity(intent)
    }

    private fun handleSort(route: String, which: Int) {
        userPreferencesRepository.setSortKey(route, which)

        when (route) {
            ROUTE_FILMROLLS -> filmRollViewModel.updateViewRule(Pair(which, filmRollViewModel.viewRule.value.second))
            ROUTE_CAMERAS -> cameraViewModel.changeSortKey(which)
            ROUTE_LENSES -> lensViewModel.changeSortKey(which)
            ROUTE_ACCESSORIES -> accessoryViewModel.updateSortingRule(which)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppScreen() {
        val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val observedRoute = navBackStackEntry?.destination?.route ?: mainViewModel.startRoute
        val observedArgs = navBackStackEntry?.arguments

        LaunchedEffect(observedRoute, observedArgs) {
            mainViewModel.currentRoute = observedRoute
            mainViewModel.currentArguments = observedArgs
            if (observedRoute in TOP_LEVEL_ROUTES) {
                mainViewModel.startRoute = observedRoute
            }
        }

        TrisquelDialogManager(
            activeDialog = mainViewModel.activeDialog,
            onDismiss = { mainViewModel.dismissDialog() }
        )

        TrisquelNavigationDrawer(
            drawerState = drawerState,
            navController = navController,
            observedRoute = observedRoute,
            scope = scope,
            gesturesEnabled = observedRoute in listOf(ROUTE_FILMROLLS, ROUTE_CAMERAS, ROUTE_LENSES, ROUTE_ACCESSORIES, ROUTE_FAVORITES),
            onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
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
            onLicenseClick = { navController.navigate(ROUTE_LICENSE) }
        ) {
            val mainTopBar: @Composable (String) -> Unit = { route ->
                TrisquelTopAppBar(
                    observedRoute = route,
                    currentSubtitle = mainViewModel.currentSubtitle,
                    currentFilter = mainViewModel.currentFilter,
                    drawerState = drawerState,
                    scope = scope,
                    onSortClick = {
                        val arr = when(route){
                            ROUTE_FILMROLLS -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_camera), getString(R.string.label_brand))
                            ROUTE_CAMERAS -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_format))
                            ROUTE_LENSES -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_focal_length))
                            ROUTE_ACCESSORIES -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_accessory_type))
                            else -> arrayOf()
                        }
                        val key = userPreferencesRepository.getSortKey(route)
                        mainViewModel.showDialog(ActiveDialog.SingleChoice(
                            title = getString(R.string.label_sort_by),
                            items = arr,
                            selected = key,
                            onConfirm = { handleSort(route, it) }
                        ))
                    },
                    onFilterNoFilterClick = {
                        mainViewModel.currentFilter = Pair(0, arrayListOf())
                        filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(0, "")))
                    },
                    onFilterByCameraClick = {
                        mainViewModel.requestFilterByCamera { _, id ->
                            if (id != null) {
                                mainViewModel.currentFilter = Pair(1, arrayListOf(id.toString()))
                                filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(1, id.toString())))
                            }
                        }
                    },
                    onFilterByFilmBrandClick = {
                        mainViewModel.requestFilterByFilmBrand { fb ->
                            mainViewModel.currentFilter = Pair(2, arrayListOf(fb.first, fb.second))
                            filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(2, fb.second)))
                        }
                    },
                    onPinnedFilterClick = { f ->
                        mainViewModel.currentFilter = f
                        val searchStr = if(f.first == 1) f.second[0].toInt().toString() else f.second[1]
                        filmRollViewModel.updateViewRule(Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(f.first, searchStr)))
                    },
                    onSearchClick = {
                        val title = getString(R.string.title_dialog_search_by_tags)
                        mainViewModel.requestSearch(title) { checkedLabels ->
                            if (checkedLabels.isNotEmpty()) {
                                navController.navigate("search?tags=${checkedLabels.joinToString(",")}")
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
                initialRoute = mainViewModel.startRoute,
                modifier = Modifier,
                mainTopBar = mainTopBar,
                mainViewModel = mainViewModel,
                filmRollViewModel = filmRollViewModel,
                cameraViewModel = cameraViewModel,
                lensViewModel = lensViewModel,
                accessoryViewModel = accessoryViewModel,
                onFilmRollDeleteRequest = { onFilmRollDeleteRequest(it) },
                onCameraDeleteRequest = { onCameraDeleteRequest(it) },
                onLensDeleteRequest = { onLensDeleteRequest(it) },
                onAccessoryDeleteRequest = { onAccessoryDeleteRequest(it) },
                onPhotoInteraction = { photo, list -> onPhotoInteraction(photo, list) }
            )
        }
    }
}
