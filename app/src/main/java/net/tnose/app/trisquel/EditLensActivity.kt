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
import android.widget.GridView
import com.rengwuxian.materialedittext.MaterialEditText
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import java.util.regex.Pattern

class EditLensActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = 0
    private var created: String? = null
    private var editMount: ImmediateAutoCompleteTextView? = null
    private var editManufacturer: ImmediateAutoCompleteTextView? = null
    private var editModelName: MaterialEditText? = null
    private var editFocalLength: MaterialEditText? = null
    private var fStopGridView: GridView? = null
    private var fsAdapter: FStepAdapter? = null
    private var isDirty: Boolean = false

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

    private val fStepsString: String
        get() {
            Log.d("getFStepsString", fsAdapter!!.fStepsString)
            return fsAdapter!!.fStepsString
        }

    val data: Intent
        get() {
            val data = Intent()
            data.putExtra("id", id)
            data.putExtra("created", created)
            data.putExtra("mount", editMount!!.text.toString())
            data.putExtra("manufacturer", editManufacturer!!.text.toString())
            data.putExtra("model_name", editModelName!!.text.toString())
            data.putExtra("focal_length", editFocalLength!!.text.toString())
            data.putExtra("f_steps", fStepsString)
            return data
        }

    protected fun findViews() {
        editManufacturer = findViewById(R.id.edit_manufacturer)
        editMount = findViewById(R.id.edit_mount)
        editFocalLength = findViewById(R.id.edit_focal_length)
        editModelName = findViewById(R.id.edit_model)
        fStopGridView = findViewById(R.id.fstop_gridview)
    }

    protected fun loadData(data: Intent, savedInstanceState: Bundle?) {
        val id = data.getIntExtra("id", -1)
        this.id = id
        fsAdapter = FStepAdapter(this)
        if (id <= 0) {
            if (savedInstanceState != null) {
                fsAdapter!!.setCheckedState(savedInstanceState.getString("FStepsString"))
            }
            fStopGridView!!.adapter = fsAdapter
            return
        }
        setTitle(R.string.title_activity_edit_lens)
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val l = dao.getLens(id)
        dao.close()

        this.created = Util.dateToStringUTC(l!!.created)

        editManufacturer!!.setText(l.manufacturer)
        editMount!!.setText(l.mount)
        editModelName!!.setText(l.modelName)
        editFocalLength!!.setText(l.focalLength)

        if (savedInstanceState != null) {
            fsAdapter!!.setCheckedState(savedInstanceState.getString("FStepsString"))
        } else {
            fsAdapter!!.setCheckedState(l.fSteps)
        }
        fStopGridView!!.adapter = fsAdapter
    }

    protected fun setEventListeners() {
        editMount!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })

        editMount!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> invalidateOptionsMenu() }

        editManufacturer!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })

        editModelName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })

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

        editFocalLength!!.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b && editFocalLength!!.text.toString().isEmpty()) {
                val zoom = Pattern.compile(".*?(\\d++)-(\\d++)mm.*")
                var m = zoom.matcher(editModelName!!.text.toString())
                if (m.find()) {
                    val suggestedWideFocalLength = m.group(1)
                    val suggestedTeleFocalLength = m.group(2)
                    editFocalLength!!.setText("$suggestedWideFocalLength-$suggestedTeleFocalLength")
                } else {
                    val prime = Pattern.compile(".*?(\\d++)mm.*")
                    m = prime.matcher(editModelName!!.text.toString())
                    if (m.find()) {
                        val suggestedFocalLength = m.group(1)
                        editFocalLength!!.setText(suggestedFocalLength)
                    }
                }
            }
        }

        fStopGridView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            isDirty = true
        }
    }

    protected fun canSave(): Boolean {
        //マウントとモデル名とF値リストは必須。
        return editMount!!.text.length > 0 && editModelName!!.text.length > 0 && focalLengthOk
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_lens)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        findViews()

        val manufacturer_adapter = getSuggestListPref("lens_manufacturer",
                R.array.lens_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        editManufacturer!!.setAdapter(manufacturer_adapter)

        val mount_adapter = getSuggestListPref("camera_mounts",
                R.array.camera_mounts,
                android.R.layout.simple_dropdown_item_1line)
        editMount!!.setAdapter(mount_adapter)

        editFocalLength!!.setHelperText(getString(R.string.hint_zoom))

        val data = intent
        if (data != null) {
            loadData(data, savedInstanceState)
        } else {
            this.id = -1
        }

        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("FStepsString", fStepsString)
        outState.putBoolean("isDirty", isDirty)
        super.onSaveInstanceState(outState)
    }

    //dead copy from EditCameraActivity
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
                saveSuggestListPref("lens_manufacturer",
                        R.array.lens_manufacturer, editManufacturer!!.text.toString())
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
                saveSuggestListPref("lens_manufacturer",
                        R.array.lens_manufacturer, editManufacturer!!.text.toString())
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
        Log.d("onBackPressed", "EditLensActivity")
        if (!isDirty) {
            setResult(Activity.RESULT_CANCELED, null)
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
