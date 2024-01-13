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
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.databinding.ActivityEditFilmRollBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
    private lateinit var binding: ActivityEditFilmRollBinding

    val data: Intent
        get() {
            val data = Intent()
            data.putExtra("id", id)
            data.putExtra("name", binding.editName.text.toString())
            data.putExtra("created", created)
            data.putExtra("camera", cameralist!![binding.spinnerCamera.position].id)
            data.putExtra("manufacturer", binding.editManufacturer.text.toString())
            data.putExtra("brand", binding.editBrand.text.toString())
            var iso: Int
            try {
                iso = Integer.parseInt(binding.editIso.text.toString())
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
            binding.editName.setText(f.name)
            if (f.camera.id > 0)
                binding.spinnerCamera.position = cadapter!!.getPosition(f.camera.id)
            binding.editManufacturer.setText(f.manufacturer)
            binding.editBrand.setText(f.brand)
            if (f.iso > 0)
                binding.editIso.setText(Integer.toString(f.iso))
        }else if(savedInstanceState != null){ //復帰データあり
            this.created = savedInstanceState.getString("created")
            binding.editName.setText(savedInstanceState.getString("name"))
            binding.spinnerCamera.position = savedInstanceState.getInt("camera_position")
            binding.editManufacturer.setText(savedInstanceState.getString("manufacturer"))
            binding.editBrand.setText(savedInstanceState.getString("brand"))
            binding.editIso.setText(savedInstanceState.getString("iso"))
        }else{ //新規データ開きたて
            val cameraid = data.getIntExtra("default_camera", -1)
            if (cameraid != -1)
                binding.spinnerCamera.position = cadapter!!.getPosition(cameraid)

            val film_manufacturer = data.getStringExtra("default_manufacturer") ?: ""
            if (film_manufacturer.isNotEmpty())
                binding.editManufacturer.setText(film_manufacturer)

            val film_brand = data.getStringExtra("default_brand") ?: ""
            if (film_brand.isNotEmpty())
                binding.editBrand.setText(film_brand)
        }
    }

    protected fun setEventListeners() {
        binding.editName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })
        val oldListener = binding.spinnerCamera.onItemClickListener// これやらないとgetPositionがおかしくなる
        binding.spinnerCamera.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
            oldListener.onItemClick(parent, view, position, id)
        }

        binding.spinnerCamera.setOnClickListener {
            if(binding.spinnerCamera.adapter.count == 0){
                val fragment = YesNoDialogFragment.Builder()
                        .build(REQCODE_ASK_CREATE_CAMERA)
                fragment.arguments?.putString("message", getString(R.string.msg_ask_create_camera))
                fragment.arguments?.putString("positive", getString(android.R.string.yes))
                fragment.arguments?.putString("negative", getString(android.R.string.no))
                fragment.showOn(this, "dialog")
            }
        }

        binding.editManufacturer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
                val brand_adapter = getSuggestListSubPref("film_brand",
                        binding.editManufacturer.text.toString(),
                        android.R.layout.simple_dropdown_item_1line)
                binding.editBrand.setAdapter(brand_adapter)
            }
        })

        binding.editBrand.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.editIso.addTextChangedListener(object : TextWatcher {
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
        return binding.editName.text.toString().length > 0 && binding.spinnerCamera.position >= 0
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditFilmRollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        val dao = TrisquelDao(applicationContext)
        dao.connection()

        updateCameraList(dao)

        val manufacturer_adapter = getSuggestListPref("film_manufacturer",
                R.array.film_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        binding.editManufacturer.setAdapter(manufacturer_adapter)

        val iso_adapter = ArrayAdapter.createFromResource(this, R.array.film_iso, android.R.layout.simple_dropdown_item_1line)

        binding.editIso.setAdapter(iso_adapter)
        binding.editIso.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b && binding.editIso.text.toString().isEmpty()) {
                val zoom = Pattern.compile(".*?(\\d++).*")
                val m = zoom.matcher(binding.editBrand.text.toString())
                if (m.find()) {
                    val suggestedISO = m.group(1)
                    binding.editIso.setText(suggestedISO)
                }
            }
        }

        val data = intent
        loadData(data, dao, savedInstanceState)
        dao.close()

        val brand_adapter = getSuggestListSubPref("film_brand",
                binding.editManufacturer.text.toString(),
                android.R.layout.simple_dropdown_item_1line)
        binding.editBrand.setAdapter(brand_adapter)

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
        binding.spinnerCamera.setAdapter<CameraAdapter>(cadapter)
        if(cameralist!!.size == 0){
            binding.spinnerCamera.error = getString(R.string.error_nocamera)
        }else{
            binding.spinnerCamera.error = null
            if(cameralist!!.size == 1){
                binding.spinnerCamera.position = 0
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putInt("camera_position", binding.spinnerCamera.position)
        outState.putString("created", this.created)
        outState.putString("name", binding.editName.text.toString())
        outState.putString("manufacturer", binding.editManufacturer.text.toString())
        outState.putString("brand", binding.editBrand.text.toString())
        outState.putString("iso", binding.editIso.text.toString())
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
                        R.array.film_manufacturer, binding.editManufacturer.text.toString())
                saveSuggestListSubPref("film_brand",
                        binding.editManufacturer.text.toString(),
                        binding.editBrand.text.toString())
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
                    val c = bundle!!.getParcelable<CameraSpec>("cameraspec")!!
                    val dao = TrisquelDao(this)
                    dao.connection()
                    c.id = dao.addCamera(c).toInt()
                    updateCameraList(dao)
                    dao.close()
                    binding.spinnerCamera.clearFocus()
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
                        R.array.film_manufacturer, binding.editManufacturer.text.toString())
                saveSuggestListSubPref("film_brand",
                        binding.editManufacturer.text.toString(),
                        binding.editBrand.text.toString())
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
