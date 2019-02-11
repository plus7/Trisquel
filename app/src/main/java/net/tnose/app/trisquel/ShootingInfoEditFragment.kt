package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.media.ExifInterface
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import kotlinx.android.synthetic.main.fragment_shooting_info.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class ShootingInfoEditFragment : Fragment(), AbstractDialogFragment.Callback {
    private var listener: OnFragmentInteractionListener? = null
    private var mFilmRollId: Int = -1
    private var mId: Int = -1
    private var mFrameIndex: Int = -1

    internal val REQCODE_GET_LOCATION = 100
    internal val REQCODE_MOUNT_ADAPTERS = 103
    internal val REQCODE_DATE = 104
    internal val REQCODE_ACCESSORY = 105
    internal val REQCODE_IMAGES = 106
    internal val RETCODE_SDCARD_PERM_LOADIMG = 107
    internal val RETCODE_SDCARD_PERM_IMGPICKER = 108
    internal val REQCODE_ASK_CREATE_LENS = 109
    internal val REQCODE_ADD_LENS = 110
    private var evGrainSize = 3
    private var evWidth = 3
    private var focalLengthRange = Pair(43.0, 43.0) // FA limited 43mm こそ真の標準レンズ！！！
    private val PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    private var filmroll: FilmRoll? = null
    private var photo: Photo? = null
    val oldphoto:Photo?
        get(){
            return photo
        }
    private var lenslist: ArrayList<LensSpec> = ArrayList()
    internal var lensadapter: LensAdapter? = null
    internal var apertureAdapter: ArrayAdapter<String>? = null

    private var ssAdapter: ArrayAdapter<String>? = null
    private var lensid: Int = -1
    private var lens: LensSpec? = null
    private var latitude = 999.0
    private var longitude = 999.0
    private var selectedAccessories: ArrayList<Int> = ArrayList()
    private var supplementalImages: ArrayList<String> = ArrayList()
    private var supplementalImagesToLoad: ArrayList<String> = ArrayList()
    private var favorite = false
    //private var isResumed: Boolean = false
    var isDirty: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mFilmRollId = it.getInt("filmroll")
            mId          = it.getInt("id")
            mFrameIndex = it.getInt("frameIndex")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)

        imageButton.setOnClickListener {
            checkPermAndOpenImagePicker()
        }

        val typedValue = TypedValue()
        val theme = activity?.applicationContext?.theme
        theme?.resolveAttribute(R.attr.editTextColor, typedValue, true)
        val color = typedValue.data
        label_images.setTextColor(color and 0x00ffffff or 0x44000000)

        edit_location.helperTextColor = R.color.common_google_signin_btn_text_light_default

        apertureAdapter = ArrayAdapter(context!!, /* これでいいのか？ */
                android.R.layout.simple_dropdown_item_1line)
        spinner_aperture.isEnabled = false

        val dao = TrisquelDao(activity!!.applicationContext)
        dao.connection()
        loadData(dao, savedInstanceState)
        dao.close()

        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_shooting_info, container, false)
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
            listener?.onFragmentAttached(this)
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    val newphoto: Photo
        get() {
            val sb = StringBuilder("/")
            for (accessory in selectedAccessories) {
                sb.append(accessory)
                sb.append('/')
            }
            val accessories = sb.toString()

            return Photo(
                    mId,
                    filmroll!!.id,
                    mFrameIndex,
                    edit_date.text.toString(),
                    0,
                    lenslist[spinner_lens.position].id,
                    seek_focal_length.progress + focalLengthRange.first,
                    Util.safeStr2Dobule(spinner_aperture.text.toString()),
                    Util.stringToDoubleShutterSpeed(spinner_ss.text.toString()),
                    expCompensation, ttlLightMeter,
                    edit_location.text.toString(),
                    latitude, longitude,
                    edit_memo.text.toString(),
                    accessories, JSONArray(supplementalImages).toString(),
                    favorite)
        }

    /*val resultData: Intent
        get(){
            val intent = Intent()
            intent.putExtra("photo", newphoto)
            return intent
        }*/

    private val expCompensation: Double
        get() {
            val bd = BigDecimal((seek_exp_compensation.progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
            return java.lang.Double.parseDouble(bd2.toPlainString())
        }

    private val ttlLightMeter: Double
        get() {
            val bd3 = BigDecimal((seek_ttl_light_meter.progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            val bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN)
            return java.lang.Double.parseDouble(bd4.toPlainString())
        }

    val photoText: String
        get() {
            val sb = StringBuilder()
            sb.append(getString(R.string.label_date) + ": " + edit_date.text.toString() + "\n")
            if (spinner_lens.position >= 0) {
                val l = lenslist[spinner_lens.position]
                sb.append(getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n")
                if (spinner_aperture.text.isNotEmpty())
                    sb.append(getString(R.string.label_aperture) + ": " + spinner_aperture.text + "\n")
                if (spinner_ss.text.isNotEmpty())
                    sb.append(getString(R.string.label_shutter_speed) + ": " + spinner_ss.text.toString() + "\n")
                if (expCompensation != 0.0)
                    sb.append(getString(R.string.label_exposure_compensation) + ": " + expCompensation + "\n")
                if (ttlLightMeter != 0.0)
                    sb.append(getString(R.string.label_ttl_light_meter) + ": " + ttlLightMeter + "\n")
                if (edit_location.text?.isNotEmpty() == true)
                    sb.append(getString(R.string.label_location) + ": " + edit_location.text.toString() + "\n")
                if (latitude != 999.0 && longitude != 999.0)
                    sb.append(getString(R.string.label_coordinate) + ": " + java.lang.Double.toString(latitude) + ", " + java.lang.Double.toString(longitude) + "\n")
                if (edit_memo.text?.isNotEmpty() == true)
                    sb.append(getString(R.string.label_memo) + ": " + edit_memo.text.toString() + "\n")
                if (edit_accessories.text?.isNotEmpty() == true)
                    sb.append(getString(R.string.label_accessories) + ": " + edit_accessories.text.toString() + "\n")
            }
            return sb.toString()
        }

    protected fun updateLensList(lens: LensSpec?, dao: TrisquelDao) {
        if (filmroll!!.camera.type == 1) { //問答無用。selectedId等は完全無視
            lenslist = ArrayList()
            lenslist.add(dao.getLens(dao.getFixedLensIdByBody(filmroll!!.camera.id))!!)
            spinner_lens.isEnabled = false
            btn_mount_adapters.isEnabled = false
            btn_mount_adapters.setImageResource(R.drawable.ic_mount_adapter_disabled)
        } else {
            lenslist = dao.getLensesByMount(filmroll!!.camera.mount)
            for (s in getSuggestListSubPref("mount_adapters", filmroll!!.camera.mount)) {
                lenslist.addAll(dao.getLensesByMount(s))
            }
            if (lens != null && this.filmroll!!.camera.mount != lens.mount) {
                lenslist.add(0, lens)
            }
        }
        lensadapter = LensAdapter(activity!!, android.R.layout.simple_spinner_item, lenslist)
        lensadapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_lens.setAdapter(lensadapter)
        if (lensadapter!!.count == 0) {
            spinner_lens.error = getString(R.string.error_nolens).replace("%s", filmroll!!.camera.mount)
        }else{
            spinner_lens.error = null
        }

        if (lens != null) {
            spinner_lens.position = lensadapter!!.getPosition(lens.id)
        } else if (filmroll!!.camera.type == 1) {
            spinner_lens.position = 0
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
        edit_accessories.setText(sb.toString())
    }

    protected fun loadData(dao: TrisquelDao, savedInstanceState: Bundle?) {
        this.filmroll = dao.getFilmRoll(mFilmRollId)
        this.photo = dao.getPhoto(mId)

        this.evGrainSize = filmroll!!.camera.evGrainSize
        this.evWidth = filmroll!!.camera.evWidth
        seek_exp_compensation.max = evWidth * 2 * evGrainSize
        seek_ttl_light_meter.max = evWidth * 2 * evGrainSize

        val ssArray = when (filmroll!!.camera.shutterSpeedGrainSize) {
            1 -> resources.getStringArray(R.array.shutter_speeds_one)
            2 -> resources.getStringArray(R.array.shutter_speeds_half)
            else -> resources.getStringArray(R.array.shutter_speeds_one_third)
        }.filter{ s ->
            val ssval = Util.stringToDoubleShutterSpeed(s)
            ssval <= filmroll!!.camera.slowestShutterSpeed!! && ssval >= filmroll!!.camera.fastestShutterSpeed!!
        }

        ssAdapter = ArrayAdapter(activity!!, android.R.layout.simple_dropdown_item_1line, ssArray)
        ssAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_ss.setAdapter<ArrayAdapter<String>>(ssAdapter)
        // 写ルンですを考慮
        if (ssAdapter!!.count == 1) {
            spinner_ss.position = 0
        }

        if(mId >= 0 && savedInstanceState == null) { //既存データを開きたて
            lensid = photo!!.lensid
            lens = dao.getLens(lensid)
            if(photo!!.date.isNotEmpty())
                edit_date.setText(photo!!.date)
            setLatLng(photo!!.latitude, photo!!.longitude)
            setAccessories(dao, photo!!.accessories)
            if (photo!!.aperture > 0) spinner_aperture.setText(Util.doubleToStringShutterSpeed(photo!!.aperture))
            if (photo!!.shutterSpeed > 0) spinner_ss.setText(Util.doubleToStringShutterSpeed(photo!!.shutterSpeed))
            edit_location.setText(photo!!.location)
            edit_memo.setText(photo!!.memo)

            seek_exp_compensation.progress = ((evWidth + photo!!.expCompensation) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            label_ev_corr_amount.text = toHumanReadableCompensationAmount((photo!!.expCompensation * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())
            seek_ttl_light_meter.progress = ((evWidth + photo!!.ttlLightMeter) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            label_ttl_light_meter.text = toHumanReadableCompensationAmount((photo!!.ttlLightMeter * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())

            refreshFocalLength(lens)
            if(lens != null){
                seek_focal_length.progress = (photo!!.focalLength - lens!!.focalLengthRange.first).toInt()
                label_focal_length.text = photo!!.focalLength.toString() + "mm"
            }

            favorite = photo!!.favorite
            checkPermAndAppendSupplementalImages(photo!!.supplementalImages)

        }else if(savedInstanceState != null){ //復帰データあり
            lensid = savedInstanceState.getInt("lensid")
            lens = dao.getLens(lensid)
            edit_date.setText(savedInstanceState.getString("date"))
            setLatLng(savedInstanceState.getDouble("latitude"), savedInstanceState.getDouble("longitude"))
            setAccessories(dao, savedInstanceState.getIntegerArrayList("selected_accessories"))
            spinner_ss.setText(savedInstanceState.getString("shutter_speed"))
            spinner_aperture.setText(savedInstanceState.getString("aperture"))
            edit_location.setText(savedInstanceState.getString("location"))
            edit_memo.setText(savedInstanceState.getString("memo"))

            val exp_compensation = savedInstanceState.getDouble("exp_compensation")
            seek_exp_compensation.progress = ((evWidth + exp_compensation) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            label_ev_corr_amount.text = toHumanReadableCompensationAmount((exp_compensation * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())

            val ttl_light_meter = savedInstanceState.getDouble("ttl_light_meter")
            seek_ttl_light_meter.progress = ((evWidth + ttl_light_meter) * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
            label_ttl_light_meter.text = toHumanReadableCompensationAmount((ttl_light_meter * evGrainSize).toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt())

            refreshFocalLength(lens)
            if(lens != null) {
                val focalLength = savedInstanceState.getDouble("focal_length")
                seek_focal_length.progress = (focalLength - lens!!.focalLengthRange.first).toInt()
                label_focal_length.text = focalLength.toString() + "mm"
            }

            favorite = savedInstanceState.getBoolean("favorite")

            checkPermAndAppendSupplementalImages(savedInstanceState.getStringArrayList("suppimgs"))
        }else{ //未入力開きたて
            seek_exp_compensation.progress = evWidth * evGrainSize
            label_ev_corr_amount.text = toHumanReadableCompensationAmount(0)
            seek_ttl_light_meter.progress = evWidth * evGrainSize
            label_ttl_light_meter.text = toHumanReadableCompensationAmount(0)
            if(filmroll!!.camera.type == 1){
                lensid = dao.getFixedLensIdByBody(filmroll!!.camera.id)
            }else {
                val pref = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
                val autocomplete = pref.getBoolean("autocomplete_from_previous_shot", false)
                if (autocomplete) {
                    val ps = dao.getPhotosByFilmRollId(mFilmRollId)
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
                        lensid = -1
                    }
                } else {
                    lensid = -1
                }
            }

            lens = dao.getLens(lensid)

            refreshFocalLength(lens)
            seek_focal_length.progress = 0

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            edit_date.setText(sdf.format(calendar.time))

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

        spinner_lens.setOnClickListener {
            if(spinner_lens.adapter.count == 0){
                val fragment = YesNoDialogFragment.Builder()
                        .build(REQCODE_ASK_CREATE_LENS)
                fragment.arguments?.putString("message", getString(R.string.msg_ask_create_lens))
                fragment.arguments?.putString("positive", getString(android.R.string.yes))
                fragment.arguments?.putString("negative", getString(android.R.string.no))
                fragment.showOn(this, "dialog")
            }
        }

        val oldListener = spinner_lens.onItemClickListener // これやらないとgetPositionがおかしくなる
        spinner_lens.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            if(lens?.id != lenslist[i].id){
                lens = lenslist[i]
                lensid = lens!!.id
                refreshApertureAdapter(lenslist[i])
                refreshFocalLength(lenslist[i])
                activity?.invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
            oldListener.onItemClick(adapterView, view, i, l)
        }

        edit_date.setOnClickListener { showDateDialogOnActivity() }

        edit_date.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        edit_accessories.setOnClickListener { showAccessoryDialogOnActivity() }

        edit_accessories.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        spinner_aperture.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })
        spinner_ss.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })

        seek_exp_compensation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                label_ev_corr_amount.text = toHumanReadableCompensationAmount(i - evGrainSize * evWidth)
                if(isResumed) isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        seek_ttl_light_meter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                label_ttl_light_meter.text = toHumanReadableCompensationAmount(i - evGrainSize * evWidth)
                if(isResumed) isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        seek_focal_length.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                label_focal_length.text = java.lang.Double.toString(seek_focal_length.progress + focalLengthRange.first) + "mm"
                if(isResumed) isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        edit_location.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })
        edit_memo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true

            }
        })
        btn_get_location.setOnClickListener {
            val intent = Intent(activity!!.application, MapsActivity::class.java)
            if (latitude != 999.0 && longitude != 999.0) {
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
            }
            startActivityForResult(intent, REQCODE_GET_LOCATION)
        }
        btn_mount_adapters.setOnClickListener {
            val fragment = CheckListDialogFragment.Builder()
                    .build(REQCODE_MOUNT_ADAPTERS)
            val dao = TrisquelDao(activity!!.applicationContext)
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
            fragment.showOn(this, "dialog")
        }
    }

    fun canSave(): Boolean {
        return spinner_lens.position >= 0
    }

    protected fun refreshApertureAdapter(lens: LensSpec?) {
        apertureAdapter?.clear()
        if (lens != null) {
            apertureAdapter?.addAll(lens.fSteps.map { d -> d.toString() })
            spinner_aperture.isEnabled = true
        }
        spinner_aperture.setAdapter(apertureAdapter)

        //写ルンですを考慮
        if (apertureAdapter!!.count == 1) {
            spinner_aperture.position = 0
        }
    }

    protected fun refreshFocalLength(lens: LensSpec?) {
        focalLengthRange = when(lens){
            null -> Pair(0.0, 0.0)
            else -> Util.getFocalLengthRangeFromStr(lens.focalLength)
        }
        label_focal_length.text = focalLengthRange.first.toString() + "mm"
        if (primeLens(focalLengthRange)) {
            seek_focal_length.visibility = View.GONE
        } else {
            seek_focal_length.visibility = View.VISIBLE
        }
        seek_focal_length.max = (focalLengthRange.second - focalLengthRange.first).toInt() // API Level 26以降ならsetMinが使えるのだが…
        seek_focal_length.progress = 0
    }

    protected fun getSuggestListSubPref(parentkey: String, subkey: String): ArrayList<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
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
        val pref = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            RETCODE_SDCARD_PERM_LOADIMG, RETCODE_SDCARD_PERM_IMGPICKER -> {
                onRequestSDCardAccessPermissionsResult(permissions, grantResults, requestCode)
            }
        }
    }

    internal fun onRequestSDCardAccessPermissionsResult(permissions: Array<String>, grantResults: IntArray, requestCode: Int) {
        val granted = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)
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
            Toast.makeText(activity, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
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
        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //ActivityCompat.requestPermissions(activity!!, PERMISSIONS, RETCODE_SDCARD_PERM_IMGPICKER)
            requestPermissions(PERMISSIONS, RETCODE_SDCARD_PERM_IMGPICKER)
            return
        }
        openImagePicker()
    }

    fun checkPermAndAppendSupplementalImages(paths: ArrayList<String>) {
        if(paths.size == 0) return
        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            supplementalImagesToLoad = paths
            requestPermissions(PERMISSIONS, RETCODE_SDCARD_PERM_LOADIMG)
            //ActivityCompat.requestPermissions(activity!!, )
            return
        }
        appendSupplementalImages(paths)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putString("date", edit_date.text.toString())
        outState.putDouble("latitude", latitude)
        outState.putDouble("longitude", longitude)
        outState.putInt("lensid", lensid)
        outState.putIntegerArrayList("selected_accessories", selectedAccessories)
        outState.putString("aperture", spinner_aperture.text.toString())
        outState.putDouble("focal_length", seek_focal_length.progress + focalLengthRange.first)
        outState.putString("shutter_speed", spinner_ss.text.toString())
        outState.putDouble("exp_compensation", expCompensation)
        outState.putDouble("ttl_light_meter", ttlLightMeter)
        outState.putString("location", edit_location.text.toString())
        outState.putString("memo", edit_memo.text.toString())
        outState.putStringArrayList("suppimgs", supplementalImages)
        outState.putBoolean("favorite", favorite)
        super.onSaveInstanceState(outState)
    }

    internal fun showDateDialogOnActivity() {
        DatePickerFragment.Builder()
                .build(REQCODE_DATE)
                .showOn(this, "dialog")
    }

    internal fun showAccessoryDialogOnActivity() {
        val fragment = CheckListDialogFragment.Builder()
                .build(REQCODE_ACCESSORY)
        val dao = TrisquelDao(activity!!.applicationContext)
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
        fragment.showOn(this@ShootingInfoEditFragment, "dialog")
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
            REQCODE_DATE -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val year = data.getIntExtra(DatePickerFragment.EXTRA_YEAR, 1970)
                val month = data.getIntExtra(DatePickerFragment.EXTRA_MONTH, 0)
                val day = data.getIntExtra(DatePickerFragment.EXTRA_DAY, 0)
                val c = Calendar.getInstance()
                c.set(Calendar.YEAR, year)
                c.set(Calendar.MONTH, month)
                c.set(Calendar.DAY_OF_MONTH, day)
                val sdf = SimpleDateFormat("yyyy/MM/dd")
                edit_date.setText(sdf.format(c.time))
            }
            REQCODE_MOUNT_ADAPTERS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                saveSuggestListSubPref("mount_adapters",
                        filmroll!!.camera.mount,
                        bundle!!.getStringArrayList("checked_items"))
                val dao = TrisquelDao(activity!!.applicationContext)
                dao.connection()
                val lens = when(spinner_lens.position){
                    -1 -> null
                    else -> dao.getLens(lenslist[spinner_lens.position].id)
                }
                updateLensList(lens, dao)
                dao.close()
                spinner_lens.clearFocus() //こうしないとドロップダウンリストが更新されない…気がする。バグか？
            }
            REQCODE_ACCESSORY -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                val tags = bundle!!.getIntegerArrayList("checked_tags")
                if (!sameArrayList(tags!!, selectedAccessories)) {
                    val dao = TrisquelDao(activity!!.applicationContext)
                    dao.connection()
                    setAccessories(dao, tags)
                    if(isResumed) isDirty = true
                    dao.close()
                }
            }
            REQCODE_ASK_CREATE_LENS -> if(resultCode == DialogInterface.BUTTON_POSITIVE){
                val intent = Intent(activity!!.application, EditLensActivity::class.java)
                startActivityForResult(intent, REQCODE_ADD_LENS)
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    private fun setLatLng(newlatitude: Double, newlongitude: Double) {
        val sb = StringBuilder()
        if (newlatitude == 999.0 || newlongitude == 999.0) {
            latitude = 999.0
            longitude = 999.0
            edit_location.setHelperText(null)
            edit_location.isHelperTextAlwaysShown = false
            btn_get_location.setImageResource(R.drawable.ic_place_gray_24dp)
        } else {
            latitude = newlatitude
            longitude = newlongitude
            edit_location.setHelperText(
                    getString(R.string.label_coordinate) +
                            ": " + java.lang.Double.toString(newlatitude) + ", " + java.lang.Double.toString(newlongitude))
            edit_location.isHelperTextAlwaysShown = true
            btn_get_location.setImageResource(R.drawable.ic_place_black_24dp)
        }
    }

    private fun appendSupplementalImages(paths: ArrayList<String>){
        for(s in paths) appendSupplementalImage(s)
    }

    private fun appendSupplementalImage(path: String){
        if(path in this.supplementalImages) return
        val options = BitmapFactory.Options()

        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(path, options)
        var (w, h) =
                if(options.outWidth > 0 && options.outHeight > 0)
                    Pair(options.outWidth, options.outHeight)
                else
                    Pair(150, 150)
        try {
            // 枠を回転させるかどうか考える
            val exifInterface = ExifInterface(path)
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

        val imageView = CustomImageView(activity!!)
        val scale = resources.displayMetrics.density
        val lp = LinearLayout.LayoutParams((150 * scale * w / h).toInt(), (150 * scale).toInt())
        lp.setMargins(0,0, 16, 0)
        image_container.addView(imageView, image_container.childCount - 1, lp)
        imageView.path = path
        imageView.setOnCloseClickListener(object: View.OnClickListener{
            override fun onClick(view: View?): Unit {
                if(view is CustomImageView) {
                    this@ShootingInfoEditFragment.supplementalImages.remove(view.path)
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
                    val file = File(view.path)
                    // android.os.FileUriExposedException回避
                    val photoURI = FileProvider.getUriForFile(activity!!, activity?.applicationContext?.packageName + ".provider", file)
                    intent.action = android.content.Intent.ACTION_VIEW
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
                edit_location.setText(bundle.getString("location"))
            } else if (resultCode == MapsActivity.RESULT_DELETE) {
                if (999.0 != latitude || 999.0 != longitude) if(isResumed) isDirty = true
                setLatLng(999.0, 999.0)
            }
            REQCODE_IMAGES -> if (resultCode == AppCompatActivity.RESULT_OK) {
                val paths = Matisse.obtainPathResult(data)
                for(p in paths){
                    if(p != null){
                        appendSupplementalImage(p)
                    }else{
                        Toast.makeText(activity!!, "Failed to obtain an image. The result was null.", Toast.LENGTH_LONG).show()
                    }
                }
                if(isResumed) isDirty = true
            }
            REQCODE_ADD_LENS -> if (resultCode == Activity.RESULT_OK) {
                if(data != null) {
                    val bundle = data.extras
                    val l = bundle!!.getParcelable<LensSpec>("lensspec")
                    val dao = TrisquelDao(activity!!)
                    dao.connection()
                    l.id = dao.addLens(l).toInt()
                    updateLensList(l, dao)
                    dao.close()
                    spinner_lens.clearFocus()
                    refreshApertureAdapter(l)
                    refreshFocalLength(l)
                    activity?.invalidateOptionsMenu()
                }
            }
        }
    }
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
        fun onFragmentAttached(f: ShootingInfoEditFragment)
    }

    companion object {

        fun newInstance(filmroll: Int, id: Int, frameIndex: Int): ShootingInfoEditFragment {
            val fragment = ShootingInfoEditFragment()
            val args = Bundle()
            args.putInt("filmroll", filmroll)
            args.putInt("id", id)
            args.putInt("frameIndex", frameIndex)
            fragment.arguments = args
            return fragment
        }
    }
}