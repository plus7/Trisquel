package net.tnose.app.trisquel

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.regex.Pattern

private val mFArray = listOf(0.95, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.2, 2.4, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 4.8, 5.0, 5.6, 6.3, 6.7, 7.1, 8.0, 9.0, 9.5, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0, 22.0)

class EditCameraViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)

    val idInput: Int = savedStateHandle.get<Int>("id") ?: -1
    val type: Int = savedStateHandle.get<Int>("type") ?: 0
    val id = if (idInput < 0) 0 else idInput

    private val _uiState = MutableStateFlow(EditCameraUiState(id = id, type = type))
    val uiState: StateFlow<EditCameraUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (id > 0) {
                val entity = repo.getCamera(id)
                if (entity != null) {
                    val c = CameraSpec.fromEntity(entity)
                    var lensName = ""
                    var focalLength = ""
                    var fSteps = emptySet<Double>()
                    if (type == 1) {
                        val lEntity = repo.getLensByFixedBody(id)
                        if (lEntity != null) {
                            val l = LensSpec.fromEntity(lEntity)
                            lensName = l.modelName
                            focalLength = l.focalLength
                            fSteps = l.fSteps.toSet()
                        }
                    }
                    _uiState.update { it.copy(
                        isLoaded = true,
                        created = Util.dateToStringUTC(c.created),
                        manufacturer = c.manufacturer,
                        mount = c.mount,
                        modelName = c.modelName,
                        format = if (c.format < 0) 0 else c.format,
                        ssGrainSize = c.shutterSpeedGrainSize,
                        ssCustomSteps = c.shutterSpeedSteps.map { Util.doubleToStringShutterSpeed(it) },
                        fastestSs = Util.doubleToStringShutterSpeed(c.fastestShutterSpeed ?: 0.0),
                        slowestSs = Util.doubleToStringShutterSpeed(c.slowestShutterSpeed ?: 0.0),
                        bulbAvailable = c.bulbAvailable,
                        evGrainSize = c.evGrainSize,
                        evWidth = c.evWidth,
                        lensName = lensName,
                        focalLength = focalLength,
                        fSteps = fSteps
                    ) }
                } else {
                    _uiState.update { it.copy(isLoaded = true) }
                }
            } else {
                _uiState.update { it.copy(isLoaded = true) }
            }
        }
    }

    fun save(
        manufacturer: String,
        mount: String,
        model: String,
        format: Int,
        fastestSs: String,
        slowestSs: String,
        bulbAvailable: Boolean,
        ssCustomSteps: List<String>,
        ssGrainSize: Int,
        evGrainSize: Int,
        evWidth: Int,
        lensName: String,
        focalLength: String,
        fSteps: Set<Double>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val fStepsString = mFArray.filter { fSteps.contains(it) }.joinToString(", ")
            val fastestSsDouble = Util.stringToDoubleShutterSpeed(fastestSs)
            val slowestSsDouble = Util.stringToDoubleShutterSpeed(slowestSs)

            val created = if (state.created.isNotEmpty()) state.created else Util.dateToStringUTC(Date())
            val updated = Util.dateToStringUTC(Date())

            val c = CameraSpec(
                id = state.id,
                type = type,
                created = created,
                lastModified = updated,
                mount = mount,
                manufacturer = manufacturer,
                modelName = model,
                format = format,
                shutterSpeedGrainSize = ssGrainSize,
                fastestShutterSpeed = if (fastestSsDouble != 0.0) fastestSsDouble else null,
                slowestShutterSpeed = if (slowestSsDouble != 0.0) slowestSsDouble else null,
                bulbAvailable = bulbAvailable,
                shutterSpeedSteps = ssCustomSteps.map { Util.stringToDoubleShutterSpeed(it).toString() }.joinToString(","),
                evGrainSize = evGrainSize,
                evWidth = evWidth
            )
            
            val newCameraId = repo.upsertCamera(c.toEntity()).toInt()
            val actualCameraId = if (state.id > 0) state.id else newCameraId

            if (type == 1) {
                val existingLens = repo.getLensByFixedBody(actualCameraId)
                val l = LensSpec(
                    id = existingLens?.id ?: 0,
                    created = if (existingLens != null) existingLens.created else updated,
                    lastModified = updated,
                    mount = "",
                    body = actualCameraId,
                    manufacturer = manufacturer,
                    modelName = lensName,
                    focalLength = focalLength,
                    fSteps = fStepsString
                )
                repo.upsertLens(l.toEntity())
            }
            
            _isSaved.value = true
        }
    }
}

