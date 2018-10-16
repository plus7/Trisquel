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
import com.rengwuxian.materialedittext.MaterialEditText
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import org.json.JSONArray
import org.json.JSONException

class EditAccessoryActivity : AppCompatActivity(), AbstractDialogFragment.Callback  {
    var id: Int = -1
    var created: String = ""
    //var type: Int = 0
    var name: String = ""
    var mount: String = ""
    var focal_length_factor: Double = 0.0

    var spAccessoryType: MaterialBetterSpinner? = null
        get() = findViewById(R.id.spinner_accessory_type)
    var editName: MaterialEditText? = null
        get() = findViewById(R.id.edit_name)
    var spMount: ImmediateAutoCompleteTextView? = null
        get() = findViewById(R.id.spinner_mount)
    var editFlFactor: MaterialEditText? = null
        get() = findViewById(R.id.edit_fl_factor)
    var isDirty: Boolean = false

    fun setAdapters(){
        val typeArray = ArrayList<String>()
        typeArray.add(getString(R.string.label_accessory_filter))
        typeArray.add(getString(R.string.label_accessory_tc))
        typeArray.add(getString(R.string.label_accessory_wc))
        typeArray.add(getString(R.string.label_accessory_ext_tube))
        typeArray.add(getString(R.string.label_accessory_unknown))

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, typeArray)
        spAccessoryType?.setAdapter(adapter)

