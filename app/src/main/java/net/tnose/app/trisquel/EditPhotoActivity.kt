package net.tnose.app.trisquel

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_edit_photo.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class EditPhotoActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    internal val REQCODE_GET_LOCATION = 100
    internal val REQCODE_MOUNT_ADAPTERS = 103
    internal val REQCODE_DATE = 104
    internal val REQCODE_ACCESSORY = 105
    private var evGrainSize = 3
    private var evWidth = 3
    private var flWideEnd = 31.0
    private var flTeleEnd = 31.0

    private var filmroll: FilmRoll? = null
    private var photo: Photo? = null
    private var lenslist: ArrayList<LensSpec> = ArrayList()
    internal var lensadapter: LensAdapter? = null
    internal var apertureAdapter: ArrayAdapter<String>? = null

    private var ssAdapter: ArrayAdapter<String>? = null
    private var lensid: Int = 0
    private var lens: LensSpec? = null
    private var id: Int = 0
    private var index: Int = 0
    private var latitude = 999.0
    private var longitude = 999.0
    private var selectedAccessories: ArrayList<Int>? = null
    private var isDirty: Boolean = false

    val data: Intent
        get() {
            val data = Intent()
            data.putExtra("id", id)
            data.putExtra("index", index)
            data.putExtra("filmroll", filmroll!!.id)
            data.putExtra("date", edit_date.text.toString())
            data.putExtra("lens", lenslist[spinner_lens.position].id)
            data.putExtra("focal_length", seek_focal_length.progress + flWideEnd)
            var a: Double?
            try {
                a = java.lang.Double.parseDouble(spinner_aperture.text.toString())
            } catch (e: NumberFormatException) {
                a = 0.0
            }

            data.putExtra("aperture", a)
            data.putExtra("shutter_speed", Util.stringToDoubleShutterSpeed(spinner_ss.text.toString()))

            val bd = BigDecimal((seek_exp_compensation.progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
            data.putExtra("exp_compensation", java.lang.Double.parseDouble(bd2.toPlainString()))

            val bd3 = BigDecimal((seek_ttl_light_meter.progress - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
            val bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN)
            data.putExtra("ttl_light_meter", java.lang.Double.parseDouble(bd4.toPlainString()))

            data.putExtra("location", edit_location.text.toString())
            data.putExtra("latitude", latitude)
            data.putExtra("longitude", longitude)
            data.putExtra("memo", edit_memo.text.toString())

            val sb = StringBuilder("/")
            for (accessory in selectedAccessories!!) {
                sb.append(accessory)
                sb.append('/')
            }
            data.putExtra("accessories", sb.toString())
            return data
        }

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

    private val photoText: String
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
                if (edit_location.text.isNotEmpty())
                    sb.append(getString(R.string.label_location) + ": " + edit_location.text.toString() + "\n")
                if (latitude != 999.0 && longitude != 999.0)
                    sb.append(getString(R.string.label_coordinate) + ": " + java.lang.Double.toString(latitude) + ", " + java.lang.Double.toString(longitude) + "\n")
                if (edit_memo.text.isNotEmpty())
                    sb.append(getString(R.string.label_memo) + ": " + edit_memo.text.toString() + "\n")
                if (edit_accessories.text.isNotEmpty())
                    sb.append(getString(R.string.label_accessories) + ": " + edit_accessories.text.toString() + "\n")
            }
            return sb.toString()
        }

    protected fun updateLensList(selectedId: Int, dao: TrisquelDao) {
        if (filmroll!!.camera.type == 1) { //問答無用。selectedId等は完全無視
            lens = dao.getLens(dao.getFixedLensIdByBody(filmroll!!.camera.id))
            lensid = lens!!.id
            lenslist = ArrayList()
            lenslist.add(lens!!)
            spinner_lens.isEnabled = false
            btn_mount_adapters.isEnabled = false
            btn_mount_adapters.setImageResource(R.drawable.ic_mount_adapter_disabled)
        } else {
            lenslist = dao.getLensesByMount(filmroll!!.camera.mount)
            for (s in getSuggestListSubPref("mount_adapters", filmroll!!.camera.mount)) {
                lenslist.addAll(dao.getLensesByMount(s))
            }
            if (selectedId > 0) {
                lens = dao.getLens(selectedId)
                if (lens != null && this.filmroll!!.camera.mount != lens!!.mount) {
                    lenslist.add(0, lens!!)
                }
            }
        }
        lensadapter = LensAdapter(this, android.R.layout.simple_spinner_item, lenslist)
        lensadapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_lens.setAdapter(lensadapter)
        if (lensadapter!!.count == 0) {
            spinner_lens.error = getString(R.string.error_nolens).replace("%s", filmroll!!.camera.mount)
        }

        if (selectedId > 0) {
            spinner_lens.position = lensadapter!!.getPosition(selectedId)
        } else if (filmroll!!.camera.type == 1) {
            spinner_lens.position = 0
        }
    }

    private fun setAccessories(dao: TrisquelDao, accessories: ArrayList<Int>?) {
        selectedAccessories!!.clear()
        selectedAccessories!!.addAll(accessories!!)
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

    protected fun loadData(data: Intent, dao: TrisquelDao, savedInstanceState: Bundle?) {
        this.id = data.getIntExtra("id", -1)
        this.index = data.getIntExtra("index", -1)
        val filmrollid = data.getIntExtra("filmroll", -1)
        this.filmroll = dao.getFilmRoll(filmrollid)
        this.photo = dao.getPhoto(id)
        if (photo != null) {
            lensid = photo!!.lensid
        } else {
            val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val autocomplete = pref.getBoolean("autocomplete_from_previous_shot", false)
            if (autocomplete) {
                val ps = dao.getPhotosByFilmRollId(filmrollid)
                val lastPhoto: Photo
                if (this.index > 0) {
                    lensid = ps[this.index - 1].lensid
                } else if (this.index < 0 && ps.size > 0) {
                    lensid = ps[ps.size - 1].lensid
                } else {
                    lensid = -1
                }
            } else {
                lensid = -1
            }
        }

        if (photo != null && !photo!!.date.isEmpty()) {
            edit_date.setText(photo!!.date)
        } else {
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            edit_date.setText(sdf.format(calendar.time))
        }

        updateLensList(lensid, dao)
        /*
        if(filmroll.camera.type == 1){
            lens = dao.getLens(dao.getFixedLensIdByBody(filmroll.camera.id));
            lensid = lens.id;
            lenslist = new ArrayList<LensSpec>();
            lenslist.add(lens);
            spinner_lens.setEnabled(false);
        }else{
            lens = dao.getLens(lensid);
            lenslist = dao.getLensesByMount(this.filmroll.camera.mount);
            //Javaなので左から評価されることは保証されている
            if(lens != null && !this.filmroll.camera.mount.equals(lens.mount)){
                lenslist.add(0, lens);
            }
        }
        */

        if (lens != null) {
            if (lens!!.focalLength.indexOf("-") > 0) {
                val range = lens!!.focalLength.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                flWideEnd = java.lang.Double.parseDouble(range[0])
                flTeleEnd = java.lang.Double.parseDouble(range[1])
            } else {
                flWideEnd = java.lang.Double.parseDouble(lens!!.focalLength)
                flTeleEnd = flWideEnd
            }
            refreshApertureAdapter(lens)
            refreshFocalLength(lens)
        } else {
            flWideEnd = 0.0
            flTeleEnd = 0.0
        }
        this.evGrainSize = filmroll!!.camera.evGrainSize
        this.evWidth = filmroll!!.camera.evWidth
        if (photo != null) {
            if (photo!!.shutterSpeed > 0) spinner_ss.setText(Util.doubleToStringShutterSpeed(photo!!.shutterSpeed))
            edit_location.setText(photo!!.location)
            edit_memo.setText(photo!!.memo)
        }

        if (savedInstanceState != null) {
            setLatLng(savedInstanceState.getDouble("latitude"), savedInstanceState.getDouble("longitude"))
            setAccessories(dao, savedInstanceState.getIntegerArrayList("selected_accessories"))
        } else {
            if (photo != null) {
                setLatLng(photo!!.latitude, photo!!.longitude)
                setAccessories(dao, photo!!.accessories)
            } else {
                setLatLng(999.0, 999.0)
            }
        }
    }

    protected fun setEventListeners() {
        val oldListener = spinner_lens.onItemClickListener // これやらないとgetPositionがおかしくなる
        spinner_lens.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            lens = lenslist[i]
            lensid = lens!!.id
            refreshApertureAdapter(lenslist[i])
            refreshFocalLength(lenslist[i])
            invalidateOptionsMenu()
            isDirty = true
            oldListener.onItemClick(adapterView, view, i, l)
        }

        edit_date.setOnClickListener { showDateDialogOnActivity() }

        edit_date.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
            }
        })


        edit_accessories.setOnClickListener { showAccessoryDialogOnActivity() }

        edit_accessories.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
            }
        })

        spinner_aperture.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true

            }
        })
        spinner_ss.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true

            }
        })

        seek_exp_compensation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val bd = BigDecimal((i - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
                val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
                label_ev_corr_amount.text = (if (bd2.signum() > 0) "+" else "") + bd2.toPlainString() + "EV"
                isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        seek_ttl_light_meter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val bd = BigDecimal((i - evGrainSize * evWidth).toDouble() / evGrainSize.toDouble())
                val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
                label_ttl_light_meter.text = (if (bd2.signum() > 0) "+" else "") + bd2.toPlainString() + "EV"
                isDirty = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        seek_focal_length.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                label_focal_length.text = java.lang.Double.toString(seek_focal_length.progress + flWideEnd) + "mm"
                isDirty = true
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
                isDirty = true

            }
        })
        edit_memo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true

            }
        })
        btn_get_location.setOnClickListener {
            val intent = Intent(application, MapsActivity::class.java)
            if (latitude != 999.0 && longitude != 999.0) {
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
            }
            startActivityForResult(intent, REQCODE_GET_LOCATION)
        }
        btn_mount_adapters.setOnClickListener {
            val fragment = CheckListDialogFragment.Builder()
                    .build(REQCODE_MOUNT_ADAPTERS)
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

            fragment.arguments.putStringArray("items", availableLensMountsArray)
            fragment.arguments.putIntegerArrayList("checked_indices", checkedIndices)
            fragment.arguments.putString("title", getString(R.string.msg_select_mount_adapters).replace("%s", mount))
            fragment.arguments.putString("positive", getString(android.R.string.yes))
            fragment.arguments.putString("negative", getString(android.R.string.no))
            fragment.showOn(this@EditPhotoActivity, "dialog")
        }
    }

    protected fun canSave(): Boolean {
        return spinner_lens.position >= 0
    }

    protected fun refreshApertureAdapter(lens: LensSpec?) {
        if (lens == null) {
            apertureAdapter?.clear()
        } else {
            val list = ArrayList<String>()
            val d = lens.fSteps
            apertureAdapter?.clear()
            for (j in d.indices) { //ダサい
                apertureAdapter?.add(java.lang.Double.toString(d[j]))
            }
        }
        spinner_aperture.setAdapter(apertureAdapter)
        spinner_aperture.isEnabled = true
    }

    fun refreshFocalLength(lens: LensSpec?) {
        if (lens == null) {
            flWideEnd = 0.0
            flTeleEnd = 0.0
        } else {
            if (lens.focalLength.indexOf("-") > 0) {
                val range = lens.focalLength.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                flWideEnd = java.lang.Double.parseDouble(range[0])
                flTeleEnd = java.lang.Double.parseDouble(range[1])
            } else {
                flWideEnd = java.lang.Double.parseDouble(lens.focalLength)
                flTeleEnd = flWideEnd
            }
        }
        if (flWideEnd == flTeleEnd) {
            label_focal_length.text = java.lang.Double.toString(flWideEnd) + "mm (" + getString(R.string.prime) + ")"
            seek_focal_length.visibility = View.GONE
        } else {
            label_focal_length.text = java.lang.Double.toString(flWideEnd) + "mm"
            //label_focal_length.setText(Double.toString(flWideEnd) + "-" + Double.toString(flTeleEnd) + "mm");
            seek_focal_length.visibility = View.VISIBLE
        }
        seek_focal_length.max = (flTeleEnd - flWideEnd).toInt() // API Level 26以降ならsetMinが使えるのだが…
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_photo)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        edit_location.helperTextColor = R.color.common_google_signin_btn_text_light_default

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent

        apertureAdapter = ArrayAdapter(this, /* これでいいのか？ */
                android.R.layout.simple_dropdown_item_1line)
        spinner_aperture.isEnabled = false

        selectedAccessories = ArrayList()

        lensid = -1
        lens = null
        if (data != null) {
            loadData(data, dao, savedInstanceState)
        } else {
            this.id = -1
            lenslist = dao.allLenses
            lensadapter = LensAdapter(this, android.R.layout.simple_spinner_item, lenslist)
            lensadapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_lens.setAdapter(lensadapter)
            if (lensadapter?.count == 0) {
                spinner_lens.error = getString(R.string.error_nolens).replace("%s", filmroll!!.camera.mount)
            }
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            edit_date.setText(sdf.format(calendar.time))
        }
        dao.close()

        val ssArray: Array<String>
        when (filmroll!!.camera.shutterSpeedGrainSize) {
            1 -> ssArray = resources.getStringArray(R.array.shutter_speeds_one)
            2 -> ssArray = resources.getStringArray(R.array.shutter_speeds_half)
            else -> ssArray = resources.getStringArray(R.array.shutter_speeds_one_third)
        }
        val ssList = ArrayList(Arrays.asList(*ssArray))
        for (i in ssList.indices.reversed()) {
            val ssval = Util.stringToDoubleShutterSpeed(ssList[i])
            if (ssval > filmroll!!.camera.slowestShutterSpeed!! || ssval < filmroll!!.camera.fastestShutterSpeed!!) {
                ssList.removeAt(i)
            }
        }

        ssAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ssList.toTypedArray())
        ssAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_ss.setAdapter<ArrayAdapter<String>>(ssAdapter)

        /*if(lensid > 0) {
            refreshApertureAdapter(lens);
            refreshFocalLength(lens);
        }*/

        if (photo != null) {
            if (photo!!.aperture > 0) spinner_aperture.setText(Util.doubleToStringShutterSpeed(photo!!.aperture)) //これもごまかし
        } else {
            // 写ルンです（シャッタースピード・絞り固定）を考慮
            if (ssAdapter!!.count == 1) {
                spinner_ss.position = 0
            }
            if (apertureAdapter!!.count == 1) {
                spinner_aperture.position = 0
            }
        }

        seek_exp_compensation.max = evWidth * 2 * evGrainSize
        seek_ttl_light_meter.max = evWidth * 2 * evGrainSize
        if (photo != null) {
            if (flTeleEnd != flWideEnd) {
                seek_focal_length.progress = (photo!!.focalLength - flWideEnd).toInt()
                label_focal_length.text = java.lang.Double.toString(photo!!.focalLength) + "mm"
            }

            seek_exp_compensation.progress = ((evWidth + photo!!.expCompensation) * evGrainSize).toInt()
            val bd = BigDecimal(photo!!.expCompensation)
            val bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN)
            label_ev_corr_amount.text = (if (bd2.signum() > 0) "+" else "") + bd2.toPlainString() + "EV"

            seek_ttl_light_meter.progress = ((evWidth + photo!!.ttlLightMeter) * evGrainSize).toInt()
            val bd3 = BigDecimal(photo!!.ttlLightMeter)
            val bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN)
            label_ttl_light_meter.text = (if (bd4.signum() > 0) "+" else "") + bd4.toPlainString() + "EV"
        } else {
            seek_exp_compensation.progress = evWidth * evGrainSize
            seek_ttl_light_meter.progress = evWidth * evGrainSize
        }

        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putDouble("latitude", latitude)
        outState.putDouble("longitude", longitude)
        outState.putInt("lensid", lensid)
        outState.putIntegerArrayList("selected_accessories", selectedAccessories)
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
            if (selectedAccessories!!.contains(a.id)) {
                chkidx.add(i)
            }
            tags.add(a.id)
        }

        fragment.arguments.putStringArray("items", a_str.toTypedArray())
        fragment.arguments.putIntegerArrayList("tags", tags)
        fragment.arguments.putIntegerArrayList("checked_indices", chkidx)
        fragment.arguments.putString("title", getString(R.string.title_dialog_select_accessories))
        fragment.arguments.putString("positive", getString(android.R.string.yes))
        fragment.arguments.putString("negative", getString(android.R.string.no))
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
            REQCODE_DATE -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if (data != null) {
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
            }
            101 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val resultData = data
                setResult(Activity.RESULT_OK, resultData)
                finish()
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, data)
                finish()
            }
            102 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                /* do nothing */
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, data)
                finish()
            }
            REQCODE_MOUNT_ADAPTERS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val bundle = data.extras
                saveSuggestListSubPref("mount_adapters",
                        filmroll!!.camera.mount,
                        bundle!!.getStringArrayList("checked_items"))
                val dao = TrisquelDao(applicationContext)
                dao.connection()
                var id = -1
                if (spinner_lens.position >= 0) {
                    id = lenslist[spinner_lens.position].id
                }
                updateLensList(id, dao)
                dao.close()
                spinner_lens.clearFocus() //こうしないとドロップダウンリストが更新されない…気がする。バグか？
            }
            REQCODE_ACCESSORY -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if (data != null) {
                    val bundle = data.extras
                    val tags = bundle!!.getIntegerArrayList("checked_tags")
                    if (!sameArrayList(tags!!, selectedAccessories!!)) {
                        val dao = TrisquelDao(applicationContext)
                        dao.connection()
                        setAccessories(dao, tags)
                        isDirty = true
                        dao.close()
                    }
                }
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
                if (isDirty)
                    Log.d("Dirty?", "yes")
                else
                    Log.d("Dirty?", "no")
                if (!isDirty) {
                    setResult(Activity.RESULT_CANCELED, Intent())
                    finish()
                } else {
                    if (canSave()) {
                        val fragment = YesNoDialogFragment.Builder()
                                .build(101)
                        fragment.arguments.putString("message", getString(R.string.msg_save_or_discard_data))
                        fragment.arguments.putString("positive", getString(R.string.save))
                        fragment.arguments.putString("negative", getString(R.string.discard))
                        fragment.showOn(this, "dialog")
                    } else {
                        val fragment = YesNoDialogFragment.Builder()
                                .build(102)
                        fragment.arguments.putString("message", getString(R.string.msg_continue_editing_or_discard_data))
                        fragment.arguments.putString("positive", getString(R.string.continue_editing))
                        fragment.arguments.putString("negative", getString(R.string.discard))
                        fragment.showOn(this, "dialog")
                    }
                }
                return true
            }
            R.id.menu_save -> {
                setResult(Activity.RESULT_OK, data)
                finish()
                return true
            }
            R.id.menu_copy -> {
                val s = photoText
                if (s.isEmpty()) {
                    return true
                }
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.primaryClip = ClipData.newPlainText("", s)
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        Log.d("onBackPressed", "EditPhotoActivity")
        if (!isDirty) {
            setResult(Activity.RESULT_CANCELED, Intent())
            super.onBackPressed()
        } else {
            if (canSave()) {
                val fragment = YesNoDialogFragment.Builder()
                        .build(101)
                fragment.arguments.putString("message", getString(R.string.msg_save_or_discard_data))
                fragment.arguments.putString("positive", getString(R.string.save))
                fragment.arguments.putString("negative", getString(R.string.discard))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(102)
                fragment.arguments.putString("message", getString(R.string.msg_continue_editing_or_discard_data))
                fragment.arguments.putString("positive", getString(R.string.continue_editing))
                fragment.arguments.putString("negative", getString(R.string.discard))
                fragment.showOn(this, "dialog")
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQCODE_GET_LOCATION -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val newlat: Double
                val newlog: Double
                newlat = bundle!!.getDouble("latitude")
                newlog = bundle.getDouble("longitude")
                if (newlat != latitude || newlog != longitude) isDirty = true
                setLatLng(newlat, newlog)
                edit_location.setText(bundle.getString("location"))
            } else if (resultCode == MapsActivity.RESULT_DELETE) {
                if (999.0 != latitude || 999.0 != longitude) isDirty = true
                setLatLng(999.0, 999.0)
            }
        }
    }
}