package net.tnose.app.trisquel

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.databinding.ActivityEditCameraBinding
import org.json.JSONArray
import org.json.JSONException
import java.util.Date
import java.util.regex.Pattern

class EditCameraActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    val DIALOG_CUSTOM_SHUTTER_SPEEDS = 100
    private var id: Int = -1
    private var type: Int = 0
    private var created: String = ""
    private var formatAdapter: ArrayAdapter<CharSequence>? = null
    private var ssAdapterOne: ArrayAdapter<CharSequence>? = null
    private var ssAdapterHalf: ArrayAdapter<CharSequence>? = null
    private var ssAdapterOneThird: ArrayAdapter<CharSequence>? = null
    private var ssCustomSteps: ArrayList<String> = arrayListOf()
    private var fsAdapter: FStepAdapter? = null
    private var isDirty: Boolean = false
    private var isResumed: Boolean = false
    private var userIsInteracting = false
    private var previousCheckedSsSteps = -1
    private lateinit var binding: ActivityEditCameraBinding

    private val shutterSpeedRangeOk: Boolean
        get() {
            val fastestSS = Util.stringToDoubleShutterSpeed(binding.spinnerFastestSs.text.toString())
            val slowestSS = Util.stringToDoubleShutterSpeed(binding.spinnerSlowestSs.text.toString())
            return if (fastestSS == 0.0 || slowestSS == 0.0) {
                false
            } else
                fastestSS <= slowestSS
        }

    private val focalLengthOk: Boolean
        get() {
            if (!binding.editFocalLengthFlc.text.toString().isEmpty()) {
                val zoom = Pattern.compile("(\\d++)-(\\d++)")
                var m = zoom.matcher(binding.editFocalLengthFlc.text.toString())
                if (m.find()) {
                    return true
                }

                val prime = Pattern.compile("(\\d++)")
                m = prime.matcher(binding.editFocalLengthFlc.text.toString())
                if (m.find()) {
                    return true
                }
            }
            return false
        }

    protected val isDataChanged: Boolean
        get() = false

    private var ssGrainSize: Int
        get() = if (binding.radioStopOne.isChecked) {
            1
        } else if (binding.radioStopHalf.isChecked) {
            2
        } else if (binding.radioStopOneThird.isChecked){
            3
        } else { // custom
            0
        }
        set(value) =
            when (value) {
                1 -> binding.radioStopOne.isChecked = true
                2 -> binding.radioStopHalf.isChecked = true
                3 -> binding.radioStopOneThird.isChecked = true
                else -> binding.radioStopCustom.isChecked = true
            }

    /*
                  "shutter_speeds"はまだ対応しない
                */
    val data: Intent
        get() {
            val data = Intent()
            val c = CameraSpec(id, type,
                    if(created.isNotEmpty()) created else Util.dateToStringUTC(Date()),
                    Util.dateToStringUTC(Date()),
                    binding.editMount.text.toString(),
                    binding.editManufacturer.text.toString(),
                    binding.editModel.text.toString(),
                    binding.spinnerFormat.position,
                    ssGrainSize,
                    Util.stringToDoubleShutterSpeed(binding.spinnerFastestSs.text.toString()),
                    Util.stringToDoubleShutterSpeed(binding.spinnerSlowestSs.text.toString()),
                    binding.checkBulbAvailable.isChecked,
                    ssCustomSteps.map{ Util.stringToDoubleShutterSpeed(it).toString() }.joinToString(","),
                    binding.spinnerEvGrainSize.selectedItemPosition + 1,
                    binding.spinnerEvWidth.selectedItemPosition + 1)
            data.putExtra("cameraspec", c)
            if (type == 1) {
                val l = LensSpec(-1, "", id, binding.editManufacturer.text.toString(),
                        binding.editLensNameFlc.text.toString(),
                        binding.editFocalLengthFlc.text.toString(),
                        fsAdapter!!.fStepsString)
                data.putExtra("fixed_lens", l)
            }
            return data
        }

    private fun refreshShutterSpeedSpinners() {
        if (binding.radioStopOne.isChecked) {
            binding.spinnerFastestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
            binding.spinnerSlowestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
        } else if (binding.radioStopHalf.isChecked) {
            binding.spinnerFastestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterHalf)
            binding.spinnerSlowestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterHalf)
        } else if (binding.radioStopOneThird.isChecked) {
            binding.spinnerFastestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOneThird)
            binding.spinnerSlowestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOneThird)
        } else {
            val customSsAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ssCustomSteps)
            binding.spinnerFastestSs.setAdapter(customSsAdapter)
            binding.spinnerSlowestSs.setAdapter(customSsAdapter)
        }
    }

    private fun doShutterSpeedValidation(): Boolean {
        var ret = true
        val fastestSS = Util.stringToDoubleShutterSpeed(binding.spinnerFastestSs.text.toString())
        val slowestSS = Util.stringToDoubleShutterSpeed(binding.spinnerSlowestSs.text.toString())
        if (fastestSS == 0.0 || slowestSS == 0.0) { //未設定なだけなのでエラー表示にはしない
            binding.spinnerFastestSs.error = null
            binding.spinnerSlowestSs.error = null
        } else if (fastestSS > slowestSS) {
            binding.spinnerFastestSs.error = getString(R.string.error_ss_inconsistent)
            binding.spinnerSlowestSs.error = ""
            ret = false
        } else {
            binding.spinnerFastestSs.error = null
            binding.spinnerSlowestSs.error = null
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

            binding.editMount.visibility = View.GONE
            binding.layoutFlc.visibility = View.VISIBLE
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
                binding.editLensNameFlc.setText(l.modelName)
                binding.editFocalLengthFlc.setText(l.focalLength)
            }

            this.created = Util.dateToStringUTC(c!!.created)

            binding.editMount.setText(c.mount)
            binding.editManufacturer.setText(c.manufacturer)
            binding.editModel.setText(c.modelName)
            if (c.format < 0) c.format = 0
            binding.spinnerFormat.position = c.format
            ssGrainSize = c.shutterSpeedGrainSize
            previousCheckedSsSteps = ssGrainSize
            ssCustomSteps = ArrayList(c.shutterSpeedSteps.map { Util.doubleToStringShutterSpeed(it) })
            binding.spinnerFastestSs.setText(Util.doubleToStringShutterSpeed(c.fastestShutterSpeed!!))
            binding.spinnerSlowestSs.setText(Util.doubleToStringShutterSpeed(c.slowestShutterSpeed!!))
            binding.checkBulbAvailable.isChecked = c.bulbAvailable
            binding.spinnerEvGrainSize.setSelection(c.evGrainSize - 1, false)
            binding.spinnerEvWidth.setSelection(c.evWidth - 1, false)
        }else if(savedInstanceState != null){ //復帰データあり
            if (type == 1) {
                fsAdapter!!.setCheckedState(savedInstanceState.getString("FStepsString")?:"")
                //EditTextは勝手に復元するので処理不要
                //binding.editLensNameFlc.setText(savedInstanceState.getString("fixedlens_name"))
                //binding.editFocalLengthFlc.setText(savedInstanceState.getString("fixedlens_focal_length"))
            }

            this.created = savedInstanceState.getString("created")?:""

            binding.editMount.setText(savedInstanceState.getString("mount"))
            binding.editManufacturer.setText(savedInstanceState.getString("manufacturer"))
            binding.editModel.setText(savedInstanceState.getString("model_name"))
            binding.spinnerFormat.position = savedInstanceState.getInt("format_position")
            ssGrainSize = savedInstanceState.getInt("ss_grain_size")
            previousCheckedSsSteps = savedInstanceState.getInt("previous_checked_ss_steps")
            ssCustomSteps = savedInstanceState.getStringArrayList("ss_custom_steps") ?: arrayListOf()
            binding.spinnerFastestSs.setText(savedInstanceState.getString("fastest_ss"))
            binding.spinnerSlowestSs.setText(savedInstanceState.getString("slowest_ss"))
            binding.checkBulbAvailable.isChecked = savedInstanceState.getBoolean("bulb_available")
            binding.spinnerEvGrainSize.setSelection(savedInstanceState.getInt("ev_grain_size") - 1, false)
            binding.spinnerEvWidth.setSelection(savedInstanceState.getInt("ev_width") - 1, false)
        }else{ //未入力開きたて
            this.created = ""
            previousCheckedSsSteps = 1
        }

        if (type == 1) binding.gridview.adapter = fsAdapter
    }

    protected fun setEventListeners() {
        binding.editMount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        })

        binding.editMount.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }

        binding.editManufacturer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        binding.editModel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
                if (binding.editModel.text.toString() == "jenkinsushi") {
                    Toast.makeText(this@EditCameraActivity,
                            getString(R.string.google_maps_key),
                            Toast.LENGTH_LONG).show()
                }
            }
        })

        binding.spinnerFormat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
            }
        })

        binding.radioStopCustom.setOnClickListener {
            if(previousCheckedSsSteps == 0){
                val fragment = ShutterSpeedCustomizeDialogFragment.Builder()
                        .build(DIALOG_CUSTOM_SHUTTER_SPEEDS)
                //fragment.arguments?.putInt("id", item.id)
                fragment.arguments?.putString("title", getString(R.string.title_dialog_custom_ss))
                fragment.arguments?.putString("message", getString(R.string.msg_dialog_custom_ss))
                val default_value = if(ssCustomSteps.size == 0){
                    resources.getStringArray(R.array.shutter_speeds_one).joinToString("\n")
                }else{
                    ssCustomSteps.joinToString("\n")
                }
                fragment.arguments?.putString("default_value", default_value)
                fragment.showOn(this, "dialog")
            }
        }

        val rg = findViewById<RadioGroup>(R.id.radiogroup_ss_stop)
        rg.setOnCheckedChangeListener { group, checkedId ->
            if(checkedId == R.id.radio_stop_custom){
                val fragment = ShutterSpeedCustomizeDialogFragment.Builder()
                        .build(DIALOG_CUSTOM_SHUTTER_SPEEDS)
                //fragment.arguments?.putInt("id", item.id)
                fragment.arguments?.putString("title", getString(R.string.title_dialog_custom_ss))
                fragment.arguments?.putString("message", getString(R.string.msg_dialog_custom_ss))
                val default_value = if(ssCustomSteps.size == 0){
                    resources.getStringArray(R.array.shutter_speeds_one).joinToString("\n")
                }else{
                    ssCustomSteps.joinToString("\n")
                }
                fragment.arguments?.putString("default_value", default_value)
                fragment.showOn(this, "dialog")
            } else {
                previousCheckedSsSteps = ssGrainSize
                refreshShutterSpeedSpinners()
                if (isResumed) isDirty = true
            }
        }

        binding.spinnerFastestSs.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            doShutterSpeedValidation()
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }

        binding.spinnerSlowestSs.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            doShutterSpeedValidation()
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }

        binding.checkBulbAvailable.setOnCheckedChangeListener { buttonView, isChecked -> if(isResumed) isDirty = true }


        binding.spinnerEvGrainSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(isResumed) isDirty = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if(isResumed) isDirty = true
            }
        }

        binding.spinnerEvWidth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(isResumed) isDirty = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if(isResumed) isDirty = true
            }
        }

        binding.editFocalLengthFlc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.editLensNameFlc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.gridview.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
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
        cameraOk = (type == 1 || type == 0 && binding.editMount.text.isNotEmpty()) &&
                (binding.editModel.text?.isNotEmpty() ?: false) &&
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
        binding = ActivityEditCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        ssAdapterOne = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one, android.R.layout.simple_spinner_item)
        ssAdapterHalf = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_half, android.R.layout.simple_spinner_item)
        ssAdapterOneThird = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one_third, android.R.layout.simple_spinner_item)

        ssAdapterOne!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ssAdapterHalf!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ssAdapterOneThird!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerFastestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)
        binding.spinnerSlowestSs.setAdapter<ArrayAdapter<CharSequence>>(ssAdapterOne)

        binding.spinnerFastestSs.setHelperText(getString(R.string.label_shutter_speed_fastest))
        binding.spinnerSlowestSs.setHelperText(getString(R.string.label_shutter_speed_slowest))

        formatAdapter = ArrayAdapter.createFromResource(this, R.array.film_formats, android.R.layout.simple_spinner_item)
        formatAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFormat.setAdapter<ArrayAdapter<CharSequence>>(formatAdapter)
        binding.spinnerFormat.position = FilmFormat.FULL_FRAME.ordinal

        val manufacturer_adapter = getSuggestListPref("camera_manufacturer",
                R.array.camera_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        binding.editManufacturer.setAdapter(manufacturer_adapter)

        val mount_adapter = getSuggestListPref("camera_mounts",
                R.array.camera_mounts,
                android.R.layout.simple_dropdown_item_1line)
        binding.editMount.setAdapter(mount_adapter)

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
        outState.putString("mount", binding.editMount.text.toString())
        outState.putString("manufacturer", binding.editManufacturer.text.toString())
        outState.putString("model_name", binding.editModel.text.toString())
        outState.putInt("format_position", binding.spinnerFormat.position)
        outState.putInt("ss_grain_size", ssGrainSize)
        outState.putInt("previous_checked_ss_steps", previousCheckedSsSteps)
        outState.putStringArrayList("ss_custom_steps", ssCustomSteps)
        outState.putString("fastest_ss", binding.spinnerFastestSs.text.toString())
        outState.putString("slowest_ss", binding.spinnerSlowestSs.text.toString())
        outState.putBoolean("bulb_available", binding.checkBulbAvailable.isChecked)
        outState.putInt("ev_grain_size", binding.spinnerEvGrainSize.selectedItemPosition + 1)
        outState.putInt("ev_width", binding.spinnerEvWidth.selectedItemPosition + 1)
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
                        R.array.camera_manufacturer, binding.editManufacturer.text.toString())
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, binding.editMount.text.toString())
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
            DIALOG_CUSTOM_SHUTTER_SPEEDS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val list = ArrayList(data.getStringExtra("value")!!.split("\n").sortedBy { Util.stringToDoubleShutterSpeed(it) })
                ssCustomSteps = list
                refreshShutterSpeedSpinners()
                previousCheckedSsSteps = 0
                if (isResumed) isDirty = true
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                ssGrainSize = previousCheckedSsSteps
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        when(requestCode){
            DIALOG_CUSTOM_SHUTTER_SPEEDS -> ssGrainSize = previousCheckedSsSteps
        }
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
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
                setResult(Activity.RESULT_OK, data)
                saveSuggestListPref("camera_manufacturer",
                        R.array.camera_manufacturer, binding.editManufacturer.text.toString())
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, binding.editMount.text.toString())
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
}
