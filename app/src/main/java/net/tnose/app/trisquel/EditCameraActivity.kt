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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_edit_camera.*
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import java.util.regex.Pattern

class EditCameraActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = 0
    private var type: Int = 0
    private var created: String = ""
    private var formatAdapter: ArrayAdapter<CharSequence>? = null
    private var ssAdapterOne: ArrayAdapter<CharSequence>? = null
    private var ssAdapterHalf: ArrayAdapter<CharSequence>? = null
    private var ssAdapterOneThird: ArrayAdapter<CharSequence>? = null
    private var fsAdapter: FStepAdapter? = null
    private var isDirty: Boolean = false
    private var isResumed: Boolean = false
    private var userIsInteracting = false

    private val shutterSpeedRangeOk: Boolean
        get() {
            val fastestSS = Util.stringToDoubleShutterSpeed(spinner_fastest_ss!!.text.toString())
            val slowestSS = Util.stringToDoubleShutterSpeed(spinner_slowest_ss!!.text.toString())
            return if (fastestSS == 0.0 || slowestSS == 0.0) {
                false
            } else
                fastestSS <= slowestSS
        }

    private val focalLengthOk: Boolean
        get() {
            if (!edit_focal_length_flc!!.text.toString().isEmpty()) {
                val zoom = Pattern.compile("(\\d++)-(\\d++)")
                var m = zoom.matcher(edit_focal_length_flc!!.text.toString())
                if (m.find()) {
                    return true
                }

                val prime = Pattern.compile("(\\d++)")
                m = prime.matcher(edit_focal_length_flc!!.text.toString())
                if (m.find()) {
                    return true
                }
            }
            return false
        }

    protected val isDataChanged: Boolean
        get() = false

    private var ssGrainSize: Int
        get() = if (radio_stop_one!!.isChecked) {
            1
        } else if (radio_stop_half!!.isChecked) {
            2
        } else {
            3
        }
        set(value) =
            when (value) {
                1 -> radio_stop_one!!.isChecked = true
                2 -> radio_stop_half!!.isChecked = true
                else -> radio_stop_one_third!!.isChecked = true
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
            data.putExtra("mount", edit_mount!!.text.toString())
            data.putExtra("manufacturer", edit_manufacturer!!.text.toString())
            data.putExtra("model_name", edit_model!!.text.toString())
            data.putExtra("format", spinner_format!!.position)
            data.putExtra("ss_grain_size", ssGrainSize)
            data.putExtra("fastest_ss", Util.stringToDoubleShutterSpeed(spinner_fastest_ss!!.text.toString()))
            data.putExtra("slowest_ss", Util.stringToDoubleShutterSpeed(spinner_slowest_ss!!.text.toString()))
            data.putExtra("bulb_available", if (check_bulb_available!!.isChecked) 1 else 0)
            data.putExtra("ev_grain_size", spinner_ev_grain_size!!.selectedItemPosition + 1)
            data.putExtra("ev_width", spinner_ev_width!!.selectedItemPosition + 1)
            if (type == 1) {
                data.putExtra("fixedlens_name", edit_lens_name_flc!!.text.toString())
                data.putExtra("fixedlens_focal_length", edit_focal_length_flc!!.text.toString())
                data.putExtra("fixedlens_f_steps", fsAdapter!!.fStepsString)
            }
            return data
        }

    private fun refreshShutterSpeedSpinners() {
        if (radio_stop_one!!.isChecked) {
            spinner_fastest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
            spinner_slowest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
        } else if (radio_stop_half!!.isChecked) {
            spinner_fastest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterHalf)
            spinner_slowest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterHalf)
        } else {
            spinner_fastest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOneThird)
            spinner_slowest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOneThird)
        }
    }

    private fun doShutterSpeedValidation(): Boolean {
        var ret = true
        val fastestSS = Util.stringToDoubleShutterSpeed(spinner_fastest_ss!!.text.toString())
        val slowestSS = Util.stringToDoubleShutterSpeed(spinner_slowest_ss!!.text.toString())
        if (fastestSS == 0.0 || slowestSS == 0.0) { //未設定なだけなのでエラー表示にはしない
            spinner_fastest_ss!!.error = null
            spinner_slowest_ss!!.error = null
        } else if (fastestSS > slowestSS) {
            spinner_fastest_ss!!.error = "okashii"
            spinner_slowest_ss!!.error = ""
            ret = false
        } else {
            spinner_fastest_ss!!.error = null
            spinner_slowest_ss!!.error = null
        }
        return ret
    }

    protected fun loadData(data: Intent, dao: TrisquelDao, savedInstanceState: Bundle?) {
        val id = data.getIntExtra("id", -1)
        this.id = id
        val type = data.getIntExtra("type", 0)
        this.type = type

        if(type == 1) {
            if (id < 0) setTitle(R.string.title_activity_reg_cam_and_lens)
            else        setTitle(R.string.title_activity_edit_cam_and_lens)

            edit_mount!!.visibility = View.GONE
            layout_flc!!.visibility = View.VISIBLE
            fsAdapter = FStepAdapter(this)
        }else{
            if (id < 0) setTitle(R.string.title_activity_reg_cam)
            else        setTitle(R.string.title_activity_edit_cam)
        }

        if(id >= 0 && savedInstanceState == null) { //既存データを開きたて
            val c = dao.getCamera(id)
            var l: LensSpec? = null
            if(type == 1) {
                l = dao.getLens(dao.getFixedLensIdByBody(id))
                fsAdapter!!.setCheckedState(l!!.fSteps)
                edit_lens_name_flc!!.setText(l.modelName)
                edit_focal_length_flc!!.setText(l.focalLength)
            }

            this.created = Util.dateToStringUTC(c!!.created)

            edit_mount!!.setText(c.mount)
            edit_manufacturer!!.setText(c.manufacturer)
            edit_model!!.setText(c.modelName)
            if (c.format < 0) c.format = 0
            spinner_format!!.position = c.format
            this.ssGrainSize = c.shutterSpeedGrainSize
            spinner_fastest_ss!!.setText(Util.doubleToStringShutterSpeed(c.fastestShutterSpeed!!))
            spinner_slowest_ss!!.setText(Util.doubleToStringShutterSpeed(c.slowestShutterSpeed!!))
            check_bulb_available!!.isChecked = c.bulbAvailable
            spinner_ev_grain_size!!.setSelection(c.evGrainSize - 1, false)
            spinner_ev_width!!.setSelection(c.evWidth - 1, false)
        }else if(savedInstanceState != null){ //復帰データあり
            if (type == 1) {
                fsAdapter!!.setCheckedState(savedInstanceState.getString("FStepsString"))
                //EditTextは勝手に復元するので処理不要
                //edit_lens_name_flc!!.setText(savedInstanceState.getString("fixedlens_name"))
                //edit_focal_length_flc!!.setText(savedInstanceState.getString("fixedlens_focal_length"))
            }

            this.created = savedInstanceState.getString("created")

            edit_mount!!.setText(savedInstanceState.getString("mount"))
            edit_manufacturer!!.setText(savedInstanceState.getString("manufacturer"))
            edit_model!!.setText(savedInstanceState.getString("model_name"))
            spinner_format!!.position = savedInstanceState.getInt("format_position")
            ssGrainSize = savedInstanceState.getInt("ss_grain_size")
            spinner_fastest_ss!!.setText(savedInstanceState.getString("fastest_ss"))
            spinner_slowest_ss!!.setText(savedInstanceState.getString("slowest_ss"))
            check_bulb_available!!.isChecked = savedInstanceState.getBoolean("bulb_available")
            spinner_ev_grain_size!!.setSelection(savedInstanceState.getInt("ev_grain_size") - 1, false)
            spinner_ev_width!!.setSelection(savedInstanceState.getInt("ev_width") - 1, false)
        }else{ //未入力開きたて
        }

        if (type == 1) gridview!!.adapter = fsAdapter
    }

    protected fun setEventListeners() {
        edit_mount!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        })

        edit_mount!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }

        edit_manufacturer!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        edit_model!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
                if (edit_model!!.text.toString() == "jenkinsushi") {
                    Toast.makeText(this@EditCameraActivity,
                            getString(R.string.google_maps_key),
                            Toast.LENGTH_LONG).show()
                }
            }
        })

        spinner_format!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        val rg = findViewById<RadioGroup>(R.id.radiogroup_ss_stop)
        rg.setOnCheckedChangeListener { group, checkedId ->
            refreshShutterSpeedSpinners()
            if(isResumed) isDirty = true
        }

        spinner_fastest_ss!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            doShutterSpeedValidation()
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }

        spinner_slowest_ss!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            doShutterSpeedValidation()
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }

        check_bulb_available!!.setOnCheckedChangeListener { buttonView, isChecked -> if(isResumed) isDirty = true }


        spinner_ev_grain_size!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(isResumed) isDirty = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if(isResumed) isDirty = true
            }
        }

        spinner_ev_width!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(isResumed) isDirty = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if(isResumed) isDirty = true
            }
        }

        edit_focal_length_flc!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        edit_lens_name_flc!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        gridview!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }
    }

    protected fun canSave(): Boolean {
        val cameraOk: Boolean
        val lensOk: Boolean
        //マウントが空でない
        //モデル名が空でない
        //シャッタースピードレンジが設定されていて内容に矛盾がない
        cameraOk = (type == 1 || type == 0 && edit_mount!!.text.length > 0) &&
                edit_model!!.text.length > 0 &&
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

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isResumed = false
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_camera)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        ssAdapterOne = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one, android.R.layout.simple_spinner_item)
        ssAdapterHalf = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_half, android.R.layout.simple_spinner_item)
        ssAdapterOneThird = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one_third, android.R.layout.simple_spinner_item)

        ssAdapterOne!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ssAdapterHalf!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ssAdapterOneThird!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner_fastest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
        spinner_slowest_ss!!.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)

        spinner_fastest_ss!!.setHelperText(getString(R.string.label_shutter_speed_fastest))
        spinner_slowest_ss!!.setHelperText(getString(R.string.label_shutter_speed_slowest))

        formatAdapter = ArrayAdapter.createFromResource(this, R.array.film_formats, android.R.layout.simple_spinner_item)
        formatAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_format!!.setAdapter<ArrayAdapter<CharSequence>>(formatAdapter)
        spinner_format!!.position = FilmFormat.FULL_FRAME.ordinal

        val manufacturer_adapter = getSuggestListPref("camera_manufacturer",
                R.array.camera_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        edit_manufacturer!!.setAdapter(manufacturer_adapter)

        val mount_adapter = getSuggestListPref("camera_mounts",
                R.array.camera_mounts,
                android.R.layout.simple_dropdown_item_1line)
        edit_mount!!.setAdapter(mount_adapter)

        val data = intent

        val dao = TrisquelDao(applicationContext)
        dao.connection()
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
        if(fsAdapter != null)
            outState.putString("FStepsString", fsAdapter!!.fStepsString)
        outState.putBoolean("isDirty", isDirty)
        outState.putString("created", this.created)
        outState.putString("mount", edit_mount!!.text.toString())
        outState.putString("manufacturer", edit_manufacturer!!.text.toString())
        outState.putString("model_name", edit_model!!.text.toString())
        outState.putString("mount", edit_mount!!.text.toString())
        outState.putInt("format_position", spinner_format!!.position)
        outState.putInt("ss_grain_size", ssGrainSize)
        outState.putString("fastest_ss", spinner_fastest_ss!!.text.toString())
        outState.putString("slowest_ss", spinner_slowest_ss!!.text.toString())
        outState.putBoolean("bulb_available", check_bulb_available!!.isChecked)
        outState.putInt("ev_grain_size", spinner_ev_grain_size!!.selectedItemPosition + 1)
        outState.putInt("ev_width", spinner_ev_width!!.selectedItemPosition + 1)
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
        if(newValue.isEmpty()) return
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
                val resultData = this.data
                setResult(Activity.RESULT_OK, resultData)
                saveSuggestListPref("camera_manufacturer",
                        R.array.camera_manufacturer, edit_manufacturer!!.text.toString())
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, edit_mount!!.text.toString())
                finish()
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            102 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                /* do nothing */
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, Intent())
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
                        R.array.camera_manufacturer, edit_manufacturer!!.text.toString())
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, edit_mount!!.text.toString())
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
