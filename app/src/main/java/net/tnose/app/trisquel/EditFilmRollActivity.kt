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
import android.widget.Toast
import com.rengwuxian.materialedittext.MaterialEditText
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

class EditFilmRollActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = 0
    private var created: String? = null
    private var cameralist: ArrayList<CameraSpec>? = null
    private var cadapter: CameraAdapter? = null
    private var editBrand: ImmediateAutoCompleteTextView? = null
    private var editManufacturer: ImmediateAutoCompleteTextView? = null
    private var editIso: ImmediateAutoCompleteTextView? = null
    private var cameraSpinner: MaterialBetterSpinner? = null
    private var editName: MaterialEditText? = null
    private var isDirty: Boolean = false

    val data: Intent
        get() {
            val data = Intent()
            data.putExtra("id", id)
            data.putExtra("name", editName!!.text.toString())
            data.putExtra("created", created)
            data.putExtra("camera", cameralist!![cameraSpinner!!.position].id)
            data.putExtra("manufacturer", editManufacturer!!.text.toString())
            data.putExtra("brand", editBrand!!.text.toString())
            var iso: Int
            try {
                iso = Integer.parseInt(editIso!!.text.toString())
            } catch (e: NumberFormatException) {
                iso = 0
            }

            data.putExtra("iso", iso)
            return data
        }

    protected fun findViews() {
        cameraSpinner = findViewById(R.id.spinner_camera)
        editManufacturer = findViewById(R.id.edit_manufacturer)
        editBrand = findViewById(R.id.edit_brand)
        editIso = findViewById(R.id.edit_iso)
        editName = findViewById(R.id.edit_name)
    }

    protected fun loadData(data: Intent, dao: TrisquelDao) {
        val id = data.getIntExtra("id", -1)
        this.id = id
        if (id <= 0) return
        setTitle(R.string.title_activity_edit_filmroll)
        val f = dao.getFilmRoll(id)
        this.created = Util.dateToStringUTC(f!!.created)

        editName!!.setText(f.name)

        if (f.camera.id > 0) {
            cameraSpinner!!.position = cadapter!!.getPosition(f.camera.id)
        }

        editManufacturer!!.setText(f.manufacturer)
        editBrand!!.setText(f.brand)
        if (f.iso > 0) editIso!!.setText(Integer.toString(f.iso))
    }

    protected fun setEventListeners() {
        editName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })
        val oldListener = cameraSpinner!!.onItemClickListener// これやらないとgetPositionがおかしくなる
        cameraSpinner!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            isDirty = true
            oldListener.onItemClick(parent, view, position, id)
        }

        editManufacturer!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
                val brand_adapter = getSuggestListSubPref("film_brand",
                        editManufacturer!!.text.toString(),
                        android.R.layout.simple_dropdown_item_1line)
                editBrand!!.setAdapter(brand_adapter)
            }
        })

        editBrand!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })

        editIso!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                isDirty = true
            }
        })
    }

    protected fun canSave(): Boolean {
        return editName!!.text.toString().length > 0 && cameraSpinner!!.position >= 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_film_roll)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        findViews()

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        cameralist = dao.allCameras

        cadapter = CameraAdapter(this, android.R.layout.simple_spinner_item, cameralist!!)
        cadapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cameraSpinner!!.setAdapter<CameraAdapter>(cadapter)

        val manufacturer_adapter = getSuggestListPref("film_manufacturer",
                R.array.film_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        editManufacturer!!.setAdapter(manufacturer_adapter)

        val iso_adapter = ArrayAdapter.createFromResource(this, R.array.film_iso, android.R.layout.simple_dropdown_item_1line)

        editIso!!.setAdapter(iso_adapter)
        editIso!!.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b && editIso!!.text.toString().isEmpty()) {
                val zoom = Pattern.compile(".*?(\\d++).*")
                val m = zoom.matcher(editBrand!!.text.toString())
                if (m.find()) {
                    val suggestedISO = m.group(1)
                    editIso!!.setText(suggestedISO)
                }
            }
        }

        val data = intent
        if (data != null) {
            loadData(data, dao)
        } else {
            this.id = -1
        }
        dao.close()

        if (cameralist!!.size == 0) {
            Toast.makeText(this, getString(R.string.error_please_reg_cam), Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
        }

        val brand_adapter = getSuggestListSubPref("film_brand",
                editManufacturer!!.text.toString(),
                android.R.layout.simple_dropdown_item_1line)
        editBrand!!.setAdapter(brand_adapter)

        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save, menu)
        menu.findItem(R.id.menu_save).isEnabled = canSave()
        return true
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


    //dead copy from EditCameraActivity
    protected fun getSuggestListSubPref(parentkey: String, subkey: String, resource: Int): ArrayAdapter<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        val strArray = ArrayList<String>()
        try {
            val obj = JSONObject(prefstr)
            val array = obj.getJSONArray(subkey) ?: return ArrayAdapter(this, resource, strArray)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        return ArrayAdapter(this, resource, strArray)
    }

    protected fun saveSuggestListSubPref(parentkey: String, subkey: String, newValue: String) {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(parentkey, "{}")
        val strArray = ArrayList<String>()
        var obj: JSONObject? = null
        try {
            val array: JSONArray
            obj = JSONObject(prefstr)
            if (obj.isNull(subkey)) {
                array = JSONArray()
            } else {
                array = obj.getJSONArray(subkey)
            }
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
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

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            101 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val resultData = data
                saveSuggestListPref("film_manufacturer",
                        R.array.film_manufacturer, editManufacturer!!.text.toString())
                saveSuggestListSubPref("film_brand",
                        editManufacturer!!.text.toString(),
                        editBrand!!.text.toString())
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
                saveSuggestListPref("film_manufacturer",
                        R.array.film_manufacturer, editManufacturer!!.text.toString())
                saveSuggestListSubPref("film_brand",
                        editManufacturer!!.text.toString(),
                        editBrand!!.text.toString())
                setResult(Activity.RESULT_OK, data)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        Log.d("onBackPressed", "EditFilmRollActivity")
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
