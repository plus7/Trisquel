package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import java.util.Date
import java.util.regex.Pattern

private val mFArray = listOf(0.95, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.2, 2.4, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 4.8, 5.0, 5.6, 6.3, 6.7, 7.1, 8.0, 9.0, 9.5, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0, 22.0)

class EditCameraActivity : AppCompatActivity() {
    private var id: Int = -1
    private var type: Int = 0
    private var created: String = ""
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    var ssCustomSteps by mutableStateOf<List<String>>(emptyList())
    var ssGrainSize by mutableIntStateOf(1) // 1=1 stop, 2=half, 3=third, 0=custom
    var previousCheckedSsSteps = 1
    var isDirty by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent
        id = data.getIntExtra("id", -1)
        type = data.getIntExtra("type", 0)

        var initManufacturer = ""
        var initMount = ""
        var initModel = ""
        var initFormat = FilmFormat.FULL_FRAME.ordinal
        var initFastestSs = ""
        var initSlowestSs = ""
        var initBulbAvailable = false
        var initEvGrainSize = 1
        var initEvWidth = 1
        var initLensName = ""
        var initFocalLength = ""
        var initFSteps = emptySet<Double>()

        if (id >= 0 && savedInstanceState == null) {
            val c = dao.getCamera(id)
            if (c != null) {
                created = Util.dateToStringUTC(c.created)
                initManufacturer = c.manufacturer
                initMount = c.mount
                initModel = c.modelName
                initFormat = if (c.format < 0) 0 else c.format
                ssGrainSize = c.shutterSpeedGrainSize
                previousCheckedSsSteps = ssGrainSize
                ssCustomSteps = c.shutterSpeedSteps.map { Util.doubleToStringShutterSpeed(it) }
                initFastestSs = Util.doubleToStringShutterSpeed(c.fastestShutterSpeed ?: 0.0)
                initSlowestSs = Util.doubleToStringShutterSpeed(c.slowestShutterSpeed ?: 0.0)
                initBulbAvailable = c.bulbAvailable
                initEvGrainSize = c.evGrainSize
                initEvWidth = c.evWidth
            }

            if (type == 1) {
                val l = dao.getLens(dao.getFixedLensIdByBody(id))
                if (l != null) {
                    initLensName = l.modelName
                    initFocalLength = l.focalLength
                    initFSteps = l.fSteps.toSet()
                }
            }
        } else if (savedInstanceState != null) {
            created = savedInstanceState.getString("created") ?: ""
            initManufacturer = savedInstanceState.getString("manufacturer") ?: ""
            initMount = savedInstanceState.getString("mount") ?: ""
            initModel = savedInstanceState.getString("model_name") ?: ""
            initFormat = savedInstanceState.getInt("format_position")
            ssGrainSize = savedInstanceState.getInt("ss_grain_size")
            previousCheckedSsSteps = savedInstanceState.getInt("previous_checked_ss_steps")
            ssCustomSteps = savedInstanceState.getStringArrayList("ss_custom_steps")?.toList() ?: emptyList()
            initFastestSs = savedInstanceState.getString("fastest_ss") ?: ""
            initSlowestSs = savedInstanceState.getString("slowest_ss") ?: ""
            initBulbAvailable = savedInstanceState.getBoolean("bulb_available")
            initEvGrainSize = savedInstanceState.getInt("ev_grain_size")
            initEvWidth = savedInstanceState.getInt("ev_width")
            
            if (type == 1) {
                initLensName = savedInstanceState.getString("fixedlens_name") ?: ""
                initFocalLength = savedInstanceState.getString("fixedlens_focal_length") ?: ""
                initFSteps = parseFSteps(savedInstanceState.getString("FStepsString") ?: "")
            }
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            created = ""
        }
        dao.close()

        val titleRes = if (type == 1) {
            if (id < 0) R.string.title_activity_reg_cam_and_lens else R.string.title_activity_edit_cam_and_lens
        } else {
            if (id < 0) R.string.title_activity_reg_cam else R.string.title_activity_edit_cam
        }

