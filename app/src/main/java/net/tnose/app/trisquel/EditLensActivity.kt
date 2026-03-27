package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import org.json.JSONArray
import org.json.JSONException
import java.util.Date
import java.util.regex.Pattern

private val mFArray = listOf(0.95, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.2, 2.4, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 4.8, 5.0, 5.6, 6.3, 6.7, 7.1, 8.0, 9.0, 9.5, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0, 22.0)

class EditLensActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = -1
    private var created: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent
        id = data.getIntExtra("id", -1)

        var initManufacturer = ""
        var initMount = ""
        var initModel = ""
        var initFocalLength = ""
        var initFSteps = emptySet<Double>()

        if (id >= 0 && savedInstanceState == null) {
            val l = dao.getLens(id)
            if (l != null) {
                created = Util.dateToStringUTC(l.created)
                initManufacturer = l.manufacturer
                initMount = l.mount
                initModel = l.modelName
                initFocalLength = l.focalLength
                initFSteps = l.fSteps.toSet()
            }
        } else if (savedInstanceState != null) {
            created = savedInstanceState.getString("created") ?: ""
            initManufacturer = savedInstanceState.getString("manufacturer") ?: ""
            initMount = savedInstanceState.getString("mount") ?: ""
            initModel = savedInstanceState.getString("model_name") ?: ""
            initFocalLength = savedInstanceState.getString("focal_length") ?: ""
            initFSteps = parseFSteps(savedInstanceState.getString("FStepsString") ?: "")
        } else {
            created = ""
        }
        dao.close()

        val titleRes = if (id < 0) R.string.title_activity_reg_lens else R.string.title_activity_edit_lens

        setContent {
            TrisquelTheme {
                EditLensScreen(
                    title = stringResource(id = titleRes),
                    initManufacturer = initManufacturer,
                    initMount = initMount,
                    initModel = initModel,
                    initFocalLength = initFocalLength,
                    initFSteps = initFSteps,
                    suggestedManufacturers = getSuggestListPref("lens_manufacturer", R.array.lens_manufacturer),
                    suggestedMounts = getSuggestListPref("camera_mounts", R.array.camera_mounts),
                    onSave = { manufacturer, mount, model, focalLength, fSteps ->
                        saveAndFinish(manufacturer, mount, model, focalLength, fSteps)
                    },
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

    private fun getSuggestListPref(prefkey: String, defRscId: Int): List<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(prefkey, "[]")
        val strArray = ArrayList<String>()
        val defRsc = resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }
        strArray.addAll(defRsc)
        return strArray.distinct()
    }

    private fun saveSuggestListPref(prefkey: String, defRscId: Int, newValues: Array<String>) {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(prefkey, "[]")
        val strArray = ArrayList<String>()
        val defRsc = resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        for (item in newValues) {
            if (item.isEmpty()) continue
            if (strArray.indexOf(item) >= 0) {
                strArray.removeAt(strArray.indexOf(item))
                strArray.add(0, item)
            }
        }
        strArray.addAll(defRsc)

        val result = JSONArray(strArray.distinct())
        val e = pref.edit()
        e.putString(prefkey, result.toString())
        e.apply()
    }

    private fun saveAndFinish(manufacturer: String, mount: String, model: String, focalLength: String, fSteps: Set<Double>) {
        val fStepsString = mFArray.filter { fSteps.contains(it) }.joinToString(", ")

        val data = Intent()
        val l = LensSpec(id,
            if (created.isNotEmpty()) created else Util.dateToStringUTC(Date()),
            Util.dateToStringUTC(Date()),
            mount, 0,
            manufacturer,
            model,
            focalLength,
            fStepsString)
        data.putExtra("lensspec", l)
        
        saveSuggestListPref("lens_manufacturer", R.array.lens_manufacturer, arrayOf(manufacturer))
        saveSuggestListPref("camera_mounts", R.array.camera_mounts, arrayOf(mount))

        setResult(RESULT_OK, data)
        finish()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {}
    override fun onDialogCancelled(requestCode: Int) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLensScreen(
    title: String,
    initManufacturer: String,
    initMount: String,
    initModel: String,
    initFocalLength: String,
    initFSteps: Set<Double>,
    suggestedManufacturers: List<String>,
    suggestedMounts: List<String>,
    onSave: (String, String, String, String, Set<Double>) -> Unit,
    onCancel: () -> Unit
) {
    var manufacturer by remember { mutableStateOf(initManufacturer) }
    var mount by remember { mutableStateOf(initMount) }
    var model by remember { mutableStateOf(initModel) }
    var focalLength by remember { mutableStateOf(initFocalLength) }
    var fSteps by remember { mutableStateOf(initFSteps) }
    var isDirty by remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val focalLengthOk = remember(focalLength) {
        if (focalLength.isNotEmpty()) {
            val zoom = Pattern.compile("(\\d++)-(\\d++)")
            if (zoom.matcher(focalLength).find()) return@remember true

            val prime = Pattern.compile("(\\d++)")
            if (prime.matcher(focalLength).find()) return@remember true
        }
        false
    }

    val canSave = mount.isNotEmpty() && model.isNotEmpty() && focalLengthOk

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
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.msg_save_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    onSave(manufacturer, mount, model, focalLength, fSteps)
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
                        onClick = { onSave(manufacturer, mount, model, focalLength, fSteps) },
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
            // Mount
            var expandedMount by remember { mutableStateOf(false) }
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

            // Manufacturer
            var expandedManufacturer by remember { mutableStateOf(false) }
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
                onValueChange = { model = it; isDirty = true },
                label = stringResource(R.string.label_model),
                modifier = Modifier.fillMaxWidth()
            )

            // Focal Length
            ClassicTextField(
                value = focalLength,
                onValueChange = { focalLength = it; isDirty = true },
                label = stringResource(R.string.label_focal_length),
                supportingText = { Text(stringResource(R.string.hint_zoom)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused && focalLength.isEmpty()) {
                            val zoom = Pattern.compile(".*?(\\d++)-(\\d++)mm.*")
                            val mZoom = zoom.matcher(model)
                            if (mZoom.find()) {
                                focalLength = "${mZoom.group(1)}-${mZoom.group(2)}"
                                isDirty = true
                            } else {
                                val prime = Pattern.compile(".*?(\\d++)mm.*")
                                val mPrime = prime.matcher(model)
                                if (mPrime.find()) {
                                    focalLength = mPrime.group(1) ?: ""
                                    isDirty = true
                                }
                            }
                        }
                    }
            )

            // F-Stops Grid
            Text(
                text = stringResource(R.string.label_f_stops),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                mFArray.forEach { fValue ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(84.dp)
                            .clickable {
                                val newSet = fSteps.toMutableSet()
                                if (fSteps.contains(fValue)) newSet.remove(fValue) else newSet.add(fValue)
                                fSteps = newSet
                                isDirty = true
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = fSteps.contains(fValue),
                            onCheckedChange = null // Managed by the Row's clickable
                        )
                        Text(
                            text = fValue.toString(),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
