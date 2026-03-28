package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.preference.PreferenceManager
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

class EditFilmRollActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = 0
    private var created: String? = null
    
    // UI state
    var cameralist by mutableStateOf<List<CameraSpec>>(emptyList())
    var isDirty by mutableStateOf(false)

    val addCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val bundle = data.extras
                val c = BundleCompat.getParcelable(bundle!!, "cameraspec", CameraSpec::class.java)!!
                val dao = TrisquelDao(this)
                dao.connection()
                c.id = dao.addCamera(c).toInt()
                updateCameraList(dao)
                dao.close()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        updateCameraList(dao)
        val data = intent
        id = data.getIntExtra("id", 0)

        var initName = ""
        var initCameraId = -1
        var initManufacturer = ""
        var initBrand = ""
        var initIso = ""

        if (id > 0 && savedInstanceState == null) {
            val f = dao.getFilmRoll(id)
            if (f != null) {
                created = Util.dateToStringUTC(f.created)
                initName = f.name
                initCameraId = f.camera.id
                initManufacturer = f.manufacturer
                initBrand = f.brand
                if (f.iso > 0) initIso = f.iso.toString()
            }
        } else if (savedInstanceState != null) {
            created = savedInstanceState.getString("created")
            initName = savedInstanceState.getString("name") ?: ""
            val cameraPosition = savedInstanceState.getInt("camera_position", -1)
            if (cameraPosition >= 0 && cameraPosition < cameralist.size) {
                initCameraId = cameralist[cameraPosition].id
            }
            initManufacturer = savedInstanceState.getString("manufacturer") ?: ""
            initBrand = savedInstanceState.getString("brand") ?: ""
            initIso = savedInstanceState.getString("iso") ?: ""
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            val defaultCameraId = data.getIntExtra("default_camera", -1)
            initCameraId = defaultCameraId
            initManufacturer = data.getStringExtra("default_manufacturer") ?: ""
            initBrand = data.getStringExtra("default_brand") ?: ""
        }
        dao.close()

        val titleRes = if (id <= 0) R.string.title_activity_reg_filmroll else R.string.title_activity_edit_filmroll

        setContent {
            TrisquelTheme {
                EditFilmRollScreen(
                    title = stringResource(id = titleRes),
                    initName = initName,
                    initCameraId = initCameraId,
                    initManufacturer = initManufacturer,
                    initBrand = initBrand,
                    initIso = initIso,
                    cameras = cameralist,
                    suggestedManufacturers = getSuggestListPref("film_manufacturer", R.array.film_manufacturer),
                    onBrandSuggestionsRequested = { manufacturer ->
                        getSuggestListSubPref("film_brand", manufacturer)
                    },
                    onSave = { name, cameraId, manufacturer, brand, isoStr ->
                        saveAndFinish(name, cameraId, manufacturer, brand, isoStr)
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED, Intent())
                        finish()
                    }
                )
            }
        }
    }

    private fun updateCameraList(dao: TrisquelDao) {
        cameralist = dao.allCameras
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

    private fun saveSuggestListPref(prefkey: String, defRscId: Int, newValue: String) {
        if (newValue.isEmpty()) return
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

        if (strArray.indexOf(newValue) >= 0) {
            strArray.removeAt(strArray.indexOf(newValue))
        }
        strArray.add(0, newValue)
        strArray.addAll(defRsc)
        
        val result = JSONArray(strArray.distinct())
        val e = pref.edit()
        e.putString(prefkey, result.toString())
        e.apply()
    }

    private fun getSuggestListSubPref(parentkey: String, subkey: String): List<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        val strArray = ArrayList<String>()
        try {
            val obj = JSONObject(prefstr)
            if (!obj.isNull(subkey)) {
                val array = obj.getJSONArray(subkey)
                for (i in 0 until array.length()) {
                    strArray.add(array.getString(i))
                }
            }
        } catch (e: JSONException) {
        }
        return strArray
    }

    private fun saveSuggestListSubPref(parentkey: String, subkey: String, newValue: String) {
        if (newValue.isEmpty() || subkey.isEmpty()) return
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        val strArray = ArrayList<String>()
        var obj: JSONObject? = null
        try {
            obj = JSONObject(prefstr)
            if (!obj.isNull(subkey)) {
                val array = obj.getJSONArray(subkey)
                for (i in 0 until array.length()) {
                    strArray.add(array.getString(i))
                }
            }
        } catch (e: JSONException) {
            obj = JSONObject()
        }

        if (strArray.indexOf(newValue) >= 0) {
            strArray.removeAt(strArray.indexOf(newValue))
        }
        strArray.add(0, newValue)
        
        val result = JSONArray(strArray)
        val e = pref.edit()
        try {
            obj!!.put(subkey, result)
            e.putString(parentkey, obj.toString())
            e.apply()
        } catch (e1: JSONException) {
        }
    }

    private fun saveAndFinish(name: String, cameraId: Int, manufacturer: String, brand: String, isoStr: String) {
        val data = Intent()
        data.putExtra("id", id)
        data.putExtra("name", name)
        data.putExtra("created", created)
        data.putExtra("camera", cameraId)
        data.putExtra("manufacturer", manufacturer)
        data.putExtra("brand", brand)
        val iso = isoStr.toIntOrNull() ?: 0
        data.putExtra("iso", iso)

        saveSuggestListPref("film_manufacturer", R.array.film_manufacturer, manufacturer)
        saveSuggestListSubPref("film_brand", manufacturer, brand)

        setResult(RESULT_OK, data)
        finish()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {}

    override fun onDialogCancelled(requestCode: Int) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFilmRollScreen(
    title: String,
    initName: String,
    initCameraId: Int,
    initManufacturer: String,
    initBrand: String,
    initIso: String,
    cameras: List<CameraSpec>,
    suggestedManufacturers: List<String>,
    onBrandSuggestionsRequested: (String) -> List<String>,
    onSave: (String, Int, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current as EditFilmRollActivity

    var name by remember { mutableStateOf(initName) }
    var cameraId by remember { mutableIntStateOf(initCameraId) }
    var manufacturer by remember { mutableStateOf(initManufacturer) }
    var brand by remember { mutableStateOf(initBrand) }
    var iso by remember { mutableStateOf(initIso) }

    // If there is exactly one camera, select it by default.
    // Ensure this only happens once, or when list changes and cameraId is still invalid
    LaunchedEffect(cameras.size) {
        if (cameras.size == 1 && cameraId <= 0) {
            cameraId = cameras[0].id
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showAskCreateCameraDialog by remember { mutableStateOf(false) }

    val canSave = name.isNotEmpty() && cameraId > 0

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
                    onSave(name, cameraId, manufacturer, brand, iso)
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

    if (showAskCreateCameraDialog) {
        AlertDialog(
            onDismissRequest = { showAskCreateCameraDialog = false },
            title = null,
            text = { Text(stringResource(R.string.msg_ask_create_camera)) },
            confirmButton = {
                TextButton(onClick = {
                    showAskCreateCameraDialog = false
                    val nextIntent = Intent(context, EditCameraActivity::class.java)
                    context.addCameraLauncher.launch(nextIntent)
                }) {
                    Text(stringResource(android.R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskCreateCameraDialog = false }) {
                    Text(stringResource(android.R.string.no))
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
                        onClick = { onSave(name, cameraId, manufacturer, brand, iso) },
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
            // Name
            ClassicTextField(
                value = name,
                onValueChange = { name = it; context.isDirty = true },
                label = stringResource(R.string.label_name),
                modifier = Modifier.fillMaxWidth()
            )

            // Camera
            var expandedCamera by remember { mutableStateOf(false) }
            val selectedCameraName = cameras.find { it.id == cameraId }?.let { "${it.manufacturer} ${it.modelName}" } ?: ""
            
            ExposedDropdownMenuBox(
                expanded = expandedCamera,
                onExpandedChange = { 
                    if (cameras.isEmpty()) {
                        showAskCreateCameraDialog = true
                    } else {
                        expandedCamera = it
                    }
                }
            ) {
                ClassicTextField(
                    value = selectedCameraName,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_camera),
                    isError = cameras.isEmpty(),
                    supportingText = if (cameras.isEmpty()) { { Text(stringResource(R.string.error_nocamera)) } } else null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCamera) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (cameras.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedCamera,
                        onDismissRequest = { expandedCamera = false }
                    ) {
                        cameras.forEach { camera ->
                            DropdownMenuItem(
                                text = { Text("${camera.manufacturer} ${camera.modelName}") },
                                onClick = {
                                    cameraId = camera.id
                                    context.isDirty = true
                                    expandedCamera = false
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

            // Brand
            var expandedBrand by remember { mutableStateOf(false) }
            val suggestedBrands = remember(manufacturer) { onBrandSuggestionsRequested(manufacturer) }
            ExposedDropdownMenuBox(
                expanded = expandedBrand,
                onExpandedChange = { expandedBrand = it }
            ) {
                ClassicTextField(
                    value = brand,
                    onValueChange = {
                        brand = it
                        context.isDirty = true
                        expandedBrand = true
                    },
                    label = stringResource(R.string.label_brand),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                // Extract ISO from brand when losing focus
                                val zoom = Pattern.compile(".*?(\\d++).*")
                                val m = zoom.matcher(brand)
                                if (m.find() && iso.isEmpty()) {
                                    iso = m.group(1) ?: ""
                                }
                            }
                        }
                )
                val filteredBrands = suggestedBrands.filter { it.contains(brand, ignoreCase = true) }
                if (filteredBrands.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedBrand,
                        onDismissRequest = { expandedBrand = false }
                    ) {
                        filteredBrands.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    brand = suggestion
                                    context.isDirty = true
                                    expandedBrand = false
                                    
                                    val zoom = Pattern.compile(".*?(\\d++).*")
                                    val m = zoom.matcher(suggestion)
                                    if (m.find() && iso.isEmpty()) {
                                        iso = m.group(1) ?: ""
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ISO
            ClassicTextField(
                value = iso,
                onValueChange = { iso = it; context.isDirty = true },
                label = stringResource(R.string.label_iso),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