        setContent {
            TrisquelTheme {
                EditCameraScreen(
                    title = stringResource(id = titleRes),
                    type = type,
                    initManufacturer = initManufacturer,
                    initMount = initMount,
                    initModel = initModel,
                    initFormat = initFormat,
                    initFastestSs = initFastestSs,
                    initSlowestSs = initSlowestSs,
                    initBulbAvailable = initBulbAvailable,
                    initEvGrainSize = initEvGrainSize,
                    initEvWidth = initEvWidth,
                    initLensName = initLensName,
                    initFocalLength = initFocalLength,
                    initFSteps = initFSteps,
                    suggestedManufacturers = userPreferencesRepository.getSuggestList("camera_manufacturer", R.array.camera_manufacturer),
                    suggestedMounts = userPreferencesRepository.getSuggestList("camera_mounts", R.array.camera_mounts),
                    onCancel = {
                        setResult(RESULT_CANCELED, Intent())
                        finish()
                    }
                )
            }
        }
    }

    private fun parseFSteps(s: String): Set<Double> {
        if (s.isEmpty()) return emptySet()
        val fsAsArray = s.split(", ").filter { it.isNotEmpty() }
        val list = mutableSetOf<Double>()
        for (speed in fsAsArray) {
            speed.toDoubleOrNull()?.let { list.add(it) }
        }
        return list
    }

    fun saveAndFinish(
        manufacturer: String,
        mount: String,
        model: String,
        format: Int,
        fastestSs: String,
        slowestSs: String,
        bulbAvailable: Boolean,
        evGrainSize: Int,
        evWidth: Int,
        lensName: String,
        focalLength: String,
        fSteps: Set<Double>
    ) {
        val fStepsString = mFArray.filter { fSteps.contains(it) }.joinToString(", ")
        val fastestSsDouble = Util.stringToDoubleShutterSpeed(fastestSs)
        val slowestSsDouble = Util.stringToDoubleShutterSpeed(slowestSs)
        
        val data = Intent()
        val c = CameraSpec(id, type,
            if (created.isNotEmpty()) created else Util.dateToStringUTC(Date()),
            Util.dateToStringUTC(Date()),
            mount,
            manufacturer,
            model,
            format,
            ssGrainSize,
            if (fastestSsDouble != 0.0) fastestSsDouble else null,
            if (slowestSsDouble != 0.0) slowestSsDouble else null,
            bulbAvailable,
            ssCustomSteps.map { Util.stringToDoubleShutterSpeed(it).toString() }.joinToString(","),
            evGrainSize,
            evWidth)
        data.putExtra("cameraspec", c)
        if (type == 1) {
            val l = LensSpec(-1, "", id, manufacturer,
                lensName,
                focalLength,
                fStepsString)
            data.putExtra("fixed_lens", l)
        }
        
        userPreferencesRepository.saveSuggestList("camera_manufacturer", R.array.camera_manufacturer, arrayOf(manufacturer))
        if (type == 0) {
            userPreferencesRepository.saveSuggestList("camera_mounts", R.array.camera_mounts, arrayOf(mount))
        }

        setResult(RESULT_OK, data)
        finish()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("created", created)
        outState.putStringArrayList("ss_custom_steps", java.util.ArrayList(ssCustomSteps))
        outState.putInt("ss_grain_size", ssGrainSize)
        outState.putInt("previous_checked_ss_steps", previousCheckedSsSteps)
        outState.putBoolean("isDirty", isDirty)
    }
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
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current as EditCameraActivity

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

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomSsDialog by rememberSaveable { mutableStateOf(false) }

    val formatList = stringArrayResource(id = R.array.film_formats).toList()
    val evGrainSizeList = stringArrayResource(id = R.array.ev_grain_size_list).toList()
    val evWidthList = stringArrayResource(id = R.array.ev_width_list).toList()

    val ssListOne = stringArrayResource(id = R.array.shutter_speeds_one).toList()
    val ssListHalf = stringArrayResource(id = R.array.shutter_speeds_half).toList()
    val ssListOneThird = stringArrayResource(id = R.array.shutter_speeds_one_third).toList()
    
    val currentSsList = remember(context.ssGrainSize, context.ssCustomSteps) {
        when (context.ssGrainSize) {
            1 -> ssListOne
            2 -> ssListHalf
            3 -> ssListOneThird
            else -> context.ssCustomSteps
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
        if (!context.isDirty) {
            onCancel()
        } else {
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
                TextButton(onClick = {
                    showSaveDialog = false
                    context.saveAndFinish(manufacturer, mount, model, format, fastestSs, slowestSs, bulbAvailable, evGrainSize, evWidth, lensName, focalLength, fSteps)
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
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
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.msg_continue_editing_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onCancel()
                }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showCustomSsDialog) {
        val defaultValue = if (context.ssCustomSteps.isEmpty()) {
            context.resources.getStringArray(R.array.shutter_speeds_one).joinToString("\n")
        } else {
            context.ssCustomSteps.joinToString("\n")
        }
        
        ShutterSpeedCustomizeDialog(
            title = stringResource(R.string.title_dialog_custom_ss),
            message = stringResource(R.string.msg_dialog_custom_ss),
            defaultValue = defaultValue,
            onConfirm = { valueStr ->
                val list = valueStr.split("\n").filter { it.isNotEmpty() }.sortedBy { Util.stringToDoubleShutterSpeed(it) }
                context.ssCustomSteps = list
                context.previousCheckedSsSteps = 0
                context.isDirty = true
                showCustomSsDialog = false
            },
            onDismiss = {
                context.ssGrainSize = context.previousCheckedSsSteps
                showCustomSsDialog = false
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
                        onClick = { context.saveAndFinish(manufacturer, mount, model, format, fastestSs, slowestSs, bulbAvailable, evGrainSize, evWidth, lensName, focalLength, fSteps) },
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
                        onValueChange = { mount = it; context.isDirty = true; expandedMount = true },
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
                                        context.isDirty = true
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
                    onValueChange = { manufacturer = it; context.isDirty = true; expandedManufacturer = true },
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
                                    context.isDirty = true
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
                    context.isDirty = true
                    if (it == "jenkinsushi") {
                        Toast.makeText(context, context.getString(R.string.google_maps_key), Toast.LENGTH_LONG).show()
                    }
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
                                context.isDirty = true
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
                                    if (value == 0) {
                                        if (context.previousCheckedSsSteps != 0) {
                                            showCustomSsDialog = true
                                        }
                                        context.ssGrainSize = value
                                    } else {
                                        context.ssGrainSize = value
                                        context.previousCheckedSsSteps = value
                                        context.isDirty = true
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = context.ssGrainSize == value,
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
                        onValueChange = { slowestSs = it; context.isDirty = true; expandedSlowest = true },
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
                                DropdownMenuItem(text = { Text(suggestion) }, onClick = { slowestSs = suggestion; context.isDirty = true; expandedSlowest = false })
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
                        onValueChange = { fastestSs = it; context.isDirty = true; expandedFastest = true },
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
                                DropdownMenuItem(text = { Text(suggestion) }, onClick = { fastestSs = suggestion; context.isDirty = true; expandedFastest = false })
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
                    context.isDirty = true
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
                            DropdownMenuItem(text = { Text(option) }, onClick = { evGrainSize = index + 1; context.isDirty = true; expandedEvGrain = false })
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
                            DropdownMenuItem(text = { Text(option) }, onClick = { evWidth = index + 1; context.isDirty = true; expandedEvWidth = false })
                        }
                    }
                }
            }

            // Fixed Lens attributes
            if (type == 1) {
                Spacer(modifier = Modifier.height(8.dp))
                ClassicTextField(
                    value = lensName,
                    onValueChange = { lensName = it; context.isDirty = true },
                    label = stringResource(R.string.label_lens_name),
                    modifier = Modifier.fillMaxWidth()
                )

                ClassicTextField(
                    value = focalLength,
                    onValueChange = { focalLength = it; context.isDirty = true },
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
                                context.isDirty = true
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