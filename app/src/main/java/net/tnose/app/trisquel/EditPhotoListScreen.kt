package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import kotlinx.coroutines.launch

@Composable
fun EditPhotoListRoute(
    id: Int,
    onBack: () -> Unit,
    onNavigateToEditFilmRoll: (Int) -> Unit,
    onNavigateToEditPhoto: (Int, Int, Int) -> Unit,
    viewModel: EditPhotoListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filmRoll by viewModel.filmRoll.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    class PickImageUriContract : ActivityResultContract<Any, List<Uri>>() {
        override fun createIntent(context: Context, input: Any): Intent {
            return Matisse.from(context as Activity)
                .choose(MimeType.ofImage())
                .captureStrategy(CaptureStrategy(true, "net.tnose.app.trisquel.provider", "Camera"))
                .capture(true)
                .countable(true)
                .maxSelectable(1)
                .thumbnailScale(0.85f)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .imageEngine(Glide4Engine())
                .createIntent()!!
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return if (resultCode == Activity.RESULT_OK) Matisse.obtainResult(intent) else emptyList()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(PickImageUriContract()) { uris ->
        viewModel.handlePickImageResult(uris)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            pickImageLauncher.launch(42)
        } else {
            viewModel.setThumbnailEditingPhotoId(-1)
            Toast.makeText(context, context.getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    val checkPermAndEditThumbPhoto = { photo: Photo ->
        viewModel.setThumbnailEditingPhotoId(photo.id)
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }
        val readDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        }
        val cameraDenied = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED

        if (readDenied || cameraDenied) {
            requestPermissionLauncher.launch(permissions)
        } else {
            pickImageLauncher.launch(42)
        }
    }

    EditPhotoListScreen(
        filmRoll = filmRoll,
        photos = photos,
        isLoading = isLoading,
        onBack = onBack,
        onEditFilmRoll = { onNavigateToEditFilmRoll(id) },
        onCopyToClipboard = {
            scope.launch {
                val text = viewModel.getFilmRollClipboardText()
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("", text))
                Toast.makeText(context, context.getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
            }
        },
        onPrint = {
            val intent = Intent(context, PrintPreviewActivity::class.java)
            intent.putExtra("filmroll", id)
            context.startActivity(intent)
        },
        onAddPhoto = { onNavigateToEditPhoto(id, -1, -1) },
        onItemClick = { item -> onNavigateToEditPhoto(id, item.id, item.frameIndex) },
        onDeletePhoto = { viewModel.deletePhoto(it.id) },
        onAddPhotoSameIndex = { photo -> onNavigateToEditPhoto(id, -1, photo.frameIndex) },
        onUpdatePhotoIndex = { photo, newIndex ->
            photo.frameIndex = newIndex
            viewModel.updatePhoto(photo, null)
        },
        onShiftPhotoIndex = { photo, amount ->
            viewModel.shiftFrameIndexFrom(photo, amount)
        },
        onThumbnailClick = { item ->
            if (item.supplementalImages.isEmpty()) {
                checkPermAndEditThumbPhoto(item)
            } else {
                val intent = Intent(context, GalleryActivity::class.java)
                intent.putExtra("photo", item)
                intent.putParcelableArrayListExtra("favList", ArrayList(photos.map { Photo.fromEntity(it.second.photo) }))
                context.startActivity(intent)
            }
        },
        onFavoriteClick = { viewModel.toggleFavPhoto(it) },
        possibleDownShiftLimit = { viewModel.possibleDownShiftLimit(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhotoListScreen(
    filmRoll: FilmRoll?,
    photos: List<Pair<String, PhotoAndTagIds>>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onEditFilmRoll: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onPrint: () -> Unit,
    onAddPhoto: () -> Unit,
    onItemClick: (Photo) -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onAddPhotoSameIndex: (Photo) -> Unit,
    onUpdatePhotoIndex: (Photo, Int) -> Unit,
    onShiftPhotoIndex: (Photo, Int) -> Unit,
    onThumbnailClick: (Photo) -> Unit,
    onFavoriteClick: (Photo) -> Unit,
    possibleDownShiftLimit: (Photo) -> Int
) {
    var showOpDialog by rememberSaveable { mutableStateOf<Photo?>(null) }
    var showIndexDialog by rememberSaveable { mutableStateOf<Photo?>(null) }
    var showShiftDialog by rememberSaveable { mutableStateOf<Photo?>(null) }

    val titleText = if (filmRoll == null || filmRoll.name.isEmpty()) {
        stringResource(R.string.empty_name)
    } else {
        filmRoll.name
    }

    val subtitleText = filmRoll?.let {
        "${it.camera.manufacturer} ${it.camera.modelName} / ${it.manufacturer} ${it.brand}"
    } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, style = MaterialTheme.typography.titleLarge)
                        if (subtitleText.isNotEmpty()) {
                            Text(subtitleText, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit_film)) },
                            onClick = {
                                menuExpanded = false
                                onEditFilmRoll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_copy_to_clipboard)) },
                            onClick = {
                                menuExpanded = false
                                onCopyToClipboard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_print)) },
                            onClick = {
                                menuExpanded = false
                                onPrint()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPhoto,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            PhotoListScreen(
                photos = photos,
                onItemClick = onItemClick,
                onItemLongClick = { showOpDialog = it },
                onIndexClick = { showIndexDialog = it },
                onIndexLongClick = { showShiftDialog = it },
                onThumbnailClick = onThumbnailClick,
                onFavoriteClick = onFavoriteClick,
                isLoading = isLoading
            )
        }
    }

    if (showOpDialog != null) {
        val photo = showOpDialog!!
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showOpDialog = null },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Column {
                    val options = listOf(
                        stringResource(R.string.delete),
                        stringResource(R.string.add_photo_same_index)
                    )
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOpDialog = null
                                    when (index) {
                                        0 -> onDeletePhoto(photo)
                                        1 -> onAddPhotoSameIndex(photo)
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showIndexDialog != null) {
        val photo = showIndexDialog!!
        var input by remember { mutableStateOf((photo.frameIndex + 1).toString()) }
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showIndexDialog = null },
            title = { Text(stringResource(R.string.title_dialog_edit_index)) },
            text = {
                ClassicTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showIndexDialog = null
                    val newIndex = (input.toIntOrNull() ?: 1) - 1
                    if (newIndex >= 0 && photo.frameIndex != newIndex) {
                        onUpdatePhotoIndex(photo, newIndex)
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIndexDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showShiftDialog != null) {
        val photo = showShiftDialog!!
        var input by remember { mutableStateOf((photo.frameIndex + 1).toString()) }
        val downshiftLimit = possibleDownShiftLimit(photo)
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showShiftDialog = null },
            title = { Text(stringResource(R.string.title_dialog_shift_index)) },
            text = {
                Column {
                    Text(stringResource(R.string.msg_dialog_shift_index))
                    ClassicTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = stringResource(R.string.hint_dialog_shift_index).format(downshiftLimit + 1),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showShiftDialog = null
                    val newIndex = (input.toIntOrNull() ?: 1) - 1
                    if (newIndex >= downshiftLimit && photo.frameIndex != newIndex) {
                        onShiftPhotoIndex(photo, newIndex - photo.frameIndex)
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
