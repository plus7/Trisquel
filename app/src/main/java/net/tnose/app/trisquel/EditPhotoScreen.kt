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
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CheckListDialog(
    title: String,
    items: List<String>,
    initialCheckedIndices: List<Int>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val checkedStates = rememberSaveable { mutableStateOf(items.indices.map { initialCheckedIndices.contains(it) }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            LazyRow {
                item {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        items.forEachIndexed { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentList = checkedStates.value.toMutableList()
                                        currentList[index] = !currentList[index]
                                        checkedStates.value = currentList
                                    }
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = checkedStates.value[index],
                                    onCheckedChange = { isChecked ->
                                        val currentList = checkedStates.value.toMutableList()
                                        currentList[index] = isChecked
                                        checkedStates.value = currentList
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = item)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val checkedIndices = checkedStates.value
                        .mapIndexedNotNull { index, isChecked -> if (isChecked) index else null }
                    onConfirm(checkedIndices)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun EditPhotoRoute(
    id: Int,
    filmRollId: Int,
    frameIndex: Int,
    onCancel: () -> Unit,
    onNavigateToEditLens: () -> Unit,
    viewModel: EditPhotoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filmRoll by viewModel.filmRoll.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditPhotoEvent.SaveSuccess -> onCancel()
                is EditPhotoEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    class PickImageUriContract : ActivityResultContract<Any, List<Uri>>() {
        override fun createIntent(context: Context, input: Any): Intent {
            return Matisse.from(context as Activity)
                .choose(MimeType.ofImage())
                .captureStrategy(CaptureStrategy(true, "net.tnose.app.trisquel.provider", "Camera"))
                .capture(true)
                .countable(true)
                .maxSelectable(40)
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
        val newPaths = uris.map { it.toString() }
        viewModel.onSupplementalImagesChange((uiState.supplementalImages + newPaths).distinct())
    }

    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.all { it.value }) {
            pickImageLauncher.launch(42)
        } else {
            Toast.makeText(context, context.getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    val getLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            if (bundle != null) {
                val newLat = bundle.getDouble("latitude")
                val newLng = bundle.getDouble("longitude")
                viewModel.onLatLngChange(newLat, newLng)
                viewModel.onLocationChange(bundle.getString("location") ?: "")
            }
        } else if (result.resultCode == MapsActivity.RESULT_DELETE) {
            viewModel.onLatLngChange(999.0, 999.0)
        }
    }

    val addLensLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras ?: return@rememberLauncherForActivityResult
            val l = androidx.core.os.BundleCompat.getParcelable(bundle, "lensspec", LensSpec::class.java)
            if (l != null) {
                viewModel.handleAddLensResult(l)
            }
        }
    }

    if (!uiState.isLoaded) return

    EditPhotoScreen(
        uiState = uiState,
        filmRoll = filmRoll,
        onBack = {
            if (!uiState.isDirty) onCancel()
            else { /* Handled in BackHandler within Screen */ }
        },
        onCancel = onCancel,
        onSave = { viewModel.save() },
        onDateChange = { viewModel.onDateChange(it) },
        onLensChange = { viewModel.onLensChange(it) },
        onApertureChange = { viewModel.onApertureChange(it) },
        onShutterSpeedChange = { viewModel.onShutterSpeedChange(it) },
        onFocalLengthProgressChange = { viewModel.onFocalLengthProgressChange(it) },
        onExpCompProgressChange = { viewModel.onExpCompProgressChange(it) },
        onTtlProgressChange = { viewModel.onTtlProgressChange(it) },
        onLocationChange = { viewModel.onLocationChange(it) },
        onMemoChange = { viewModel.onMemoChange(it) },
        onFavoriteChange = { viewModel.onFavoriteChange(it) },
        onAccessoriesChange = { viewModel.onAccessoriesChange(it) },
        onAddTag = { viewModel.onAddTag(it) },
        onTagCheckedChange = { index, checked -> viewModel.onTagCheckedChange(index, checked) },
        onSupplementalImagesChange = { viewModel.onSupplementalImagesChange(it) },
        onOpenImagePicker = {
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
                permissionsLauncher.launch(permissions)
            } else {
                pickImageLauncher.launch(42)
            }
        },
        onOpenLocationPicker = {
            val intent = Intent(context, MapsActivity::class.java)
            if (uiState.latitude != 999.0 && uiState.longitude != 999.0) {
                intent.putExtra("latitude", uiState.latitude)
                intent.putExtra("longitude", uiState.longitude)
            }
            getLocationLauncher.launch(intent)
        },
        onCopyPhotoText = {
            val text = viewModel.getPhotoText()
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("", text))
            Toast.makeText(context, context.getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
        },
        onNavigateToEditLens = onNavigateToEditLens,
        onMountAdaptersChanged = { mount, selected -> viewModel.onMountAdaptersChanged(mount, selected) },
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPhotoScreen(
    uiState: EditPhotoUiState,
    filmRoll: FilmRoll?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDateChange: (String) -> Unit,
    onLensChange: (Int) -> Unit,
    onApertureChange: (String) -> Unit,
    onShutterSpeedChange: (String) -> Unit,
    onFocalLengthProgressChange: (Int) -> Unit,
    onExpCompProgressChange: (Int) -> Unit,
    onTtlProgressChange: (Int) -> Unit,
    onLocationChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onAccessoriesChange: (List<Int>) -> Unit,
    onAddTag: (String) -> Unit,
    onTagCheckedChange: (Int, Boolean) -> Unit,
    onSupplementalImagesChange: (List<String>) -> Unit,
    onOpenImagePicker: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    onCopyPhotoText: () -> Unit,
    onNavigateToEditLens: () -> Unit,
    onMountAdaptersChanged: (String, ArrayList<String>) -> Unit,
    viewModel: EditPhotoViewModel
) {
    val context = LocalContext.current
    val repo = remember { TrisquelRepo(context.applicationContext as android.app.Application) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showAskCreateLensDialog by rememberSaveable { mutableStateOf(false) }

    val canSave = uiState.lensList.any { it.id == uiState.lensId }

    val onBackPressed = {
        if (!uiState.isDirty) onCancel()
        else {
            if (canSave) showSaveDialog = true else showDiscardDialog = true
        }
    }

    BackHandler(onBack = onBackPressed)

    if (showSaveDialog) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.msg_save_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false; onSave() }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; onCancel() }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.msg_continue_editing_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false; onCancel() }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showAskCreateLensDialog) {
        AlertDialog(
            onDismissRequest = { showAskCreateLensDialog = false },
            title = null,
            text = { Text(stringResource(R.string.msg_ask_create_lens)) },
            confirmButton = {
                TextButton(onClick = {
                    showAskCreateLensDialog = false
                    onNavigateToEditLens()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskCreateLensDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    if (showDatePicker) {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val initialDateMillis = try {
            val localDate = LocalDate.parse(uiState.date, formatter)
            localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateChange(localDate.format(formatter))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    var showAccessoryDialog by rememberSaveable { mutableStateOf(false) }
    if (showAccessoryDialog) {
        var accessories by remember { mutableStateOf<List<Accessory>>(emptyList()) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val list = repo.getAllAccessoriesRaw().map { Accessory.fromEntity(it) }
                accessories = list
            }
        }
        CheckListDialog(
            title = stringResource(R.string.title_dialog_select_accessories),
            items = accessories.map { it.name },
            initialCheckedIndices = accessories.mapIndexedNotNull { index, accessory -> 
                if (uiState.selectedAccessories.contains(accessory.id)) index else null
            },
            onConfirm = { checkedIndices ->
                showAccessoryDialog = false
                onAccessoriesChange(checkedIndices.map { accessories[it].id })
            },
            onDismiss = { showAccessoryDialog = false }
        )
    }

    var showMountAdaptersDialog by rememberSaveable { mutableStateOf(false) }
    if (showMountAdaptersDialog) {
        var availableLensMounts by remember { mutableStateOf<List<String>>(emptyList()) }
        val mount = filmRoll?.camera?.mount
        val userPrefs = remember { UserPreferencesRepository(context) }
        
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val list = repo.getAvailableMountList().toMutableList()
                if (mount != null) list.remove(mount)
                availableLensMounts = list
            }
        }

        val checkedMounts = if (mount != null) userPrefs.getSuggestListSub("mount_adapters", mount) else ArrayList<String>()
        val checkedIndices = checkedMounts.mapNotNull { m -> availableLensMounts.indexOf(m).let { if (it >= 0) it else null } }

        CheckListDialog(
            title = mount?.let { stringResource(R.string.msg_select_mount_adapters).replace("%s", it) } ?: "",
            items = availableLensMounts,
            initialCheckedIndices = checkedIndices,
            onConfirm = { checkedIndicesOutput ->
                showMountAdaptersDialog = false
                if (mount != null) {
                    val checkedItems = checkedIndicesOutput.map { availableLensMounts[it] }.toCollection(ArrayList())
                    onMountAdaptersChanged(mount, checkedItems)
                }
            },
            onDismiss = { showMountAdaptersDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.id > 0) R.string.title_activity_edit_photo else R.string.title_activity_add_photo)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCopyPhotoText) {
                        Icon(painterResource(android.R.drawable.ic_menu_share), contentDescription = "Copy")
                    }
                    IconButton(onClick = onSave, enabled = canSave) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date
            Box(modifier = Modifier.fillMaxWidth()) {
                ClassicTextField(
                    value = uiState.date,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_date),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            // Lens
            var expandedLens by rememberSaveable { mutableStateOf(false) }
            val selectedLens = uiState.lensList.find { it.id == uiState.lensId }
            val selectedLensText = selectedLens?.let { "${it.manufacturer} ${it.modelName}" } ?: ""
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expandedLens,
                    onExpandedChange = { 
                        if (uiState.lensList.isEmpty()) {
                            showAskCreateLensDialog = true
                        } else {
                            expandedLens = it 
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    val errorMsg = filmRoll?.camera?.mount?.let { context.getString(R.string.error_nolens).replace("%s", it) }
                    ClassicTextField(
                        value = selectedLensText,
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.label_lens),
                        isError = uiState.lensList.isEmpty() && errorMsg != null,
                        supportingText = if (uiState.lensList.isEmpty() && errorMsg != null) { { Text(errorMsg) } } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLens) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (uiState.lensList.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expandedLens, onDismissRequest = { expandedLens = false }) {
                            uiState.lensList.forEach { lensOption ->
                                DropdownMenuItem(
                                    text = { Text("${lensOption.manufacturer} ${lensOption.modelName}") },
                                    onClick = {
                                        onLensChange(lensOption.id)
                                        expandedLens = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = { showMountAdaptersDialog = true },
                    enabled = filmRoll?.camera?.type != 1
                ) {
                    Icon(
                        painter = painterResource(id = if (filmRoll?.camera?.type == 1) R.drawable.ic_mount_adapter_disabled else R.drawable.ic_mount_adapter_plane),
                        contentDescription = "Mount Adapters"
                    )
                }
            }

            // Aperture
            var expandedAperture by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedAperture,
                onExpandedChange = { if (uiState.apertureList.isNotEmpty()) expandedAperture = it }
            ) {
                ClassicTextField(
                    value = uiState.aperture,
                    onValueChange = { onApertureChange(it); expandedAperture = true },
                    readOnly = true,
                    label = stringResource(R.string.label_aperture),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAperture) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (uiState.apertureList.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = expandedAperture, onDismissRequest = { expandedAperture = false }) {
                        uiState.apertureList.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { onApertureChange(suggestion); expandedAperture = false })
                        }
                    }
                }
            }

            // Shutter Speed
            var expandedSs by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedSs,
                onExpandedChange = { expandedSs = it }
            ) {
                ClassicTextField(
                    value = uiState.shutterSpeed,
                    onValueChange = { onShutterSpeedChange(it); expandedSs = true },
                    readOnly = true,
                    label = stringResource(R.string.label_shutter_speed),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSs) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (uiState.ssList.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = expandedSs, onDismissRequest = { expandedSs = false }) {
                        uiState.ssList.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { onShutterSpeedChange(suggestion); expandedSs = false })
                        }
                    }
                }
            }

            // Focal Length
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.label_focal_length), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val currentFl = (uiState.focalLengthProgress + uiState.focalLengthRange.first).toInt()
                    Text(text = "${currentFl}mm" + if (uiState.focalLengthRange.first == uiState.focalLengthRange.second) " (prime)" else "")
                    if (uiState.focalLengthRange.first != uiState.focalLengthRange.second) {
                        Slider(
                            value = uiState.focalLengthProgress.toFloat(),
                            onValueChange = { onFocalLengthProgressChange(it.toInt()) },
                            valueRange = 0f..(uiState.focalLengthRange.second - uiState.focalLengthRange.first).toFloat()
                        )
                    }
                }
            }

            // Exposure Compensation
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.label_exposure_compensation), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = viewModel.toHumanReadableCompensationAmount(uiState.expCompProgress))
                    Slider(
                        value = uiState.expCompProgress.toFloat(),
                        onValueChange = { onExpCompProgressChange(it.toInt()) },
                        valueRange = 0f..(uiState.evWidth * 2 * uiState.evGrainSize).toFloat(),
                        steps = uiState.evWidth * 2 * uiState.evGrainSize - 1
                    )
                }
            }

            // TTL Light Meter
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.label_ttl_light_meter), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = viewModel.toHumanReadableCompensationAmount(uiState.ttlProgress))
                    Slider(
                        value = uiState.ttlProgress.toFloat(),
                        onValueChange = { onTtlProgressChange(it.toInt()) },
                        valueRange = 0f..(uiState.evWidth * 2 * uiState.evGrainSize).toFloat(),
                        steps = uiState.evWidth * 2 * uiState.evGrainSize - 1
                    )
                }
            }

            // Accessories
            Box(modifier = Modifier.fillMaxWidth()) {
                ClassicTextField(
                    value = uiState.accessoriesStr,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_accessories),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.matchParentSize().clickable { showAccessoryDialog = true })
            }

            // Location
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ClassicTextField(
                    value = uiState.location,
                    onValueChange = { onLocationChange(it) },
                    label = stringResource(R.string.label_location),
                    supportingText = if (uiState.latitude != 999.0 && uiState.longitude != 999.0) {
                        { Text("${stringResource(R.string.label_coordinate)}: ${uiState.latitude}, ${uiState.longitude}") }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onOpenLocationPicker,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (uiState.latitude == 999.0 || uiState.longitude == 999.0) R.drawable.ic_place_gray_24dp else R.drawable.ic_place_black_24dp),
                        contentDescription = "Get Location",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            // Memo
            ClassicTextField(
                value = uiState.memo,
                onValueChange = { onMemoChange(it) },
                label = stringResource(R.string.label_memo),
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
            )

            // Supplemental Images
            Text(text = stringResource(R.string.label_supplementary_images), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                items(uiState.supplementalImages) { path ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable {
                                val intent = Intent()
                                val photoURI = if(path.startsWith("/")) {
                                    val file = File(path)
                                    FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
                                } else {
                                    Uri.parse(path)
                                }
                                intent.action = Intent.ACTION_VIEW
                                intent.setDataAndType(photoURI, "image/*")
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                context.startActivity(intent)
                            }
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    adjustViewBounds = true
                                }
                            },
                            update = { view ->
                                com.bumptech.glide.Glide.with(view)
                                    .load(if (path.startsWith("/")) File(path) else Uri.parse(path))
                                    .into(view)
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                        IconButton(
                            onClick = { onSupplementalImagesChange(uiState.supplementalImages - path) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                item {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_image_gray),
                        contentDescription = "Add Image",
                        modifier = Modifier
                            .size(150.dp)
                            .clickable { onOpenImagePicker() }
                    )
                }
            }

            // Tags
            Text(text = stringResource(R.string.label_tags), color = MaterialTheme.colorScheme.onSurfaceVariant)
            var tagInput by rememberSaveable { mutableStateOf("") }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ClassicTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = "",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onAddTag(tagInput)
                        tagInput = ""
                    },
                    enabled = tagInput.isNotEmpty() && !uiState.allTags.contains(tagInput)
                ) {
                    Text(stringResource(R.string.label_add))
                }
            }
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.allTags.forEachIndexed { index, tag ->
                    FilterChip(
                        selected = uiState.tagCheckedStates[index],
                        onClick = { onTagCheckedChange(index, !uiState.tagCheckedStates[index]) },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