data class EditCameraUiState(
    val id: Int = 0,
    val isLoaded: Boolean = false,
    val type: Int = 0,
    val created: String = "",
    val manufacturer: String = "",
    val mount: String = "",
    val modelName: String = "",
    val format: Int = 0,
    val ssGrainSize: Int = 1,
    val ssCustomSteps: List<String> = emptyList(),
    val fastestSs: String = "",
    val slowestSs: String = "",
    val bulbAvailable: Boolean = false,
    val evGrainSize: Int = 1,
    val evWidth: Int = 1,
    val lensName: String = "",
    val focalLength: String = "",
    val fSteps: Set<Double> = emptySet(),
    val isDirty: Boolean = false
)

@Composable
fun EditCameraRoute(
    id: Int,
    type: Int,
    onSaveSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EditCameraViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onSaveSuccess()
        }
    }

    if (!uiState.isLoaded) return

    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    
    val titleRes = if (type == 1) {
        if (id < 0) R.string.title_activity_reg_cam_and_lens else R.string.title_activity_edit_cam_and_lens
    } else {
        if (id < 0) R.string.title_activity_reg_cam else R.string.title_activity_edit_cam
    }

    EditCameraScreen(
        title = stringResource(id = titleRes),
        type = type,
        initManufacturer = uiState.manufacturer,
        initMount = uiState.mount,
        initModel = uiState.modelName,
        initFormat = uiState.format,
        initSsCustomSteps = uiState.ssCustomSteps,
        initSsGrainSize = uiState.ssGrainSize,
        initFastestSs = uiState.fastestSs,
        initSlowestSs = uiState.slowestSs,
        initBulbAvailable = uiState.bulbAvailable,
        initEvGrainSize = uiState.evGrainSize,
        initEvWidth = uiState.evWidth,
        initLensName = uiState.lensName,
        initFocalLength = uiState.focalLength,
        initFSteps = uiState.fSteps,
        suggestedManufacturers = userPreferencesRepository.getSuggestList("camera_manufacturer", R.array.camera_manufacturer),
        suggestedMounts = userPreferencesRepository.getSuggestList("camera_mounts", R.array.camera_mounts),
        onSave = { manufacturer, mount, model, format, fastestSs, slowestSs, bulbAvailable, ssCustomSteps, ssGrainSize, evGrainSize, evWidth, lensName, focalLength, fSteps ->
            userPreferencesRepository.saveSuggestList("camera_manufacturer", R.array.camera_manufacturer, arrayOf(manufacturer))
            if (type == 0) {
                userPreferencesRepository.saveSuggestList("camera_mounts", R.array.camera_mounts, arrayOf(mount))
            }
            viewModel.save(manufacturer, mount, model, format, fastestSs, slowestSs, bulbAvailable, ssCustomSteps, ssGrainSize, evGrainSize, evWidth, lensName, focalLength, fSteps)
        },
        onCancel = onCancel
    )
}

