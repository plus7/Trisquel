package net.tnose.app.trisquel

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_edit_film_roll.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

class EditFilmRollActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    internal val REQCODE_ASK_CREATE_CAMERA = 103
    internal val REQCODE_ADD_CAMERA = 104
    private var id: Int = 0
    private var created: String? = null
    private var cameralist: ArrayList<CameraSpec>? = null
    private var cadapter: CameraAdapter? = null
    private var isResumed: Boolean = false
    private var isDirty: Boolean = false

    val data: Intent
        get() {
            val data = Intent()
            data.putExtra("id", id)
            data.putExtra("name", edit_name!!.text.toString())
            data.putExtra("created", created)
            data.putExtra("camera", cameralist!![spinner_camera!!.position].id)
            data.putExtra("manufacturer", edit_manufacturer!!.text.toString())
            data.putExtra("brand", edit_brand!!.text.toString())
            var iso: Int
            try {
                iso = Integer.parseInt(edit_iso!!.text.toString())
            } catch (e: NumberFormatException) {
                iso = 0
            }
            data.putExtra("iso", iso)
            return data
        }

    protected fun loadData(data: Intent, dao: TrisquelDao, savedInstanceState: Bundle?) {
        val id = data.getIntExtra("id", -1)
        this.id = id
        if(id < 0)
            setTitle(R.string.title_activity_reg_filmroll)
        else
            setTitle(R.string.title_activity_edit_filmroll)

        if(id >= 0 && savedInstanceState == null) { //既存データを開きたて
            val f = dao.getFilmRoll(id)
            this.created = Util.dateToStringUTC(f!!.created)
            edit_name!!.setText(f.name)
            if (f.camera.id > 0)
                spinner_camera!!.position = cadapter!!.getPosition(f.camera.id)
            edit_manufacturer!!.setText(f.manufacturer)
            edit_brand!!.setText(f.brand)
            if (f.iso > 0)
                edit_iso!!.setText(Integer.toString(f.iso))
        }else if(savedInstanceState != null){ //復帰データあり
            this.created = savedInstanceState.getString("created")
            edit_name!!.setText(savedInstanceState.getString("name"))
            spinner_camera!!.position = savedInstanceState.getInt("camera_position")
            edit_manufacturer!!.setText(savedInstanceState.getString("manufacturer"))
            edit_brand!!.setText(savedInstanceState.getString("brand"))
            edit_iso!!.setText(savedInstanceState.getString("iso"))
        }else{ //新規データ開きたて
            val cameraid = data.getIntExtra("default_camera", -1)
            if (cameraid != -1)
                spinner_camera!!.position = cadapter!!.getPosition(cameraid)

            val film_manufacturer = data.getStringExtra("default_manufacturer") ?: ""
            if (film_manufacturer.isNotEmpty())
                edit_manufacturer!!.setText(film_manufacturer)

            val film_brand = data.getStringExtra("default_brand") ?: ""
            if (film_brand.isNotEmpty())
                edit_brand!!.setText(film_brand)
        }
    }

    protected fun setEventListeners() {
        edit_name!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })
        val oldListener = spinner_camera!!.onItemClickListener// これやらないとgetPositionがおかしくなる
        spinner_camera!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
            oldListener.onItemClick(parent, view, position, id)
        }

        spinner_camera.setOnClickListener {
            if(spinner_camera.adapter.count == 0){
                val fragment = YesNoDialogFragment.Builder()
                        .build(REQCODE_ASK_CREATE_CAMERA)
                fragment.arguments?.putString("message", getString(R.string.msg_ask_create_camera))
                fragment.arguments?.putString("positive", getString(android.R.string.yes))
                fragment.arguments?.putString("negative", getString(android.R.string.no))
                fragment.showOn(this, "dialog")
            }
        }

        edit_manufacturer!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
                val brand_adapter = getSuggestListSubPref("film_brand",
                        edit_manufacturer!!.text.toString(),
                        android.R.layout.simple_dropdown_item_1line)
                edit_brand!!.setAdapter(brand_adapter)
            }
        })

        edit_brand!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        edit_iso!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })
    }

    protected fun canSave(): Boolean {
        return edit_name!!.text.toString().length > 0 && spinner_camera!!.position >= 0
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_film_roll)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        val dao = TrisquelDao(applicationContext)
        dao.connection()

        updateCameraList(dao)

        val manufacturer_adapter = getSuggestListPref("film_manufacturer",
                R.array.film_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        edit_manufacturer!!.setAdapter(manufacturer_adapter)

        val iso_adapter = ArrayAdapter.createFromResource(this, R.array.film_iso, android.R.layout.simple_dropdown_item_1line)

        edit_iso!!.setAdapter(iso_adapter)
        edit_iso!!.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b && edit_iso!!.text.toString().isEmpty()) {
                val zoom = Pattern.compile(".*?(\\d++).*")
                val m = zoom.matcher(edit_brand!!.text.toString())
                if (m.find()) {
                    val suggestedISO = m.group(1)
                    edit_iso!!.setText(suggestedISO)
                }
            }
        }

        val data = intent
        loadData(data, dao, savedInstanceState)
        dao.close()

        val brand_adapter = getSuggestListSubPref("film_brand",
                edit_manufacturer!!.text.toString(),
                android.R.layout.simple_dropdown_item_1line)
        edit_brand!!.setAdapter(brand_adapter)

        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    protected fun updateCameraList(dao: TrisquelDao) {
        cameralist = dao.allCameras
        cadapter = CameraAdapter(this, android.R.layout.simple_spinner_item, cameralist!!)
        cadapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_camera!!.setAdapter<CameraAdapter>(cadapter)
        if(cameralist!!.size == 0){
            spinner_camera.error = getString(R.string.error_nocamera)
        }else{
            spinner_camera.error = null
            if(cameralist!!.size == 1){
                spinner_camera.position = 0
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putInt("camera_position", spinner_camera!!.position)
        outState.putString("created", this.created)
        outState.putString("name", edit_name!!.text.toString())
        outState.putString("manufacturer", edit_manufacturer!!.text.toString())
        outState.putString("brand", edit_brand!!.text.toString())
        outState.putString("iso", edit_iso!!.text.toString())
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
                val resultData = this.data
                saveSuggestListPref("film_manufacturer",
                        R.array.film_manufacturer, edit_manufacturer!!.text.toString())
                saveSuggestListSubPref("film_brand",
                        edit_manufacturer!!.text.toString(),
                        edit_brand!!.text.toString())
                setResult(Activity.RESULT_OK, resultData)
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
            REQCODE_ASK_CREATE_CAMERA -> if(resultCode == DialogInterface.BUTTON_POSITIVE){
                intent = Intent(application, EditCameraActivity::class.java)
                startActivityForResult(intent, REQCODE_ADD_CAMERA)
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQCODE_ADD_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                if(data != null) {
                    val bundle = data.extras
                    val c = bundle!!.getParcelable<CameraSpec>("cameraspec")
                    val dao = TrisquelDao(this)
                    dao.connection()
                    c.id = dao.addCamera(c).toInt()
                    updateCameraList(dao)
                    dao.close()
                    spinner_camera.clearFocus()
                    invalidateOptionsMenu()
                }
            }
        }
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
                saveSuggestListPref("film_manufacturer",
                        R.array.film_manufacturer, edit_manufacturer!!.text.toString())
                saveSuggestListSubPref("film_brand",
                        edit_manufacturer!!.text.toString(),
                        edit_brand!!.text.toString())
                setResult(Activity.RESULT_OK, data)
                finish()
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
}
