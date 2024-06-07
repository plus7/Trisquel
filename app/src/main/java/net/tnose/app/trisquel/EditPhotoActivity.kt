package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.chip.Chip
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import net.tnose.app.trisquel.databinding.ActivityEditPhotoBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar

class EditPhotoActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    internal val REQCODE_GET_LOCATION = 100
    internal val DIALOG_SAVE_OR_DISCARD = 101
    internal val DIALOG_CONTINUE_OR_DISCARD = 102
    internal val DIALOG_MOUNT_ADAPTERS = 103
    internal val DIALOG_DATE = 104
    internal val DIALOG_ACCESSORY = 105
    internal val REQCODE_IMAGES = 106
    internal val RETCODE_SDCARD_PERM_LOADIMG = 107
    internal val RETCODE_SDCARD_PERM_IMGPICKER = 108
    internal val DIALOG_ASK_CREATE_LENS = 109
    internal val REQCODE_ADD_LENS = 110
    private var evGrainSize = 3
    private var evWidth = 3
    private var focalLengthRange = Pair(43.0, 43.0) // FA limited 43mm こそ真の標準レンズ！！！
    private val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
        }

    private var filmroll: FilmRoll? = null
    private var photo: Photo? = null
    private var lenslist: ArrayList<LensSpec> = ArrayList()
    internal var lensadapter: LensAdapter? = null
    internal var apertureAdapter: ArrayAdapter<String>? = null

    private var ssAdapter: ArrayAdapter<String>? = null
    private var lensid: Int = -1
    private var lens: LensSpec? = null
    private var id: Int = 0
    private var frameIndex: Int = 0
    private var latitude = 999.0
    private var longitude = 999.0
    private var selectedAccessories: ArrayList<Int> = ArrayList()
    private var supplementalImages: ArrayList<String> = ArrayList()
    private var supplementalImagesToLoad: ArrayList<String> = ArrayList()
    private var allTags: ArrayList<String> = arrayListOf()
    private var checkState: ArrayList<Boolean> = arrayListOf()
    private var favorite = false
    private var isResumed: Boolean = false
    private var isDirty: Boolean = false
    private lateinit var binding: ActivityEditPhotoBinding

    val newphoto: Photo
        get() {
            val sb = StringBuilder("/")
            for (accessory in selectedAccessories) {
                sb.append(accessory)
                sb.append('/')
            }
            val accessories = sb.toString()

            return Photo(
                    id,
                    filmroll!!.id,
                    frameIndex,
                    binding.editDate.text.toString(),
                    0,
                    lenslist[binding.spinnerLens.position].id,
                    binding.seekFocalLength.progress + focalLengthRange.first,
                    Util.safeStr2Dobule(binding.spinnerAperture.text.toString()),
                    Util.stringToDoubleShutterSpeed(binding.spinnerSs.text.toString()),
                    expCompensation, ttlLightMeter,
                    binding.editLocation.text.toString(),
                    latitude, longitude,
                    binding.editMemo.text.toString(),
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

    private val expCompensation: Double
        get() {
            val bd = BigDecimal((binding.seekExpCompensation.progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
            return java.lang.Double.parseDouble(bd2.toPlainString())
        }

    private val ttlLightMeter: Double
        get() {
            val bd3 = BigDecimal((binding.seekTtlLightMeter.progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            val bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN)
            return java.lang.Double.parseDouble(bd4.toPlainString())
        }

    private val photoText: String
        get() {
            val sb = StringBuilder()
            sb.append(getString(R.string.label_date) + ": " + binding.editDate.text.toString() + "\n")
            if (binding.spinnerLens.position >= 0) {
                val l = lenslist[binding.spinnerLens.position]
                sb.append(getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n")
                if (binding.spinnerAperture.text.isNotEmpty())
                    sb.append(getString(R.string.label_aperture) + ": " + binding.spinnerAperture.text + "\n")
                if (binding.spinnerSs.text.isNotEmpty())
                    sb.append(getString(R.string.label_shutter_speed) + ": " + binding.spinnerSs.text.toString() + "\n")
                if (expCompensation != 0.0)
                    sb.append(getString(R.string.label_exposure_compensation) + ": " + expCompensation + "\n")
                if (ttlLightMeter != 0.0)
                    sb.append(getString(R.string.label_ttl_light_meter) + ": " + ttlLightMeter + "\n")
                if (binding.editLocation.text?.isNotEmpty() == true)
                    sb.append(getString(R.string.label_location) + ": " + binding.editLocation.text.toString() + "\n")
                if (latitude != 999.0 && longitude != 999.0)
                    sb.append(getString(R.string.label_coordinate) + ": " + java.lang.Double.toString(latitude) + ", " + java.lang.Double.toString(longitude) + "\n")
                if (binding.editMemo.text?.isNotEmpty() == true)
                    sb.append(getString(R.string.label_memo) + ": " + binding.editMemo.text.toString() + "\n")
                if (binding.editAccessories.text?.isNotEmpty() == true)
                    sb.append(getString(R.string.label_accessories) + ": " + binding.editAccessories.text.toString() + "\n")
            }
            return sb.toString()
        }

    protected fun updateLensList(lens: LensSpec?, dao: TrisquelDao) {
        if (filmroll!!.camera.type == 1) { //問答無用。selectedId等は完全無視
            lenslist = ArrayList()
            lenslist.add(dao.getLens(dao.getFixedLensIdByBody(filmroll!!.camera.id))!!)
            binding.spinnerLens.isEnabled = false
            binding.btnMountAdapters.isEnabled = false
            binding.btnMountAdapters.setImageResource(R.drawable.ic_mount_adapter_disabled)
        } else {
            lenslist = dao.getLensesByMount(filmroll!!.camera.mount)
            for (s in getSuggestListSubPref("mount_adapters", filmroll!!.camera.mount)) {
                lenslist.addAll(dao.getLensesByMount(s))
            }
            if (lens != null && this.filmroll!!.camera.mount != lens.mount) {
                lenslist.add(0, lens)
            }
        }
        lensadapter = LensAdapter(this, android.R.layout.simple_spinner_item, lenslist)
        lensadapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLens.setAdapter(lensadapter)
        if (lensadapter!!.count == 0) {
            binding.spinnerLens.error = getString(R.string.error_nolens).replace("%s", filmroll!!.camera.mount)
        }else{
            binding.spinnerLens.error = null
        }

        if (lens != null) {
            binding.spinnerLens.position = lensadapter!!.getPosition(lens.id)
        } else if (filmroll!!.camera.type == 1) {
            binding.spinnerLens.position = 0
        }
    }

    private fun setAccessories(dao: TrisquelDao, accessories: ArrayList<Int>?) {
        selectedAccessories.clear()
        selectedAccessories.addAll(accessories!!)
        val sb = StringBuilder()
        var first = true
        for (id in accessories) {
            if (!first) sb.append(", ")
            val a = dao.getAccessory(id)
            sb.append(a!!.name)
            first = false
        }
        binding.editAccessories.setText(sb.toString())
    }

    protected fun loadTags(id: Int, dao: TrisquelDao){
        val tags = dao.getTagsByPhoto(id)
        val alltags = dao.allTags.sortedBy { it.label }
        for(t in alltags){
            val chip = createNewChip(t.label)
            allTags.add(t.label)
            if (tags.find { it.id == t.id } != null){
                checkState.add(true)
                chip.isChecked = true
            }else{
                checkState.add(false)
            }
        }
    }

    protected fun loadData(data: Intent, dao: TrisquelDao, savedInstanceState: Bundle?) {
        this.id = data.getIntExtra("id", 0)
        this.frameIndex = data.getIntExtra("frameIndex", -1)
        val filmrollid = data.getIntExtra("filmroll", 0)
        this.filmroll = dao.getFilmRoll(filmrollid)
        this.photo = dao.getPhoto(id)

        this.evGrainSize = filmroll!!.camera.evGrainSize
        this.evWidth = filmroll!!.camera.evWidth
        binding.seekExpCompensation.max = evWidth * 2 * evGrainSize
        binding.seekTtlLightMeter.max = evWidth * 2 * evGrainSize

        val ssArray = when (filmroll!!.camera.shutterSpeedGrainSize) {
            1 -> resources.getStringArray(R.array.shutter_speeds_one)
            2 -> resources.getStringArray(R.array.shutter_speeds_half)
            3 -> resources.getStringArray(R.array.shutter_speeds_one_third)
            else -> filmroll!!.camera.shutterSpeedSteps.map { Util.doubleToStringShutterSpeed(it) }.toTypedArray() //なんか無駄な感じするけど…
        }.filter{ s ->
            val ssval = Util.stringToDoubleShutterSpeed(s)
            ssval <= filmroll!!.camera.slowestShutterSpeed!! && ssval >= filmroll!!.camera.fastestShutterSpeed!!
        }

        ssAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ssArray)
        ssAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSs.setAdapter<ArrayAdapter<String>>(ssAdapter)
        // 写ルンですを考慮
        if (ssAdapter!!.count == 1) {
            binding.spinnerSs.position = 0
        }

        if (photo != null) {
        } else {
        }

        if(id > 0 && savedInstanceState == null) { //既存データを開きたて
            lensid = photo!!.lensid
            lens = dao.getLens(lensid)
            if(photo!!.date.isNotEmpty())
                binding.editDate.setText(photo!!.date)
            setLatLng(photo!!.latitude, photo!!.longitude)
            setAccessories(dao, photo!!.accessories)
            if (photo!!.aperture > 0) binding.spinnerAperture.setText(Util.doubleToStringShutterSpeed(photo!!.aperture))
            if (photo!!.shutterSpeed > 0) binding.spinnerSs.setText(Util.doubleToStringShutterSpeed(photo!!.shutterSpeed))
            binding.editLocation.setText(photo!!.location)
            binding.editMemo.setText(photo!!.memo)

            binding.seekExpCompensation.progress = ((evWidth + photo!!.expCompensation) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            binding.labelEvCorrAmount.text = toHumanReadableCompensationAmount((photo!!.expCompensation * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())
            binding.seekTtlLightMeter.progress = ((evWidth + photo!!.ttlLightMeter) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            binding.labelTtlLightMeter.text = toHumanReadableCompensationAmount((photo!!.ttlLightMeter * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())

            refreshFocalLength(lens)
            if(lens != null){
                binding.seekFocalLength.progress = (photo!!.focalLength - lens!!.focalLengthRange.first).toInt()
                binding.labelFocalLength.text = photo!!.focalLength.toString() + "mm"
            }

            favorite = photo!!.favorite

            loadTags(id, dao)

            checkPermAndAppendSupplementalImages(photo!!.supplementalImages)

        }else if(savedInstanceState != null){ //復帰データあり
            lensid = savedInstanceState.getInt("lensid")
            lens = dao.getLens(lensid)
            binding.editDate.setText(savedInstanceState.getString("date"))
            setLatLng(savedInstanceState.getDouble("latitude"), savedInstanceState.getDouble("longitude"))
            setAccessories(dao, savedInstanceState.getIntegerArrayList("selected_accessories"))
            binding.spinnerSs.setText(savedInstanceState.getString("shutter_speed"))
            binding.spinnerAperture.setText(savedInstanceState.getString("aperture"))
            binding.editLocation.setText(savedInstanceState.getString("location"))
            binding.editMemo.setText(savedInstanceState.getString("memo"))

            val exp_compensation = savedInstanceState.getDouble("exp_compensation")
            binding.seekExpCompensation.progress = ((evWidth + exp_compensation) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            binding.labelEvCorrAmount.text = toHumanReadableCompensationAmount((exp_compensation * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())

            val ttl_light_meter = savedInstanceState.getDouble("ttl_light_meter")
            binding.seekTtlLightMeter.progress = ((evWidth + ttl_light_meter) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            binding.labelTtlLightMeter.text = toHumanReadableCompensationAmount((ttl_light_meter * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())

            refreshFocalLength(lens)
            if(lens != null) {
                val focalLength = savedInstanceState.getDouble("focal_length")
                binding.seekFocalLength.progress = (focalLength - lens!!.focalLengthRange.first).toInt()
                binding.labelFocalLength.text = focalLength.toString() + "mm"
            }

            favorite = savedInstanceState.getBoolean("favorite")

            allTags = savedInstanceState.getStringArrayList("alltags") ?: arrayListOf()
            checkState = ArrayList<Boolean>((savedInstanceState.getBooleanArray("checkstate") ?: BooleanArray(0)).asList())
            for((i, v) in allTags.withIndex()){
                val chip = createNewChip(v)
                chip.isChecked = checkState[i]
            }

            checkPermAndAppendSupplementalImages(savedInstanceState.getStringArrayList("suppimgs"))
        }else{ //未入力開きたて
            loadTags(-1, dao)
            binding.seekExpCompensation.progress = evWidth * evGrainSize
            binding.labelEvCorrAmount.text = toHumanReadableCompensationAmount(0)
            binding.seekTtlLightMeter.progress = evWidth * evGrainSize
            binding.labelTtlLightMeter.text = toHumanReadableCompensationAmount(0)
            if(filmroll!!.camera.type == 1){
                lensid = dao.getFixedLensIdByBody(filmroll!!.camera.id)
            }else {
                val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val autocomplete = pref.getBoolean("autocomplete_from_previous_shot", false)
                if (autocomplete) {
                    val ps = dao.getPhotosByFilmRollId(filmrollid)
                    var pos = -1
                    for(i in ps.indices){
                        if(ps[i].id == id){
                            pos = i
                            break
                        }
                    }
                    if (pos > 0) {
                        lensid = ps[pos - 1].lensid
                    } else if (pos < 0 && ps.size > 0) {
                        lensid = ps[ps.size - 1].lensid
                    } else {
                        lensid = 0
                    }
                } else {
                    lensid = 0
                }
            }

            lens = dao.getLens(lensid)

            refreshFocalLength(lens)
            binding.seekFocalLength.progress = 0

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            binding.editDate.setText(sdf.format(calendar.time))

            setLatLng(999.0, 999.0)
        }

        updateLensList(lens, dao)
        refreshApertureAdapter(lens)
    }

    protected fun primeLens(p: Pair<Double, Double>): Boolean = (p.first == p.second)

    protected fun toHumanReadableCompensationAmount(value: Int):String{
        val bd = BigDecimal((value).toDouble() / evGrainSize.toDouble())
        val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
        return (if (bd2.signum() > 0) "+" else "") + bd2.toPlainString() + "EV"
    }

    protected fun setEventListeners() {

        binding.spinnerLens.setOnClickListener {
            if(binding.spinnerLens.adapter.count == 0){
                val fragment = YesNoDialogFragment.Builder()
                        .build(DIALOG_ASK_CREATE_LENS)
                fragment.arguments?.putString("message", getString(R.string.msg_ask_create_lens))
                fragment.arguments?.putString("positive", getString(android.R.string.yes))
                fragment.arguments?.putString("negative", getString(android.R.string.no))
                fragment.showOn(this, "dialog")
            }
        }

        val oldListener = binding.spinnerLens.onItemClickListener // これやらないとgetPositionがおかしくなる
        binding.spinnerLens.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            if(lens?.id != lenslist[i].id){
                lens = lenslist[i]
                lensid = lens!!.id
                refreshApertureAdapter(lenslist[i])
                refreshFocalLength(lenslist[i])
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
            oldListener.onItemClick(adapterView, view, i, l)
        }

        binding.editDate.setOnClickListener { showDateDialogOnActivity() }

        binding.editDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        binding.editAccessories.setOnClickListener { showAccessoryDialogOnActivity() }

        binding.editAccessories.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        binding.spinnerAperture.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })
        binding.spinnerSs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })

        binding.seekExpCompensation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                binding.labelEvCorrAmount.text = toHumanReadableCompensationAmount(i - evGrainSize * evWidth)
                if(isResumed) isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        binding.seekTtlLightMeter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                binding.labelTtlLightMeter.text = toHumanReadableCompensationAmount(i - evGrainSize * evWidth)
                if(isResumed) isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        binding.seekFocalLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                binding.labelFocalLength.text = java.lang.Double.toString(binding.seekFocalLength.progress + focalLengthRange.first) + "mm"
                if(isResumed) isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        binding.editLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })
        binding.editMemo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })
        binding.btnGetLocation.setOnClickListener {
            val intent = Intent(application, MapsActivity::class.java)
            if (latitude != 999.0 && longitude != 999.0) {
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
            }
            startActivityForResult(intent, REQCODE_GET_LOCATION)
        }
        binding.btnMountAdapters.setOnClickListener {
            val fragment = CheckListDialogFragment.Builder()
                    .build(DIALOG_MOUNT_ADAPTERS)
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val availableLensMounts = dao.availableMountList
            dao.close()

            val mount = filmroll!!.camera.mount
            if (availableLensMounts.indexOf(mount) >= 0) {
                availableLensMounts.remove(mount)
            }
            val availableLensMountsArray = availableLensMounts.toTypedArray()

            val checkedMounts = getSuggestListSubPref("mount_adapters", mount)
            val checkedIndices = ArrayList<Int>()
            for (i in checkedMounts.indices) {
                if (availableLensMounts.indexOf(checkedMounts[i]) >= 0) {
                    checkedIndices.add(availableLensMounts.indexOf(checkedMounts[i]))
                }
            }

            fragment.arguments?.putStringArray("items", availableLensMountsArray)
            fragment.arguments?.putIntegerArrayList("checked_indices", checkedIndices)
            fragment.arguments?.putString("title", getString(R.string.msg_select_mount_adapters).replace("%s", mount))
            fragment.arguments?.putString("positive", getString(android.R.string.yes))
            fragment.arguments?.putString("negative", getString(android.R.string.no))
            fragment.showOn(this@EditPhotoActivity, "dialog")
        }

        binding.buttonAdd.isEnabled = false
        binding.buttonAdd.setOnClickListener {
            if(isResumed) isDirty = true
            val label = binding.editTagtext.text.toString()
            allTags.add(label)
            checkState.add(true)
            val chip = createNewChip(label)
            chip.isChecked = true
            binding.editTagtext.setText("")
        }

        binding.editTagtext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.buttonAdd.isEnabled = !allTags.contains(binding.editTagtext.text.toString()) && binding.editTagtext.text.isNotEmpty()
            }
        })
    }

    protected fun canSave(): Boolean {
        return binding.spinnerLens.position >= 0
    }

    protected fun refreshApertureAdapter(lens: LensSpec?) {
        apertureAdapter?.clear()
        if (lens != null) {
            apertureAdapter?.addAll(lens.fSteps.map { d -> d.toString() })
            binding.spinnerAperture.isEnabled = true
        }
        binding.spinnerAperture.setAdapter(apertureAdapter)

        //写ルンですを考慮
        if (apertureAdapter!!.count == 1) {
            binding.spinnerAperture.position = 0
        }
    }

    protected fun refreshFocalLength(lens: LensSpec?) {
        focalLengthRange = when(lens){
            null -> Pair(0.0, 0.0)
            else -> Util.getFocalLengthRangeFromStr(lens.focalLength)
        }
        binding.labelFocalLength.text = focalLengthRange.first.toString() + "mm"
        if (primeLens(focalLengthRange)) {
            binding.seekFocalLength.visibility = View.GONE
        } else {
            binding.seekFocalLength.visibility = View.VISIBLE
        }
        binding.seekFocalLength.max = (focalLengthRange.second - focalLengthRange.first).toInt() // API Level 26以降ならsetMinが使えるのだが…
        binding.seekFocalLength.progress = 0
    }

    protected fun getSuggestListSubPref(parentkey: String, subkey: String): ArrayList<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        val strArray = ArrayList<String>()
        try {
            val obj = JSONObject(prefstr)
            val array = obj.getJSONArray(subkey) ?: return strArray
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        return strArray
    }

    protected fun saveSuggestListSubPref(parentkey: String, subkey: String, values: ArrayList<String>?) {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        var obj: JSONObject? = null
        try {
            val array: JSONArray
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
        } catch (e1: JSONException) {
        }

    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            RETCODE_SDCARD_PERM_LOADIMG, RETCODE_SDCARD_PERM_IMGPICKER -> {
                onRequestSDCardAccessPermissionsResult(permissions, grantResults, requestCode)
            }
        }
    }

    internal fun onRequestSDCardAccessPermissionsResult(permissions: Array<String>, grantResults: IntArray, requestCode: Int) {
         val granted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            }

        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted)) {
            when(requestCode){
                RETCODE_SDCARD_PERM_LOADIMG -> {
                    appendSupplementalImages(supplementalImagesToLoad)
                }
                RETCODE_SDCARD_PERM_IMGPICKER -> {
                    openImagePicker()
                }
            }
        } else {
            supplementalImages = supplementalImagesToLoad //内部的には保持する
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    fun openImagePicker(){
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
        val writeDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) false
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val cameraDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (writeDenied || readDenied || cameraDenied) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM_IMGPICKER)
            return
        }
        openImagePicker()
    }

    fun checkPermAndAppendSupplementalImages(paths: ArrayList<String>?) {
        if(paths == null) return
        if(paths.size == 0) return
        val writeDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) false
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

        if (writeDenied || readDenied) {
            supplementalImagesToLoad = paths
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM_LOADIMG)
            return
        }
        appendSupplementalImages(paths)
    }

    fun createNewChip(label: String): Chip {
        val newchip = Chip(this)
        newchip.text = label
        val chipLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        newchip.layoutParams = chipLayoutParams
        newchip.isCheckable = true
        newchip.setOnCheckedChangeListener{
            buttonView, isChecked ->
            val i = allTags.indexOf(label)
            if(i >= 0) {
                checkState[i] = isChecked
                if (isResumed) isDirty = true
            }
        }
        binding.chipGroup.addView(newchip)
        return newchip
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        binding.imageButton.setOnClickListener {
            checkPermAndOpenImagePicker()
        }

        val typedValue = TypedValue()
        val theme = applicationContext.theme
        theme.resolveAttribute(R.attr.editTextColor, typedValue, true)
        val color = typedValue.data
        binding.labelImages.setTextColor(color and 0x00ffffff or 0x44000000)

        binding.editLocation.helperTextColor = R.color.common_google_signin_btn_text_light_default

        apertureAdapter = ArrayAdapter(this, /* これでいいのか？ */
                android.R.layout.simple_dropdown_item_1line)
        binding.spinnerAperture.isEnabled = false

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent
        loadData(data, dao, savedInstanceState)
        dao.close()

        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putString("date", binding.editDate.text.toString())
        outState.putDouble("latitude", latitude)
        outState.putDouble("longitude", longitude)
        outState.putInt("lensid", lensid)
        outState.putIntegerArrayList("selected_accessories", selectedAccessories)
        outState.putString("aperture", binding.spinnerAperture.text.toString())
        outState.putDouble("focal_length", binding.seekFocalLength.progress + focalLengthRange.first)
        outState.putString("shutter_speed", binding.spinnerSs.text.toString())
        outState.putDouble("exp_compensation", expCompensation)
        outState.putDouble("ttl_light_meter", ttlLightMeter)
        outState.putString("location", binding.editLocation.text.toString())
        outState.putString("memo", binding.editMemo.text.toString())
        outState.putStringArrayList("suppimgs", supplementalImages)
        outState.putBoolean("favorite", favorite)
        outState.putStringArrayList("alltags", allTags)
        outState.putBooleanArray("checkstate", checkState.toBooleanArray())
        super.onSaveInstanceState(outState)
    }

    internal fun showDateDialogOnActivity() {
        DatePickerFragment.Builder()
                .build(DIALOG_DATE)
                .showOn(this, "dialog")
    }

    internal fun showAccessoryDialogOnActivity() {
        val fragment = CheckListDialogFragment.Builder()
                .build(DIALOG_ACCESSORY)
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
            if (selectedAccessories.contains(a.id)) {
                chkidx.add(i)
            }
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

    private fun sameArrayList(list1: ArrayList<Int>, list2: ArrayList<Int>): Boolean {
        if (list1.size != list2.size) return false
        for (i in list1.indices) {
            if (list1[i] != list2[i]) return false
        }
        return true
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
                binding.editDate.setText(sdf.format(c.time))
            }
            DIALOG_SAVE_OR_DISCARD -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                setResult(Activity.RESULT_OK, resultData)
                finish()
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            DIALOG_CONTINUE_OR_DISCARD -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                /* do nothing and continue editing */
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            DIALOG_MOUNT_ADAPTERS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                saveSuggestListSubPref("mount_adapters",
                        filmroll!!.camera.mount,
                        bundle!!.getStringArrayList("checked_items"))
                val dao = TrisquelDao(applicationContext)
                dao.connection()
                val lens = when(binding.spinnerLens.position){
                    -1 -> null
                    else -> dao.getLens(lenslist[binding.spinnerLens.position].id)
                }
                updateLensList(lens, dao)
                dao.close()
                binding.spinnerLens.clearFocus() //こうしないとドロップダウンリストが更新されない…気がする。バグか？
            }
            DIALOG_ACCESSORY -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                val tags = bundle!!.getIntegerArrayList("checked_tags")
                if (!sameArrayList(tags!!, selectedAccessories)) {
                    val dao = TrisquelDao(applicationContext)
                    dao.connection()
                    setAccessories(dao, tags)
                    if(isResumed) isDirty = true
                    dao.close()
                }
            }
            DIALOG_ASK_CREATE_LENS -> if(resultCode == DialogInterface.BUTTON_POSITIVE){
                intent = Intent(application, EditLensActivity::class.java)
                startActivityForResult(intent, REQCODE_ADD_LENS)
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_photo, menu)
        menu.findItem(R.id.menu_save).isEnabled = canSave()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (!isDirty) {
                    setResult(Activity.RESULT_CANCELED, Intent())
                    finish()
                } else {
                    if (canSave()) {
                        val fragment = YesNoDialogFragment.Builder()
                                .build(101)
                        fragment.arguments?.putString("message", getString(R.string.msg_save_or_discard_data))
                        fragment.arguments?.putString("positive", getString(R.string.save))
                        fragment.arguments?.putString("negative", getString(R.string.discard))
                        fragment.showOn(this, "dialog")
                    } else {
                        val fragment = YesNoDialogFragment.Builder()
                                .build(102)
                        fragment.arguments?.putString("message", getString(R.string.msg_continue_editing_or_discard_data))
                        fragment.arguments?.putString("positive", getString(R.string.continue_editing))
                        fragment.arguments?.putString("negative", getString(R.string.discard))
                        fragment.showOn(this, "dialog")
                    }
                }
                return true
            }
            R.id.menu_save -> {
                setResult(Activity.RESULT_OK, resultData)
                finish()
                return true
            }
            R.id.menu_copy -> {
                val s = photoText
                if (s.isEmpty()) {
                    return true
                }
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("", s))
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!isDirty) {
            setResult(Activity.RESULT_CANCELED, Intent())
            super.onBackPressed()
        } else {
            if (canSave()) {
                val fragment = YesNoDialogFragment.Builder()
                        .build(101)
                fragment.arguments?.putString("message", getString(R.string.msg_save_or_discard_data))
                fragment.arguments?.putString("positive", getString(R.string.save))
                fragment.arguments?.putString("negative", getString(R.string.discard))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(102)
                fragment.arguments?.putString("message", getString(R.string.msg_continue_editing_or_discard_data))
                fragment.arguments?.putString("positive", getString(R.string.continue_editing))
                fragment.arguments?.putString("negative", getString(R.string.discard))
                fragment.showOn(this, "dialog")
            }
        }
    }

    private fun setLatLng(newlatitude: Double, newlongitude: Double) {
        val sb = StringBuilder()
        if (newlatitude == 999.0 || newlongitude == 999.0) {
            latitude = 999.0
            longitude = 999.0
            binding.editLocation.setHelperText(null)
            binding.editLocation.isHelperTextAlwaysShown = false
            binding.btnGetLocation.setImageResource(R.drawable.ic_place_gray_24dp)
        } else {
            latitude = newlatitude
            longitude = newlongitude
            binding.editLocation.setHelperText(
                    getString(R.string.label_coordinate) +
                            ": " + java.lang.Double.toString(newlatitude) + ", " + java.lang.Double.toString(newlongitude))
            binding.editLocation.isHelperTextAlwaysShown = true
            binding.btnGetLocation.setImageResource(R.drawable.ic_place_black_24dp)
        }
    }

    private fun appendSupplementalImages(paths: ArrayList<String>){
        for(s in paths) appendSupplementalImage(s)
    }

    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
            SimpleDateFormat("dd.MM.yyyy").let { formatter ->
                formatter.parse("$day.$month.$year")?.time ?: 0
            }

    private fun appendSupplementalImage(path: String){
        if(path in this.supplementalImages) return
        val options = BitmapFactory.Options()

        options.inJustDecodeBounds = true
        var bmp = if(path.startsWith("/")) {
            BitmapFactory.decodeFile(path, options)
        } else {
            val input = CompatibilityUtil.pathToInputStream(contentResolver, path, false)
            if (input != null) {
                val bitmap = BitmapFactory.decodeStream(input, null, options)
                input.close()
                bitmap
            } else {
                null
            }
        }

        var (w, h) =
                if(options.outWidth > 0 && options.outHeight > 0)
                    Pair(options.outWidth, options.outHeight)
                else
                    Pair(150, 150)
        try {
            // 枠を回転させるかどうか考える
            val exifInterface = if(path.startsWith("/")){
                ExifInterface(path)
            } else {
                val ist = contentResolver.openInputStream(Uri.parse(path))
                ExifInterface(ist!!)
            }
            val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_TRANSVERSE,
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    val tmp = h
                    h = w
                    w = tmp
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }

        val imageView = CustomImageView(this)
        val scale = resources.displayMetrics.density
        val lp = LinearLayout.LayoutParams((150 * scale * w / h).toInt(), (150 * scale).toInt())
        lp.setMargins(0,0, 16, 0)
        binding.imageContainer.addView(imageView, binding.imageContainer.childCount - 1, lp)
        imageView.path = path
        imageView.setOnCloseClickListener(object: View.OnClickListener{
            override fun onClick(view: View?): Unit {
                if(view is CustomImageView) {
                    this@EditPhotoActivity.supplementalImages.remove(view.path)
                    val vg = view.parent as ViewGroup
                    vg.removeView(view)
                    if(isResumed) isDirty = true
                }
            }
        })
        imageView.setOnClickListener(object: View.OnClickListener{
            override fun onClick(view: View?): Unit {
                if(view is CustomImageView){
                    Log.v("debug", "clicked" + view.path)
                    val intent = Intent()
                    val photoURI = if(view.path.startsWith("/")) {
                        val file = File(view.path)
                        // android.os.FileUriExposedException回避
                        FileProvider.getUriForFile(this@EditPhotoActivity, this@EditPhotoActivity.applicationContext.packageName + ".provider", file)
                    }else{
                        Uri.parse(view.path)
                    }
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(photoURI, "image/*")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(intent)
                }
            }
        })
        supplementalImages.add(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQCODE_GET_LOCATION -> if (resultCode == Activity.RESULT_OK && data != null) {
                val bundle = data.extras
                val newlat: Double
                val newlog: Double
                newlat = bundle!!.getDouble("latitude")
                newlog = bundle.getDouble("longitude")
                if (newlat != latitude || newlog != longitude) if(isResumed) isDirty = true
                setLatLng(newlat, newlog)
                binding.editLocation.setText(bundle.getString("location"))
            } else if (resultCode == MapsActivity.RESULT_DELETE) {
                if (999.0 != latitude || 999.0 != longitude) if(isResumed) isDirty = true
                setLatLng(999.0, 999.0)
            }
            REQCODE_IMAGES -> if (resultCode == RESULT_OK) {
                val uris = Matisse.obtainResult(data)
                for(uri in uris){
                    if(uri != null){
                        appendSupplementalImage(uri.toString())
                    }else{
                        Toast.makeText(this, "Failed to obtain an image. The result was null.", Toast.LENGTH_LONG).show()
                    }
                }
                if(isResumed) isDirty = true
            }
            REQCODE_ADD_LENS -> if (resultCode == Activity.RESULT_OK) {
                if(data != null) {
                    val bundle = data.extras
                    val l = bundle!!.getParcelable<LensSpec>("lensspec")!!
                    val dao = TrisquelDao(this)
                    dao.connection()
                    l.id = dao.addLens(l).toInt()
                    updateLensList(l, dao)
                    dao.close()
                    binding.spinnerLens.clearFocus()
                    refreshApertureAdapter(l)
                    refreshFocalLength(l)
                    invalidateOptionsMenu()
                }
            }
        }
    }
}
