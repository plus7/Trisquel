package net.tnose.app.trisquel

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.preference.PreferenceManager
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar

class EditPhotoActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    internal val REQCODE_GET_LOCATION = 100
    internal val DIALOG_MOUNT_ADAPTERS = 103
    internal val DIALOG_DATE = 104
    internal val DIALOG_ACCESSORY = 105
    internal val REQCODE_IMAGES = 106
    internal val RETCODE_SDCARD_PERM_LOADIMG = 107
    internal val RETCODE_SDCARD_PERM_IMGPICKER = 108
    internal val DIALOG_ASK_CREATE_LENS = 109
    internal val REQCODE_ADD_LENS = 110

    private val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
        }

    var id: Int = 0
    var frameIndex: Int = 0
    var evGrainSize = 3
    var evWidth = 3
    var focalLengthRange by mutableStateOf(Pair(43.0, 43.0))

    var filmroll by mutableStateOf<FilmRoll?>(null)
    var photo by mutableStateOf<Photo?>(null)
    var lenslist by mutableStateOf<List<LensSpec>>(emptyList())
    var ssList by mutableStateOf<List<String>>(emptyList())
    var apertureList by mutableStateOf<List<String>>(emptyList())

    var dateStr by mutableStateOf("")
    var lensid by mutableIntStateOf(-1)
    var apertureStr by mutableStateOf("")
    var ssStr by mutableStateOf("")
    var focalLengthProgress by mutableIntStateOf(0)
    var expCompProgress by mutableIntStateOf(0)
    var ttlProgress by mutableIntStateOf(0)
    var locationStr by mutableStateOf("")
    var latitude by mutableDoubleStateOf(999.0)
    var longitude by mutableDoubleStateOf(999.0)
    var memoStr by mutableStateOf("")
    
    var selectedAccessories by mutableStateOf<List<Int>>(emptyList())
    var accessoriesStr by mutableStateOf("")
    
    var supplementalImages by mutableStateOf<List<String>>(emptyList())
    var supplementalImagesToLoad by mutableStateOf<List<String>>(emptyList())
    
    var allTags by mutableStateOf<List<String>>(emptyList())
    var checkState by mutableStateOf<List<Boolean>>(emptyList())
    
    var favorite by mutableStateOf(false)
    var isDirty by mutableStateOf(false)

    val expCompensation: Double
        get() {
            val bd = BigDecimal((expCompProgress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            return bd.setScale(1, BigDecimal.ROUND_DOWN).toDouble()
        }

    val ttlLightMeter: Double
        get() {
            val bd = BigDecimal((ttlProgress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            return bd.setScale(1, BigDecimal.ROUND_DOWN).toDouble()
        }

    fun toHumanReadableCompensationAmount(progress: Int): String {
        val bd = BigDecimal((progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
        val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
        return (if (bd2.signum() > 0) "+" else "") + bd2.toPlainString() + "EV"
    }

    val newphoto: Photo
        get() {
            val sb = StringBuilder("/")
            for (accessory in selectedAccessories) {
                sb.append(accessory)
                sb.append('/')
            }
            val accessories = sb.toString()

            val lensSelectedId = lenslist.find { it.id == lensid }?.id ?: -1

            return Photo(
                id,
                filmroll!!.id,
                frameIndex,
                dateStr,
                0,
                lensSelectedId,
                focalLengthProgress.toDouble() + focalLengthRange.first,
                Util.safeStr2Dobule(apertureStr),
                Util.stringToDoubleShutterSpeed(ssStr),
                expCompensation, ttlLightMeter,
                locationStr,
                latitude, longitude,
                memoStr,
                accessories, JSONArray(supplementalImages).toString(),
                favorite)
        }

    val resultData: Intent
        get() {
            val newdata = Intent()
            newdata.putExtra("photo", newphoto)
            val tags = ArrayList<String>()
            for((i,v) in checkState.withIndex()){
                if(v) tags.add(allTags[i])
            }
            newdata.putExtra("tags", tags)
            return newdata
        }

    val photoText: String
        get() {
            val sb = StringBuilder()
            sb.append(getString(R.string.label_date) + ": " + dateStr + "\n")
            val l = lenslist.find { it.id == lensid }
            if (l != null) {
                sb.append(getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n")
                if (apertureStr.isNotEmpty()) sb.append(getString(R.string.label_aperture) + ": " + apertureStr + "\n")
                if (ssStr.isNotEmpty()) sb.append(getString(R.string.label_shutter_speed) + ": " + ssStr + "\n")
                if (expCompensation != 0.0) sb.append(getString(R.string.label_exposure_compensation) + ": " + expCompensation + "\n")
                if (ttlLightMeter != 0.0) sb.append(getString(R.string.label_ttl_light_meter) + ": " + ttlLightMeter + "\n")
                if (locationStr.isNotEmpty()) sb.append(getString(R.string.label_location) + ": " + locationStr + "\n")
                if (latitude != 999.0 && longitude != 999.0) sb.append(getString(R.string.label_coordinate) + ": " + latitude + ", " + longitude + "\n")
                if (memoStr.isNotEmpty()) sb.append(getString(R.string.label_memo) + ": " + memoStr + "\n")
                if (accessoriesStr.isNotEmpty()) sb.append(getString(R.string.label_accessories) + ": " + accessoriesStr + "\n")
            }
            return sb.toString()
        }

    fun updateLensList(lens: LensSpec?, dao: TrisquelDao) {
        val f = filmroll ?: return
        if (f.camera.type == 1) { // Fixed lens
            val fixedLens = dao.getLens(dao.getFixedLensIdByBody(f.camera.id))
            if (fixedLens != null) {
                lenslist = listOf(fixedLens)
                lensid = fixedLens.id
            }
        } else {
            val newLensList = dao.getLensesByMount(f.camera.mount)
            for (s in getSuggestListSubPref("mount_adapters", f.camera.mount)) {
                newLensList.addAll(dao.getLensesByMount(s))
            }
            if (lens != null && f.camera.mount != lens.mount && !newLensList.any { it.id == lens.id }) {
                newLensList.add(0, lens)
            }
            lenslist = newLensList
        }

        if (lens != null && lenslist.any { it.id == lens.id }) {
            lensid = lens.id
        } else if (f.camera.type == 1 && lenslist.isNotEmpty()) {
            lensid = lenslist[0].id
        }
    }

    fun setAccessories(dao: TrisquelDao, accessories: List<Int>) {
        selectedAccessories = accessories
        val sb = StringBuilder()
        var first = true
        for (accId in accessories) {
            if (!first) sb.append(", ")
            val a = dao.getAccessory(accId)
            if (a != null) sb.append(a.name)
            first = false
        }
        accessoriesStr = sb.toString()
    }

    fun loadTags(photoId: Int, dao: TrisquelDao) {
        val tags = dao.getTagsByPhoto(photoId)
        val allTagsDb = dao.allTags.sortedBy { it.label }
        
        val newAllTags = mutableListOf<String>()
        val newCheckState = mutableListOf<Boolean>()
        for (t in allTagsDb) {
            newAllTags.add(t.label)
            newCheckState.add(tags.find { it.id == t.id } != null)
        }
        allTags = newAllTags
        checkState = newCheckState
    }

    fun refreshApertureAdapter(lens: LensSpec?) {
        if (lens != null) {
            apertureList = lens.fSteps.map { d -> d.toString() }
        } else {
            apertureList = emptyList()
        }
        if (apertureList.size == 1) {
            apertureStr = apertureList[0]
        }
    }

    fun refreshFocalLength(lens: LensSpec?) {
        focalLengthRange = when (lens) {
            null -> Pair(0.0, 0.0)
            else -> Util.getFocalLengthRangeFromStr(lens.focalLength)
        }
        focalLengthProgress = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = TrisquelDao(applicationContext)
        dao.connection()

        val data = intent
        id = data.getIntExtra("id", 0)
        frameIndex = data.getIntExtra("frameIndex", -1)
        val filmrollid = data.getIntExtra("filmroll", 0)
        
        val loadedFilmroll = dao.getFilmRoll(filmrollid)
        filmroll = loadedFilmroll
        val loadedPhoto = dao.getPhoto(id)
        photo = loadedPhoto

        if (loadedFilmroll != null) {
            evGrainSize = loadedFilmroll.camera.evGrainSize
            evWidth = loadedFilmroll.camera.evWidth
            
            val ssArray = when (loadedFilmroll.camera.shutterSpeedGrainSize) {
                1 -> resources.getStringArray(R.array.shutter_speeds_one)
                2 -> resources.getStringArray(R.array.shutter_speeds_half)
                3 -> resources.getStringArray(R.array.shutter_speeds_one_third)
                else -> loadedFilmroll.camera.shutterSpeedSteps.map { Util.doubleToStringShutterSpeed(it) }.toTypedArray()
            }.filter { s ->
                val ssval = Util.stringToDoubleShutterSpeed(s)
                ssval <= loadedFilmroll.camera.slowestShutterSpeed!! && ssval >= loadedFilmroll.camera.fastestShutterSpeed!!
            }
            ssList = ssArray
            if (ssList.size == 1) {
                ssStr = ssList[0]
            }
        }

        var loadedLens: LensSpec? = null

        if (id > 0 && savedInstanceState == null) {
            lensid = loadedPhoto!!.lensid
            loadedLens = dao.getLens(lensid)
            if (loadedPhoto.date.isNotEmpty()) dateStr = loadedPhoto.date
            setLatLng(loadedPhoto.latitude, loadedPhoto.longitude)
            setAccessories(dao, loadedPhoto.accessories)
            if (loadedPhoto.aperture > 0) apertureStr = Util.doubleToStringShutterSpeed(loadedPhoto.aperture)
            if (loadedPhoto.shutterSpeed > 0) ssStr = Util.doubleToStringShutterSpeed(loadedPhoto.shutterSpeed)
            locationStr = loadedPhoto.location
            memoStr = loadedPhoto.memo

            expCompProgress = ((evWidth + loadedPhoto.expCompensation) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            ttlProgress = ((evWidth + loadedPhoto.ttlLightMeter) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()

            refreshFocalLength(loadedLens)
            if (loadedLens != null) {
                focalLengthProgress = (loadedPhoto.focalLength - loadedLens.focalLengthRange.first).toInt()
            }
            favorite = loadedPhoto.favorite
            loadTags(id, dao)
            checkPermAndAppendSupplementalImages(loadedPhoto.supplementalImages)

        } else if (savedInstanceState != null) {
            lensid = savedInstanceState.getInt("lensid")
            loadedLens = dao.getLens(lensid)
            dateStr = savedInstanceState.getString("date") ?: ""
            setLatLng(savedInstanceState.getDouble("latitude"), savedInstanceState.getDouble("longitude"))
            setAccessories(dao, savedInstanceState.getIntegerArrayList("selected_accessories")?.toList() ?: emptyList())
            ssStr = savedInstanceState.getString("shutter_speed") ?: ""
            apertureStr = savedInstanceState.getString("aperture") ?: ""
            locationStr = savedInstanceState.getString("location") ?: ""
            memoStr = savedInstanceState.getString("memo") ?: ""

            val exp_compensation = savedInstanceState.getDouble("exp_compensation")
            expCompProgress = ((evWidth + exp_compensation) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()

            val ttl_light_meter = savedInstanceState.getDouble("ttl_light_meter")
            ttlProgress = ((evWidth + ttl_light_meter) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()

            refreshFocalLength(loadedLens)
            if (loadedLens != null) {
                val focalLengthVal = savedInstanceState.getDouble("focal_length")
                focalLengthProgress = (focalLengthVal - loadedLens.focalLengthRange.first).toInt()
            }

            favorite = savedInstanceState.getBoolean("favorite")
            allTags = savedInstanceState.getStringArrayList("alltags")?.toList() ?: emptyList()
            checkState = savedInstanceState.getBooleanArray("checkstate")?.toList() ?: emptyList()
            
            checkPermAndAppendSupplementalImages(savedInstanceState.getStringArrayList("suppimgs"))
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            loadTags(-1, dao)
            expCompProgress = evWidth * evGrainSize
            ttlProgress = evWidth * evGrainSize

            if (loadedFilmroll != null) {
                if (loadedFilmroll.camera.type == 1) {
                    lensid = dao.getFixedLensIdByBody(loadedFilmroll.camera.id)
                } else {
                    val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val autocomplete = pref.getBoolean("autocomplete_from_previous_shot", false)
                    if (autocomplete) {
                        val ps = dao.getPhotosByFilmRollId(filmrollid)
                        var pos = -1
                        for (i in ps.indices) {
                            if (ps[i].id == id) {
                                pos = i
                                break
                            }
                        }
                        if (pos > 0) {
                            lensid = ps[pos - 1].lensid
                        } else if (pos < 0 && ps.isNotEmpty()) {
                            lensid = ps[ps.size - 1].lensid
                        } else {
                            lensid = 0
                        }
                    } else {
                        lensid = 0
                    }
                }
            }
            loadedLens = dao.getLens(lensid)
            refreshFocalLength(loadedLens)

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            dateStr = sdf.format(calendar.time)
            setLatLng(999.0, 999.0)
        }

        updateLensList(loadedLens, dao)
        refreshApertureAdapter(loadedLens)
        dao.close()

        setContent {
            TrisquelTheme {
                EditPhotoScreen(
                    onSave = {
                        setResult(RESULT_OK, resultData)
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED, Intent())
                        finish()
                    }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putString("date", dateStr)
        outState.putDouble("latitude", latitude)
        outState.putDouble("longitude", longitude)
        outState.putInt("lensid", lensid)
        outState.putIntegerArrayList("selected_accessories", ArrayList(selectedAccessories))
        outState.putString("aperture", apertureStr)
        outState.putDouble("focal_length", focalLengthProgress + focalLengthRange.first)
        outState.putString("shutter_speed", ssStr)
        outState.putDouble("exp_compensation", expCompensation)
        outState.putDouble("ttl_light_meter", ttlLightMeter)
        outState.putString("location", locationStr)
        outState.putString("memo", memoStr)
        outState.putStringArrayList("suppimgs", ArrayList(supplementalImages))
        outState.putBoolean("favorite", favorite)
        outState.putStringArrayList("alltags", ArrayList(allTags))
        outState.putBooleanArray("checkstate", checkState.toBooleanArray())
        super.onSaveInstanceState(outState)
    }

    fun showDateDialogOnActivity() {
        DatePickerFragment.Builder()
            .build(DIALOG_DATE)
            .showOn(this, "dialog")
    }

    fun showAccessoryDialogOnActivity() {
        val fragment = CheckListDialogFragment.Builder().build(DIALOG_ACCESSORY)
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val accessories = dao.accessories
        dao.close()

        val a_str = ArrayList<String>()
        val chkidx = ArrayList<Int>()
        val tags = ArrayList<Int>()
        for (i in accessories.indices) {
            val a = accessories[i]
            a_str.add(a.name)
            if (selectedAccessories.contains(a.id)) chkidx.add(i)
            tags.add(a.id)
        }

        fragment.arguments?.putStringArray("items", a_str.toTypedArray())
        fragment.arguments?.putIntegerArrayList("tags", tags)
        fragment.arguments?.putIntegerArrayList("checked_indices", chkidx)
        fragment.arguments?.putString("title", getString(R.string.title_dialog_select_accessories))
        fragment.arguments?.putString("positive", getString(android.R.string.yes))
        fragment.arguments?.putString("negative", getString(android.R.string.no))
        fragment.showOn(this@EditPhotoActivity, "dialog")
    }

    fun showMountAdaptersDialog() {
        val fragment = CheckListDialogFragment.Builder().build(DIALOG_MOUNT_ADAPTERS)
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val availableLensMounts = dao.availableMountList
        dao.close()

        val mount = filmroll?.camera?.mount ?: return
        if (availableLensMounts.indexOf(mount) >= 0) {
            availableLensMounts.remove(mount)
        }
        val checkedMounts = getSuggestListSubPref("mount_adapters", mount)
        val checkedIndices = ArrayList<Int>()
        for (i in checkedMounts.indices) {
            if (availableLensMounts.indexOf(checkedMounts[i]) >= 0) {
                checkedIndices.add(availableLensMounts.indexOf(checkedMounts[i]))
            }
        }

        fragment.arguments?.putStringArray("items", availableLensMounts.toTypedArray())
        fragment.arguments?.putIntegerArrayList("checked_indices", checkedIndices)
        fragment.arguments?.putString("title", getString(R.string.msg_select_mount_adapters).replace("%s", mount))
        fragment.arguments?.putString("positive", getString(android.R.string.yes))
        fragment.arguments?.putString("negative", getString(android.R.string.no))
        fragment.showOn(this@EditPhotoActivity, "dialog")
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            DIALOG_DATE -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val year = data.getIntExtra(DatePickerFragment.EXTRA_YEAR, 1970)
                val month = data.getIntExtra(DatePickerFragment.EXTRA_MONTH, 0)
                val day = data.getIntExtra(DatePickerFragment.EXTRA_DAY, 0)
                val c = Calendar.getInstance()
                c.set(Calendar.YEAR, year)
                c.set(Calendar.MONTH, month)
                c.set(Calendar.DAY_OF_MONTH, day)
                val sdf = SimpleDateFormat("yyyy/MM/dd")
                dateStr = sdf.format(c.time)
                isDirty = true
            }
            DIALOG_MOUNT_ADAPTERS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                saveSuggestListSubPref("mount_adapters", filmroll!!.camera.mount, bundle!!.getStringArrayList("checked_items"))
                val dao = TrisquelDao(applicationContext)
                dao.connection()
                val l = lenslist.find { it.id == lensid }
                val dbLens = if (l != null) dao.getLens(l.id) else null
                updateLensList(dbLens, dao)
                dao.close()
            }
            DIALOG_ACCESSORY -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                val tags = bundle!!.getIntegerArrayList("checked_tags")
                if (tags != null && !sameArrayList(tags, ArrayList(selectedAccessories))) {
                    val dao = TrisquelDao(applicationContext)
                    dao.connection()
                    setAccessories(dao, tags)
                    isDirty = true
                    dao.close()
                }
            }
            DIALOG_ASK_CREATE_LENS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val intent = Intent(application, EditLensActivity::class.java)
                startActivityForResult(intent, REQCODE_ADD_LENS)
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {}

    fun setLatLng(newlatitude: Double, newlongitude: Double) {
        if (newlatitude == 999.0 || newlongitude == 999.0) {
            latitude = 999.0
            longitude = 999.0
        } else {
            latitude = newlatitude
            longitude = newlongitude
        }
    }

    private fun sameArrayList(list1: ArrayList<Int>, list2: ArrayList<Int>): Boolean {
        if (list1.size != list2.size) return false
        for (i in list1.indices) {
            if (list1[i] != list2[i]) return false
        }
        return true
    }

    fun getSuggestListSubPref(parentkey: String, subkey: String): ArrayList<String> {
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
        } catch (e: JSONException) {}
        return strArray
    }

    fun saveSuggestListSubPref(parentkey: String, subkey: String, values: ArrayList<String>?) {
        if (values == null) return
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        var obj: JSONObject? = null
        try {
            obj = JSONObject(prefstr)
        } catch (e: JSONException) {
            obj = JSONObject()
        }

        val result = JSONArray(values)
        val e = pref.edit()
        try {
            obj!!.put(subkey, result)
            e.putString(parentkey, obj.toString())
            e.apply()
        } catch (e1: JSONException) {}
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)
            } else {
                intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)
            }
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted)) {
            when(requestCode){
                RETCODE_SDCARD_PERM_LOADIMG -> {
                    supplementalImages = (supplementalImages + supplementalImagesToLoad).distinct()
                }
                RETCODE_SDCARD_PERM_IMGPICKER -> {
                    openImagePicker()
                }
            }
        } else {
            supplementalImages = (supplementalImages + supplementalImagesToLoad).distinct()
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    fun openImagePicker() {
        Matisse.from(this)
            .choose(MimeType.ofImage())
            .captureStrategy(CaptureStrategy(true, "net.tnose.app.trisquel.provider", "Camera"))
            .capture(true)
            .countable(true)
            .maxSelectable(40)
            .thumbnailScale(0.85f)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .imageEngine(Glide4Engine())
            .forResult(REQCODE_IMAGES)
    }

    fun checkPermAndOpenImagePicker() {
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val cameraDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (readDenied || cameraDenied) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM_IMGPICKER)
            return
        }
        openImagePicker()
    }

    fun checkPermAndAppendSupplementalImages(paths: ArrayList<String>?) {
        if (paths == null || paths.isEmpty()) return
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

        if (readDenied) {
            supplementalImagesToLoad = paths
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM_LOADIMG)
            return
        }
        supplementalImages = (supplementalImages + paths).distinct()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQCODE_GET_LOCATION -> if (resultCode == RESULT_OK && data != null) {
                val bundle = data.extras
                val newlat = bundle!!.getDouble("latitude")
                val newlog = bundle.getDouble("longitude")
                if (newlat != latitude || newlog != longitude) isDirty = true
                setLatLng(newlat, newlog)
                locationStr = bundle.getString("location") ?: ""
            } else if (resultCode == MapsActivity.RESULT_DELETE) {
                if (999.0 != latitude || 999.0 != longitude) isDirty = true
                setLatLng(999.0, 999.0)
            }
            REQCODE_IMAGES -> if (resultCode == RESULT_OK && data != null) {
                val uris = Matisse.obtainResult(data)
                val newPaths = uris.filterNotNull().map { it.toString() }
                supplementalImages = (supplementalImages + newPaths).distinct()
                isDirty = true
            }
            REQCODE_ADD_LENS -> if (resultCode == RESULT_OK && data != null) {
                val bundle = data.extras
                val l = bundle!!.getParcelable<LensSpec>("lensspec")!!
                val dao = TrisquelDao(this)
                dao.connection()
                l.id = dao.addLens(l).toInt()
                updateLensList(l, dao)
                dao.close()
                refreshApertureAdapter(l)
                refreshFocalLength(l)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhotoScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current as EditPhotoActivity
    val canSave = context.lenslist.any { it.id == context.lensid }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (context.id > 0) R.string.title_activity_edit_photo else R.string.title_activity_edit_photo)) }, // Both are same in Trisquel logic conceptually
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val s = context.photoText
                            if (s.isNotEmpty()) {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("", s))
                                Toast.makeText(context, context.getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
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
                    value = context.dateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_date),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.matchParentSize().clickable { context.showDateDialogOnActivity() })
            }

            // Lens
            var expandedLens by remember { mutableStateOf(false) }
            val selectedLens = context.lenslist.find { it.id == context.lensid }
            val selectedLensText = selectedLens?.let { "${it.manufacturer} ${it.modelName}" } ?: ""
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expandedLens,
                    onExpandedChange = { 
                        if (context.lenslist.isEmpty()) {
                            val fragment = YesNoDialogFragment.Builder().build(context.DIALOG_ASK_CREATE_LENS)
                            fragment.arguments?.putString("message", context.getString(R.string.msg_ask_create_lens))
                            fragment.arguments?.putString("positive", context.getString(android.R.string.yes))
                            fragment.arguments?.putString("negative", context.getString(android.R.string.no))
                            fragment.showOn(context, "dialog")
                        } else {
                            expandedLens = it 
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    val errorMsg = context.filmroll?.camera?.mount?.let { context.getString(R.string.error_nolens).replace("%s", it) }
                    ClassicTextField(
                        value = selectedLensText,
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.label_lens),
                        isError = context.lenslist.isEmpty() && errorMsg != null,
                        supportingText = if (context.lenslist.isEmpty() && errorMsg != null) { { Text(errorMsg) } } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLens) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (context.lenslist.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expandedLens, onDismissRequest = { expandedLens = false }) {
                            context.lenslist.forEach { lensOption ->
                                DropdownMenuItem(
                                    text = { Text("${lensOption.manufacturer} ${lensOption.modelName}") },
                                    onClick = {
                                        context.lensid = lensOption.id
                                        context.isDirty = true
                                        expandedLens = false
                                        context.refreshApertureAdapter(lensOption)
                                        context.refreshFocalLength(lensOption)
                                    }
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = { context.showMountAdaptersDialog() },
                    enabled = context.filmroll?.camera?.type != 1
                ) {
                    Icon(
                        painter = painterResource(id = if (context.filmroll?.camera?.type == 1) R.drawable.ic_mount_adapter_disabled else R.drawable.ic_mount_adapter_plane),
                        contentDescription = "Mount Adapters"
                    )
                }
            }

            // Aperture
            var expandedAperture by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedAperture,
                onExpandedChange = { if (context.apertureList.isNotEmpty()) expandedAperture = it }
            ) {
                ClassicTextField(
                    value = context.apertureStr,
                    onValueChange = { context.apertureStr = it; context.isDirty = true; expandedAperture = true },
                    label = stringResource(R.string.label_aperture),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAperture) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                val filteredAperture = context.apertureList.filter { it.contains(context.apertureStr, ignoreCase = true) }
                if (filteredAperture.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = expandedAperture, onDismissRequest = { expandedAperture = false }) {
                        filteredAperture.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { context.apertureStr = suggestion; context.isDirty = true; expandedAperture = false })
                        }
                    }
                }
            }

            // Shutter Speed
            var expandedSs by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedSs,
                onExpandedChange = { expandedSs = it }
            ) {
                ClassicTextField(
                    value = context.ssStr,
                    onValueChange = { context.ssStr = it; context.isDirty = true; expandedSs = true },
                    label = stringResource(R.string.label_shutter_speed),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSs) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                val filteredSs = context.ssList.filter { it.contains(context.ssStr, ignoreCase = true) }
                if (filteredSs.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = expandedSs, onDismissRequest = { expandedSs = false }) {
                        filteredSs.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { context.ssStr = suggestion; context.isDirty = true; expandedSs = false })
                        }
                    }
                }
            }

            // Focal Length
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.label_focal_length), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val currentFl = (context.focalLengthProgress + context.focalLengthRange.first).toInt()
                    Text(text = "${currentFl}mm" + if (context.focalLengthRange.first == context.focalLengthRange.second) " (prime)" else "")
                    if (context.focalLengthRange.first != context.focalLengthRange.second) {
                        Slider(
                            value = context.focalLengthProgress.toFloat(),
                            onValueChange = { context.focalLengthProgress = it.toInt(); context.isDirty = true },
                            valueRange = 0f..(context.focalLengthRange.second - context.focalLengthRange.first).toFloat()
                        )
                    }
                }
            }

            // Exposure Compensation
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.label_exposure_compensation), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = context.toHumanReadableCompensationAmount(context.expCompProgress))
                    Slider(
                        value = context.expCompProgress.toFloat(),
                        onValueChange = { context.expCompProgress = it.toInt(); context.isDirty = true },
                        valueRange = 0f..(context.evWidth * 2 * context.evGrainSize).toFloat(),
                        steps = context.evWidth * 2 * context.evGrainSize - 1
                    )
                }
            }

            // TTL Light Meter
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.label_ttl_light_meter), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = context.toHumanReadableCompensationAmount(context.ttlProgress))
                    Slider(
                        value = context.ttlProgress.toFloat(),
                        onValueChange = { context.ttlProgress = it.toInt(); context.isDirty = true },
                        valueRange = 0f..(context.evWidth * 2 * context.evGrainSize).toFloat(),
                        steps = context.evWidth * 2 * context.evGrainSize - 1
                    )
                }
            }

            // Accessories
            Box(modifier = Modifier.fillMaxWidth()) {
                ClassicTextField(
                    value = context.accessoriesStr,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_accessories),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.matchParentSize().clickable { context.showAccessoryDialogOnActivity() })
            }

            // Location
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ClassicTextField(
                    value = context.locationStr,
                    onValueChange = { context.locationStr = it; context.isDirty = true },
                    label = stringResource(R.string.label_location),
                    supportingText = if (context.latitude != 999.0 && context.longitude != 999.0) {
                        { Text("${stringResource(R.string.label_coordinate)}: ${context.latitude}, ${context.longitude}") }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val intent = Intent(context, MapsActivity::class.java)
                        if (context.latitude != 999.0 && context.longitude != 999.0) {
                            intent.putExtra("latitude", context.latitude)
                            intent.putExtra("longitude", context.longitude)
                        }
                        context.startActivityForResult(intent, context.REQCODE_GET_LOCATION)
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (context.latitude == 999.0 || context.longitude == 999.0) R.drawable.ic_place_gray_24dp else R.drawable.ic_place_black_24dp),
                        contentDescription = "Get Location",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            // Memo
            ClassicTextField(
                value = context.memoStr,
                onValueChange = { context.memoStr = it; context.isDirty = true },
                label = stringResource(R.string.label_memo),
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
            )

            // Supplemental Images
            Text(text = stringResource(R.string.label_supplementary_images), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                items(context.supplementalImages) { path ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable {
                                val intent = Intent()
                                val photoURI = if(path.startsWith("/")) {
                                    val file = java.io.File(path)
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
                                    .load(if (path.startsWith("/")) java.io.File(path) else Uri.parse(path))
                                    .into(view)
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                        IconButton(
                            onClick = { 
                                context.supplementalImages = context.supplementalImages - path
                                context.isDirty = true 
                            },
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
                            .clickable { context.checkPermAndOpenImagePicker() }
                    )
                }
            }

            // Tags
            Text(text = stringResource(R.string.label_tags), color = MaterialTheme.colorScheme.onSurfaceVariant)
            var tagInput by remember { mutableStateOf("") }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ClassicTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = "",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (tagInput.isNotEmpty() && !context.allTags.contains(tagInput)) {
                            context.allTags = context.allTags + tagInput
                            context.checkState = context.checkState + true
                            tagInput = ""
                            context.isDirty = true
                        }
                    },
                    enabled = tagInput.isNotEmpty() && !context.allTags.contains(tagInput)
                ) {
                    Text(stringResource(R.string.label_add))
                }
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                context.allTags.forEachIndexed { index, tag ->
                    FilterChip(
                        selected = context.checkState[index],
                        onClick = {
                            val newList = context.checkState.toMutableList()
                            newList[index] = !newList[index]
                            context.checkState = newList
                            context.isDirty = true
                        },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