        spMount?.setAdapter(getSuggestListPref("camera_mounts", R.array.camera_mounts, android.R.layout.simple_spinner_item))
    }

    fun loadData(data: Intent, dao: TrisquelDao){
        val id = data.getIntExtra("id", -1)
        this.id = id
        if(id < 0)
            setTitle(R.string.title_activity_add_accessory)
        else
            setTitle(R.string.title_activity_edit_accessory)

        //val type = data.getIntExtra("type", 0)
        //this.type = type

        if(id >= 0) {
            val a = dao.getAccessory(id)
            this.created = Util.dateToStringUTC(a!!.created) //なんか変な仕組みな気がしてきたぞ
            editName?.setText(a.name)

            when(a.type){
                Accessory.ACCESSORY_TELE_CONVERTER,
                Accessory.ACCESSORY_WIDE_CONVERTER -> {
                    spMount?.setText(a.mount)
                    editFlFactor?.setText(a.focal_length_factor.toString())
                }
                Accessory.ACCESSORY_EXT_TUBE -> {
                    spMount?.setText(a.mount)
                }
            }
            spAccessoryType?.position = (a.type + 4) % 5
            refreshUIforType(a.type)
        }else{
            refreshUIforType(Accessory.ACCESSORY_UNKNOWN)
        }

    }

    fun refreshUIforType(type: Int){
        when(type){
            Accessory.ACCESSORY_FILTER, Accessory.ACCESSORY_UNKNOWN, -1 -> {
                spMount?.visibility = View.GONE
                editFlFactor?.visibility = View.GONE
            }
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                spMount?.visibility = View.VISIBLE
                editFlFactor?.visibility = View.VISIBLE
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                spMount?.visibility = View.VISIBLE
                editFlFactor?.visibility = View.GONE
            }
        }
        invalidateFocalLengthFactor()
    }

    fun invalidateFocalLengthFactor(){
        val factor = safeStr2Dobule(editFlFactor?.text.toString())
        when(getCurrentType()){
            Accessory.ACCESSORY_TELE_CONVERTER -> {
                if(factor <= 1.0){
                    editFlFactor?.error = getString(R.string.error_flfactor_toosmall)
                }else{
                    editFlFactor?.error = null
                }
            }
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                if(factor >= 1.0 || factor == 0.0){
                    editFlFactor?.error = getString(R.string.error_flfactor_toobig)
                }else{
                    editFlFactor?.error = null
                }
            }
        }
    }

    fun setEventListeners(){
        val oldListenerAT = spAccessoryType?.onItemClickListener
        spAccessoryType?.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long){
                oldListenerAT?.onItemClick(parent, view, position, id)
                isDirty = true
                refreshUIforType((position+1) % 5)
                invalidateOptionsMenu()
            }
        }

        editName?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
                invalidateOptionsMenu()
            }
        })

        val oldListenerM = spMount?.onItemClickListener
        spMount?.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long){
                oldListenerM?.onItemClick(parent, view, position, id)
                isDirty = true
                invalidateOptionsMenu()
            }
        }

        spMount?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
                invalidateOptionsMenu()
            }
        })

        editFlFactor?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                isDirty = true
                when(getCurrentType()){
                    Accessory.ACCESSORY_TELE_CONVERTER,
                    Accessory.ACCESSORY_WIDE_CONVERTER -> invalidateFocalLengthFactor()
                }
                invalidateOptionsMenu()
            }
        })
    }

    fun getCurrentType(): Int{
        return (spAccessoryType!!.position+1) % 5
    }

    protected fun getSuggestListPref(prefkey: String, defRscId: Int, resource: Int): ArrayAdapter<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(prefkey, "[]")
        val strArray = java.util.ArrayList<String>()
        val defRsc = resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }
        strArray.addAll(defRsc)
        return ArrayAdapter<String>(this, resource, strArray.distinct())
    }

    protected fun saveSuggestListPref(prefkey: String, defRscId: Int, newValues: Array<String>) {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString(prefkey, "[]")
        val strArray = java.util.ArrayList<String>()
        val defRsc = resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        for(item in newValues){
            if(item.isEmpty()) continue
            if (strArray.indexOf(item) >= 0) {
                strArray.removeAt(strArray.indexOf(item))
                strArray.add(0, item)
            }
        }

        strArray.addAll(defRsc)

        val result = JSONArray(strArray.distinct())
        val e = pref.edit()
        e.putString(prefkey, result.toString())
        e.apply()
    }

    private fun safeStr2Dobule(s : String) : Double{
        try {
            return s.toDouble()
        }catch (e: NumberFormatException){
            return 0.0
        }
    }

    private fun canSave(): Boolean{
        val baseCond = (spAccessoryType!!.position >= 0) and (editName!!.text.length > 0)
        val flFactor = safeStr2Dobule(editFlFactor!!.text.toString())
        val additionalCond = when (spAccessoryType!!.position){
            0 -> true
            1 -> (spMount!!.text.length > 0) and (flFactor > 1.0)
            2 -> (spMount!!.text.length > 0) and (flFactor < 1.0) and (flFactor > 0.0)
            3 -> (spMount!!.text.length > 0)
            4 -> true
            else -> false
        }
        return baseCond and additionalCond
    }

    private fun getData(): Intent{
        var data: Intent = Intent()

        data.putExtra("id", this.id)
        data.putExtra("type", getCurrentType())
        data.putExtra("created", this.created)
        data.putExtra("name", editName?.text.toString())

        when(getCurrentType()){
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                data.putExtra("mount", spMount?.text.toString())
                data.putExtra("focal_length_factor", safeStr2Dobule(editFlFactor?.text.toString()))
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                data.putExtra("mount", spMount?.text.toString())
            }
        }

        return data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_accessory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setAdapters()

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent
        if (data != null) {
            loadData(data, dao)
        }else{
            setTitle(R.string.title_activity_add_accessory)
            refreshUIforType(Accessory.ACCESSORY_UNKNOWN)
        }
        dao.close()

        setEventListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save, menu)
        menu.findItem(R.id.menu_save).isEnabled = canSave()
        return true
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            101 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val resultData = getData()
                setResult(Activity.RESULT_OK, resultData)
                when(getCurrentType()) {
                    Accessory.ACCESSORY_EXT_TUBE,
                    Accessory.ACCESSORY_WIDE_CONVERTER,
                    Accessory.ACCESSORY_TELE_CONVERTER -> saveSuggestListPref("camera_mounts", R.array.camera_mounts,
                            arrayOf(spMount?.text.toString()))
                }
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
    }

    private fun askSave(){
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val data: Intent
        when (item.itemId) {
            android.R.id.home -> {
                data = Intent()
                if (isDirty)
                    Log.d("Dirty?", "yes")
                else
                    Log.d("Dirty?", "no")
                if (!isDirty) {
                    setResult(Activity.RESULT_CANCELED, data)
                    finish()
                } else {
                    askSave()
                }
                return true
            }
            R.id.menu_save -> {
                data = getData()
                setResult(Activity.RESULT_OK, data)
                when(getCurrentType()) {
                    Accessory.ACCESSORY_EXT_TUBE,
                    Accessory.ACCESSORY_WIDE_CONVERTER,
                    Accessory.ACCESSORY_TELE_CONVERTER -> saveSuggestListPref("camera_mounts", R.array.camera_mounts,
                            arrayOf(spMount?.text.toString()))
                }
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!isDirty) {
            setResult(Activity.RESULT_CANCELED, null)
            super.onBackPressed()
        } else {
            askSave()
        }
    }
}
