package net.tnose.app.trisquel

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.rengwuxian.materialedittext.MaterialEditText
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import java.util.regex.Pattern

class EditCameraActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = 0
    private var type: Int = 0
    private var created: String? = null
    private var formatAdapter: ArrayAdapter<CharSequence>? = null
    private var ssAdapterOne: ArrayAdapter<CharSequence>? = null
    private var ssAdapterHalf: ArrayAdapter<CharSequence>? = null
    private var ssAdapterOneThird: ArrayAdapter<CharSequence>? = null
    private var radioStopOne: RadioButton? = null
    private var radioStopHalf: RadioButton? = null
    private var radioStopOneThird: RadioButton? = null
    private var spSsUb: MaterialBetterSpinner? = null
    private var spSsLb: MaterialBetterSpinner? = null
    private var editMount: ImmediateAutoCompleteTextView? = null
    private var editManufacturer: ImmediateAutoCompleteTextView? = null
    private var editModelName: TextView? = null
    private var spFormat: MaterialBetterSpinner? = null
    private var spEvGrain: Spinner? = null
    private var spEvWidth: Spinner? = null
    private var cbBulb: CheckBox? = null
    private var layoutFLC: LinearLayout? = null
    private var editFocalLength: MaterialEditText? = null
    private var editFixedLensName: MaterialEditText? = null
    private var fsGridView: GridView? = null
    private var fsAdapter: FStepAdapter? = null
    private var isDirty: Boolean = false
    private var userIsInteracting = false

    private val shutterSpeedRangeOk: Boolean
        get() {
            val fastestSS = Util.stringToDoubleShutterSpeed(spSsUb!!.text.toString())
            val slowestSS = Util.stringToDoubleShutterSpeed(spSsLb!!.text.toString())
            return if (fastestSS == 0.0 || slowestSS == 0.0) {
                false
            } else
                fastestSS <= slowestSS
        }

    private val focalLengthOk: Boolean
        get() {
            if (!editFocalLength!!.text.toString().isEmpty()) {
                val zoom = Pattern.compile("(\\d++)-(\\d++)")
                var m = zoom.matcher(editFocalLength!!.text.toString())
                if (m.find()) {
                    return true
                }

                val prime = Pattern.compile("(\\d++)")
                m = prime.matcher(editFocalLength!!.text.toString())
                if (m.find()) {
                    return true
                }
            }
            return false
        }

    protected val isDataChanged: Boolean
        get() = false

    private val ssGrainSize: Int
        get() = if (radioStopOne!!.isChecked) {
            1
        } else if (radioStopHalf!!.isChecked) {
            2
        } else {
            3
        }

    /*
                  "shutter_speeds"はまだ対応しない
                */
    val data: Intent
        get() {
            val data = Intent()
            data.putExtra("id", this.id)
            data.putExtra("type", this.type)
            data.putExtra("created", this.created)
            data.putExtra("mount", editMount!!.text.toString())
            data.putExtra("manufacturer", editManufacturer!!.text.toString())
            data.putExtra("model_name", editModelName!!.text.toString())
            data.putExtra("format", spFormat!!.position)
            data.putExtra("ss_grain_size", ssGrainSize)
            data.putExtra("fastest_ss", Util.stringToDoubleShutterSpeed(spSsUb!!.text.toString()))
            data.putExtra("slowest_ss", Util.stringToDoubleShutterSpeed(spSsLb!!.text.toString()))
            data.putExtra("bulb_available", if (cbBulb!!.isChecked) 1 else 0)
            data.putExtra("ev_grain_size", spEvGrain!!.selectedItemPosition + 1)
            data.putExtra("ev_width", spEvWidth!!.selectedItemPosition + 1)
            if (type == 1) {
                data.putExtra("fixedlens_name", editFixedLensName!!.text.toString())
                data.putExtra("fixedlens_focal_length", editFocalLength!!.text.toString())
                data.putExtra("fixedlens_f_steps", fsAdapter!!.fStepsString)
            }
            return data
        }

    private fun refreshShutterSpeedSpinners() {
        if (radioStopOne!!.isChecked) {
            spSsUb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
            spSsLb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
        } else if (radioStopHalf!!.isChecked) {
            spSsUb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterHalf)
            spSsLb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterHalf)
        } else {
            spSsUb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOneThird)
            spSsLb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOneThird)
        }
    }

    private fun doShutterSpeedValidation(): Boolean {
        var ret = true
        val fastestSS = Util.stringToDoubleShutterSpeed(spSsUb!!.text.toString())
        val slowestSS = Util.stringToDoubleShutterSpeed(spSsLb!!.text.toString())
        if (fastestSS == 0.0 || slowestSS == 0.0) { //未設定なだけなのでエラー表示にはしない
            spSsUb!!.error = null
            spSsLb!!.error = null
        } else if (fastestSS > slowestSS) {
            spSsUb!!.error = "okashii"
            spSsLb!!.error = ""
            ret = false
        } else {
            spSsUb!!.error = null
            spSsLb!!.error = null
        }
        return ret
    }

    protected fun findViews() {
        radioStopOne = findViewById(R.id.radio_stop_one)
        radioStopHalf = findViewById(R.id.radio_stop_half)
        radioStopOneThird = findViewById(R.id.radio_stop_one_third)
        spSsUb = findViewById(R.id.spinner_fastest_ss)
        spSsLb = findViewById(R.id.spinner_slowest_ss)
        editManufacturer = findViewById(R.id.edit_manufacturer)
        editMount = findViewById(R.id.edit_mount)
        editModelName = findViewById(R.id.edit_model)
        spFormat = findViewById(R.id.spinner_format)
        spEvGrain = findViewById(R.id.spinner_ev_grain_size)
        spEvWidth = findViewById(R.id.spinner_ev_width)
        cbBulb = findViewById(R.id.check_bulb_available)
        layoutFLC = findViewById(R.id.layout_flc)
        editFocalLength = findViewById(R.id.edit_focal_length_flc)
        editFixedLensName = findViewById(R.id.edit_lens_name_flc)
        fsGridView = findViewById(R.id.gridview)
    }

    protected fun loadData(data: Intent, savedInstanceState: Bundle?) {
        val id = data.getIntExtra("id", -1)
        this.id = id
        val type = data.getIntExtra("type", 0)
        this.type = type
        if (type == 1) {
            editMount!!.visibility = View.GONE
            layoutFLC!!.visibility = View.VISIBLE
            setTitle(R.string.title_activity_edit_cam_and_lens)
            fsAdapter = FStepAdapter(this)
            if (savedInstanceState != null) {
                fsAdapter!!.setCheckedState(savedInstanceState.getString("FStepsString"))
            }
        } else {
            setTitle(R.string.title_activity_edit_cam)
        }

        if (id <= 0) {
            if (type == 1) fsGridView!!.adapter = fsAdapter
            return
        }

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val c = dao.getCamera(id)
        var l: LensSpec? = null
        if (c!!.type == 1) {
            l = dao.getLens(dao.getFixedLensIdByBody(c.id))
        }
        dao.close()
        this.created = Util.dateToStringUTC(c.created)
        editMount!!.setText(c.mount)
        editManufacturer!!.setText(c.manufacturer)
        editModelName!!.text = c.modelName
        if (c.format < 0) c.format = 0
        spFormat!!.position = c.format
        when (c.shutterSpeedGrainSize) {
            1 -> radioStopOne!!.isChecked = true
            2 -> radioStopHalf!!.isChecked = true
            3 -> radioStopOneThird!!.isChecked = true
        }
        spSsUb!!.setText(Util.doubleToStringShutterSpeed(c.fastestShutterSpeed!!))
        spSsLb!!.setText(Util.doubleToStringShutterSpeed(c.slowestShutterSpeed!!))
        cbBulb!!.isChecked = c.bulbAvailable
        spEvGrain!!.setSelection(c.evGrainSize - 1, false)
        spEvWidth!!.setSelection(c.evWidth - 1, false)

        if (c.type == 1) {
            if (savedInstanceState == null) {
                fsAdapter!!.setCheckedState(l!!.fSteps)
            }
            fsGridView!!.adapter = fsAdapter
            editFixedLensName!!.setText(l!!.modelName)
            editFocalLength!!.setText(l.focalLength)
        }
    }

    protected fun setEventListeners() {
        editMount!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
                invalidateOptionsMenu()
            }
        })

        editMount!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            isDirty = true
        }

        editManufacturer!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
            }
        })

        editModelName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
                invalidateOptionsMenu()
                if (editModelName!!.text.toString() == "jenkinsushi") {
                    Toast.makeText(this@EditCameraActivity,
                            getString(R.string.google_maps_key),
                            Toast.LENGTH_LONG).show()
                }
            }
        })

        spFormat!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
            }
        })

        val rg = findViewById<RadioGroup>(R.id.radiogroup_ss_stop)
        rg.setOnCheckedChangeListener { group, checkedId ->
            refreshShutterSpeedSpinners()
            isDirty = true
        }

        spSsUb!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            doShutterSpeedValidation()
            invalidateOptionsMenu()
            isDirty = true
        }

        spSsLb!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            doShutterSpeedValidation()
            invalidateOptionsMenu()
            isDirty = true
        }

        cbBulb!!.setOnCheckedChangeListener { buttonView, isChecked -> isDirty = true }


        spEvGrain!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (userIsInteracting) isDirty = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (userIsInteracting) isDirty = true
            }
        }

        spEvWidth!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (userIsInteracting) isDirty = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (userIsInteracting) isDirty = true
            }
        }

        editFocalLength!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })

        editFixedLensName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })

        fsGridView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            isDirty = true
        }
    }

    protected fun canSave(): Boolean {
        val cameraOk: Boolean
        val lensOk: Boolean
        //マウントが空でない
        //モデル名が空でない
        //シャッタースピードレンジが設定されていて内容に矛盾がない
        cameraOk = (type == 1 || type == 0 && editMount!!.text.length > 0) &&
                editModelName!!.text.length > 0 &&
                shutterSpeedRangeOk

        //レンズ付きモデルの場合
        //焦点距離が設定されている
        //F値リストに少なくとも1つチェックが入っている
        if (type == 1) {
            lensOk = focalLengthOk && !fsAdapter!!.fStepsString.isEmpty()
        } else {
            lensOk = true
        }
        return cameraOk && lensOk
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        userIsInteracting = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_camera)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        findViews()

        ssAdapterOne = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one, android.R.layout.simple_spinner_item)
        ssAdapterHalf = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_half, android.R.layout.simple_spinner_item)
        ssAdapterOneThird = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one_third, android.R.layout.simple_spinner_item)

        ssAdapterOne!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ssAdapterHalf!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ssAdapterOneThird!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spSsUb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
        spSsLb!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)

        spSsUb!!.setHelperText(getString(R.string.label_shutter_speed_fastest))
        spSsLb!!.setHelperText(getString(R.string.label_shutter_speed_slowest))

        formatAdapter = ArrayAdapter.createFromResource(this, R.array.film_formats, android.R.layout.simple_spinner_item)
        formatAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFormat!!.setAdapter<ArrayAdapter<CharSequence>>(formatAdapter)
        spFormat!!.position = FilmFormat.FULL_FRAME.ordinal

        val manufacturer_adapter = getSuggestListPref("camera_manufacturer",
                R.array.camera_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        editManufacturer!!.setAdapter(manufacturer_adapter)

        val mount_adapter = getSuggestListPref("camera_mounts",
                R.array.camera_mounts,
                android.R.layout.simple_dropdown_item_1line)
        editMount!!.setAdapter(mount_adapter)

        val data = intent
        if (data != null) {
            loadData(data, savedInstanceState)
        } else {
            this.id = -1
            this.type = 0
        }
        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if(fsAdapter != null)
            outState.putString("FStepsString", fsAdapter!!.fStepsString)
        outState.putBoolean("isDirty", isDirty)
        super.onSaveInstanceState(outState)
    }

    protected fun getSuggestListPref(prefkey: String, defRscId: Int, resource: Int): ArrayAdapter<String> {
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

        for (i in defRsc.indices) {
            if (strArray.contains(defRsc[i])) continue
            strArray.add(defRsc[i])
        }

        return ArrayAdapter<String>(this, resource, strArray)
    }

    protected fun saveSuggestListPref(prefkey: String, defRscId: Int, newValue: String) {
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
        for (i in defRsc.indices) {
            if (strArray.contains(defRsc[i])) continue
            strArray.add(defRsc[i])
        }
        val result = JSONArray(strArray)
        val e = pref.edit()
        e.putString(prefkey, result.toString())
        e.apply()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            101 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val resultData = data
                setResult(Activity.RESULT_OK, resultData)
                saveSuggestListPref("camera_manufacturer",
                        R.array.camera_manufacturer, editManufacturer!!.text.toString())
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, editMount!!.text.toString())
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
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
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
                saveSuggestListPref("camera_manufacturer",
                        R.array.camera_manufacturer, editManufacturer!!.text.toString())
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, editMount!!.text.toString())
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save, menu)
        menu.findItem(R.id.menu_save).isEnabled = canSave()
        return true
    }

    override fun onBackPressed() {
        Log.d("onBackPressed", "EditCameraActivity")
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
}