@Composable
fun ShutterSpeedCustomizeDialog(
    title: String,
    message: String,
    defaultValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(defaultValue) }
    
    val regex = Regex("(1/\\d+|\\d+\\.\\d+|\\d+)")
    val lines = text.split("\n")
    val isValid = text.isNotEmpty() && lines.all { it.matches(regex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                if (message.isNotEmpty()) {
                    Text(text = message, modifier = Modifier.padding(bottom = 12.dp))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                    isError = !isValid && text.isNotEmpty(),
                    visualTransformation = { annotatedString ->
                        val builder = AnnotatedString.Builder()
                        val textStr = annotatedString.text
                        val linesSplit = textStr.split("\n")
                        var startIndex = 0
                        for (i in linesSplit.indices) {
                            val line = linesSplit[i]
                            val match = line.matches(regex)
                            if (!match) {
                                builder.withStyle(SpanStyle(color = Color.Red)) {
                                    append(line)
                                }
                            } else {
                                builder.append(line)
                            }
                            if (i < linesSplit.size - 1) {
                                builder.append("\n")
                            }
                            startIndex += line.length + 1
                        }
                        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = isValid
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCameraScreen(
    title: String,
    type: Int,
    initManufacturer: String,
    initMount: String,
    initModel: String,
    initFormat: Int,
    initSsCustomSteps: List<String>,
    initSsGrainSize: Int,
    initFastestSs: String,
    initSlowestSs: String,
    initBulbAvailable: Boolean,
    initEvGrainSize: Int,
    initEvWidth: Int,
    initLensName: String,
    initFocalLength: String,
    initFSteps: Set<Double>,
    suggestedManufacturers: List<String>,
    suggestedMounts: List<String>,
    onSave: (String, String, String, Int, String, String, Boolean, List<String>, Int, Int, Int, String, String, Set<Double>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var manufacturer by rememberSaveable { mutableStateOf(initManufacturer) }
    var mount by rememberSaveable { mutableStateOf(initMount) }
    var model by rememberSaveable { mutableStateOf(initModel) }
    var format by rememberSaveable { mutableIntStateOf(initFormat) }
    var fastestSs by rememberSaveable { mutableStateOf(initFastestSs) }
    var slowestSs by rememberSaveable { mutableStateOf(initSlowestSs) }
    var bulbAvailable by rememberSaveable { mutableStateOf(initBulbAvailable) }
    var evGrainSize by rememberSaveable { mutableIntStateOf(initEvGrainSize) }
    var evWidth by rememberSaveable { mutableIntStateOf(initEvWidth) }

    var lensName by rememberSaveable { mutableStateOf(initLensName) }
    var focalLength by rememberSaveable { mutableStateOf(initFocalLength) }
    var fSteps by rememberSaveable { mutableStateOf(initFSteps) }

    var ssCustomSteps by rememberSaveable { mutableStateOf(initSsCustomSteps) }
    var ssGrainSize by rememberSaveable { mutableIntStateOf(initSsGrainSize) }
    var previousCheckedSsSteps by rememberSaveable { mutableIntStateOf(initSsGrainSize) }
    var isDirty by rememberSaveable { mutableStateOf(false) }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomSsDialog by rememberSaveable { mutableStateOf(false) }

    val formatList = stringArrayResource(id = R.array.film_formats).toList()
    val evGrainSizeList = stringArrayResource(id = R.array.ev_grain_size_list).toList()
    val evWidthList = stringArrayResource(id = R.array.ev_width_list).toList()

    val ssListOne = stringArrayResource(id = R.array.shutter_speeds_one).toList()
    val ssListHalf = stringArrayResource(id = R.array.shutter_speeds_half).toList()
    val ssListOneThird = stringArrayResource(id = R.array.shutter_speeds_one_third).toList()
    
    val currentSsList = remember(ssGrainSize, ssCustomSteps) {
        when (ssGrainSize) {
            1 -> ssListOne
            2 -> ssListHalf
            3 -> ssListOneThird
            else -> ssCustomSteps
        }
    }

    val focalLengthOk = remember(focalLength) {
        if (focalLength.isNotEmpty()) {
            val zoom = Pattern.compile("(\\d++)-(\\d++)")
            if (zoom.matcher(focalLength).find()) return@remember true

            val prime = Pattern.compile("(\\d++)")
            if (prime.matcher(focalLength).find()) return@remember true
        }
        false
    }

    val fSSDouble = Util.stringToDoubleShutterSpeed(fastestSs)
    val sSSDouble = Util.stringToDoubleShutterSpeed(slowestSs)
    val ssInconsistent = (fSSDouble != 0.0 && sSSDouble != 0.0 && fSSDouble > sSSDouble)
    
    val shutterSpeedRangeOk = if (fSSDouble == 0.0 || sSSDouble == 0.0) false else !ssInconsistent

    val cameraOk = (type == 1 || (type == 0 && mount.isNotEmpty())) && model.isNotEmpty() && shutterSpeedRangeOk
    val lensOk = if (type == 1) (focalLengthOk && fSteps.isNotEmpty()) else true
    val canSave = cameraOk && lensOk

    val onBackPressed = {
        if (!isDirty) {
            onCancel()
        } else {
            if (canSave) showSaveDialog = true else showDiscardDialog = true
        }
    }

    BackHandler(onBack = onBackPressed)

    if (showSaveDialog) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = {  },
            title = { Text(stringResource(R.string.msg_save_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {
                    onSave(manufacturer, mount, model, format, fastestSs, slowestSs, bulbAvailable, ssCustomSteps, ssGrainSize, evGrainSize, evWidth, lensName, focalLength, fSteps)
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onCancel()
                }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = {  },
            title = { Text(stringResource(R.string.msg_continue_editing_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {  }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onCancel()
                }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showCustomSsDialog) {
        val defaultValue = if (ssCustomSteps.isEmpty()) {
            context.resources.getStringArray(R.array.shutter_speeds_one).joinToString("\n")
        } else {
            ssCustomSteps.joinToString("\n")
        }
        
        ShutterSpeedCustomizeDialog(
            title = stringResource(R.string.title_dialog_custom_ss),
            message = stringResource(R.string.msg_dialog_custom_ss),
            defaultValue = defaultValue,
            onConfirm = { valueStr ->
                val list = valueStr.split("\n").filter { it.isNotEmpty() }.sortedBy { Util.stringToDoubleShutterSpeed(it) }
                ssCustomSteps = list
                previousCheckedSsSteps = 0
                isDirty = true
            },
            onDismiss = {
                ssGrainSize = previousCheckedSsSteps
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onSave(manufacturer, mount, model, format, fastestSs, slowestSs, bulbAvailable, ssCustomSteps, ssGrainSize, evGrainSize, evWidth, lensName, focalLength, fSteps) },
                        enabled = canSave
                    ) {
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
            // Mount (only interchangeable)
            if (type == 0) {
                var expandedMount by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMount,
                    onExpandedChange = { expandedMount = it }
                ) {
                    ClassicTextField(
                        value = mount,
                        onValueChange = { mount = it; isDirty = true; expandedMount = true },
                        label = stringResource(R.string.label_mount),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMount) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    val filteredMounts = suggestedMounts.filter { it.contains(mount, ignoreCase = true) }
                    if (filteredMounts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedMount,
                            onDismissRequest = { expandedMount = false }
                        ) {
                            filteredMounts.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        mount = suggestion
                                        isDirty = true
                                        expandedMount = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Manufacturer
            var expandedManufacturer by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedManufacturer,
                onExpandedChange = { expandedManufacturer = it }
            ) {
                ClassicTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it; isDirty = true; expandedManufacturer = true },
                    label = stringResource(R.string.label_manufacturer),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedManufacturer) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                val filteredManufacturers = suggestedManufacturers.filter { it.contains(manufacturer, ignoreCase = true) }
                if (filteredManufacturers.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedManufacturer,
                        onDismissRequest = { expandedManufacturer = false }
                    ) {
                        filteredManufacturers.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    manufacturer = suggestion
                                    isDirty = true
                                    expandedManufacturer = false
                                }
                            )
                        }
                    }
                }
            }

            // Model
            ClassicTextField(
                value = model,
                onValueChange = {
                    model = it
                    isDirty = true
                    /*if (it == "jenkinsushi") {
                        android.widget.Toast.makeText(context, stringResource(R.string.google_maps_key), android.widget.Toast.LENGTH_LONG).show()
                    }*/
                },
                label = stringResource(R.string.label_model),
                modifier = Modifier.fillMaxWidth()
            )

            // Format
            var expandedFormat by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedFormat,
                onExpandedChange = { expandedFormat = it }
            ) {
                ClassicTextField(
                    value = formatList.getOrNull(format) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_format),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFormat) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedFormat,
                    onDismissRequest = { expandedFormat = false }
                ) {
                    formatList.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                format = index
                                isDirty = true
                                expandedFormat = false
                            }
                        )
                    }
                }
            }

            // Shutter Speed Steps RadioGroup
            Text(
                text = stringResource(R.string.label_shutter_speed_step),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val stepOptions = listOf(
                    stringResource(R.string.label_shutter_speed_step_one) to 1,
                    stringResource(R.string.label_shutter_speed_step_half) to 2,
                    stringResource(R.string.label_shutter_speed_step_one_third) to 3,
                    stringResource(R.string.label_shutter_speed_step_custom) to 0
                )
                
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stepOptions.forEach { (text, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    ssGrainSize = value
                                    if (value != 0) {
                                        previousCheckedSsSteps = value
                                        isDirty = true
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = ssGrainSize == value,
                                onClick = null
                            )
                            Text(text = text, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }

            // Shutter speed range (Slowest to Fastest)
            Text(
                text = stringResource(R.string.label_shutter_speed_range),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Slowest SS
                var expandedSlowest by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedSlowest,
                    onExpandedChange = { expandedSlowest = it },
                    modifier = Modifier.weight(1f)
                ) {
                    ClassicTextField(
                        value = slowestSs,
                        onValueChange = { slowestSs = it; isDirty = true; expandedSlowest = true },
                        readOnly = true,
                        label = stringResource(R.string.label_shutter_speed_slowest),
                        isError = ssInconsistent,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSlowest) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    //val filteredSlowest = currentSsList.filter { it.contains(slowestSs, ignoreCase = true) }
                    //if (filteredSlowest.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expandedSlowest, onDismissRequest = { expandedSlowest = false }) {
                            currentSsList.forEach { suggestion ->
                                DropdownMenuItem(text = { Text(suggestion) }, onClick = { slowestSs = suggestion; isDirty = true; expandedSlowest = false })
                            }
                        }
                    //}
                }

                Text(text = stringResource(R.string.label_to))

                // Fastest SS
                var expandedFastest by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedFastest,
                    onExpandedChange = { expandedFastest = it },
                    modifier = Modifier.weight(1f)
                ) {
                    ClassicTextField(
                        value = fastestSs,
                        onValueChange = { fastestSs = it; isDirty = true; expandedFastest = true },
                        readOnly = true,
                        label = stringResource(R.string.label_shutter_speed_fastest),
                        isError = ssInconsistent,
                        supportingText = if (ssInconsistent) { { Text(stringResource(R.string.error_ss_inconsistent)) } } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFastest) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    //val filteredFastest = currentSsList.filter { it.contains(fastestSs, ignoreCase = true) }
                    //if (filteredFastest.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expandedFastest, onDismissRequest = { expandedFastest = false }) {
                            currentSsList.forEach { suggestion ->
                                DropdownMenuItem(text = { Text(suggestion) }, onClick = { fastestSs = suggestion; isDirty = true; expandedFastest = false })
                            }
                        }
                    //}
                }
            }

            // Bulb Available
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    bulbAvailable = !bulbAvailable
                    isDirty = true
                }
            ) {
                Checkbox(checked = bulbAvailable, onCheckedChange = null)
                Text(text = stringResource(R.string.label_bulb))
            }

            // EV compensation
            Text(
                text = stringResource(R.string.label_exposure_compensation),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.label_step))
                
                var expandedEvGrain by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedEvGrain, onExpandedChange = { expandedEvGrain = it }, modifier = Modifier.weight(1f)) {
                    ClassicTextField(
                        value = evGrainSizeList.getOrNull(evGrainSize - 1) ?: "",
                        onValueChange = {}, readOnly = true, label = "",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEvGrain) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedEvGrain, onDismissRequest = { expandedEvGrain = false }) {
                        evGrainSizeList.forEachIndexed { index, option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { evGrainSize = index + 1; isDirty = true; expandedEvGrain = false })
                        }
                    }
                }

                Text(text = stringResource(R.string.label_range))
                
                var expandedEvWidth by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedEvWidth, onExpandedChange = { expandedEvWidth = it }, modifier = Modifier.weight(1f)) {
                    ClassicTextField(
                        value = evWidthList.getOrNull(evWidth - 1) ?: "",
                        onValueChange = {}, readOnly = true, label = "",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEvWidth) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedEvWidth, onDismissRequest = { expandedEvWidth = false }) {
                        evWidthList.forEachIndexed { index, option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { evWidth = index + 1; isDirty = true; expandedEvWidth = false })
                        }
                    }
                }
            }

            // Fixed Lens attributes
            if (type == 1) {
                Spacer(modifier = Modifier.height(8.dp))
                ClassicTextField(
                    value = lensName,
                    onValueChange = { lensName = it; isDirty = true },
                    label = stringResource(R.string.label_lens_name),
                    modifier = Modifier.fillMaxWidth()
                )

                ClassicTextField(
                    value = focalLength,
                    onValueChange = { focalLength = it; isDirty = true },
                    label = stringResource(R.string.label_focal_length),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.label_f_stops),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    mFArray.forEach { fValue ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(84.dp).clickable {
                                val newSet = fSteps.toMutableSet()
                                if (fSteps.contains(fValue)) newSet.remove(fValue) else newSet.add(fValue)
                                fSteps = newSet
                                isDirty = true
                            }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = fSteps.contains(fValue), onCheckedChange = null)
                            Text(text = fValue.toString(), modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}