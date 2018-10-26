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
import kotlinx.android.synthetic.main.activity_edit_accessory.*
import org.json.JSONArray
import org.json.JSONException

class EditAccessoryActivity : AppCompatActivity(), AbstractDialogFragment.Callback  {
    private var id: Int = -1
    private var created: String = ""
    private var name: String = ""
    private var mount: String = ""
    private var isResumed: Boolean = false
    private var isDirty: Boolean = false

    fun setAdapters(){
        val typeArray = ArrayList<String>()
        typeArray.add(getString(R.string.label_accessory_filter))
        typeArray.add(getString(R.string.label_accessory_tc))
        typeArray.add(getString(R.string.label_accessory_wc))
        typeArray.add(getString(R.string.label_accessory_ext_tube))
        typeArray.add(getString(R.string.label_accessory_unknown))

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, typeArray)
        spinner_accessory_type?.setAdapter(adapter)

        spinner_mount?.setAdapter(getSuggestListPref("camera_mounts", R.array.camera_mounts, android.R.layout.simple_spinner_item))
    }

    fun loadData(data: Intent, dao: TrisquelDao, savedInstanceState: Bundle?){
        val id = data.getIntExtra("id", -1)
        this.id = id
        // savedInstanceStateに関係ない部分
        if(id < 0)
            setTitle(R.string.title_activity_add_accessory)
        else
            setTitle(R.string.title_activity_edit_accessory)

        if(id >= 0 && savedInstanceState == null) { //既存データを開きたて
            val a = dao.getAccessory(id)
            this.created = Util.dateToStringUTC(a!!.created)

            edit_name!!.setText(a.name)
            when(a.type){
                Accessory.ACCESSORY_TELE_CONVERTER,
                Accessory.ACCESSORY_WIDE_CONVERTER -> {
                    spinner_mount?.setText(a.mount)
                    edit_fl_factor?.setText(a.focal_length_factor.toString())
                }
                Accessory.ACCESSORY_EXT_TUBE -> {
                    spinner_mount?.setText(a.mount)
                }
            }
            spinner_accessory_type?.position = (a.type + 4) % 5
            refreshUIforType(a.type)
        }else if(savedInstanceState != null){ //復帰データあり
            this.created = savedInstanceState.getString("created")

            edit_name!!.setText(savedInstanceState.getString("name"))

            val type = savedInstanceState.getInt("type")
            spinner_accessory_type?.position = (type + 4) % 5

            when(type){
                Accessory.ACCESSORY_TELE_CONVERTER,
                Accessory.ACCESSORY_WIDE_CONVERTER -> {
                    spinner_mount?.setText(savedInstanceState.getString("mount"))
                    edit_fl_factor?.setText(savedInstanceState.getDouble("focal_length_factor").toString())
                }
                Accessory.ACCESSORY_EXT_TUBE -> {
                    spinner_mount?.setText(savedInstanceState.getString("mount"))
                }
            }
            refreshUIforType(type)
        }else{  //新規開きたて
            refreshUIforType(Accessory.ACCESSORY_UNKNOWN)
        }
    }

    fun refreshUIforType(type: Int){
        when(type){
            Accessory.ACCESSORY_FILTER, Accessory.ACCESSORY_UNKNOWN, -1 -> {
                spinner_mount?.visibility = View.GONE
                edit_fl_factor?.visibility = View.GONE
            }
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                spinner_mount?.visibility = View.VISIBLE
                edit_fl_factor?.visibility = View.VISIBLE
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                spinner_mount?.visibility = View.VISIBLE
                edit_fl_factor?.visibility = View.GONE
            }
        }
        invalidateFocalLengthFactor()
    }

    fun invalidateFocalLengthFactor(){
        val factor = Util.safeStr2Dobule(edit_fl_factor?.text.toString())
        when(getCurrentType()){
            Accessory.ACCESSORY_TELE_CONVERTER -> {
                if(factor <= 1.0){
                    edit_fl_factor?.error = getString(R.string.error_flfactor_toosmall)
                }else{
                    edit_fl_factor?.error = null
                }
            }
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                if(factor >= 1.0 || factor == 0.0){
                    edit_fl_factor?.error = getString(R.string.error_flfactor_toobig)
                }else{
                    edit_fl_factor?.error = null
                }
            }
        }
    }

    fun setEventListeners(){
        val oldListenerAT = spinner_accessory_type?.onItemClickListener
        spinner_accessory_type?.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long){
                oldListenerAT?.onItemClick(parent, view, position, id)
                if(isResumed) isDirty = true
                refreshUIforType((position+1) % 5)
                invalidateOptionsMenu()
            }
        }

        edit_name?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        })

        val oldListenerM = spinner_mount?.onItemClickListener
        spinner_mount?.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long){
                oldListenerM?.onItemClick(parent, view, position, id)
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        }

        spinner_mount?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        })

        edit_fl_factor?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                when(getCurrentType()){
                    Accessory.ACCESSORY_TELE_CONVERTER,
                    Accessory.ACCESSORY_WIDE_CONVERTER -> invalidateFocalLengthFactor()
                }
                invalidateOptionsMenu()
            }
        })
    }

    fun getCurrentType(): Int{
        return (spinner_accessory_type!!.position+1) % 5
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

    private fun canSave(): Boolean{
        val baseCond = (spinner_accessory_type!!.position >= 0) and (edit_name!!.text.length > 0)
        val flFactor = Util.safeStr2Dobule(edit_fl_factor!!.text.toString())
        val additionalCond = when (spinner_accessory_type!!.position){
            0 -> true
            1 -> (spinner_mount!!.text.length > 0) and (flFactor > 1.0)
            2 -> (spinner_mount!!.text.length > 0) and (flFactor < 1.0) and (flFactor > 0.0)
            3 -> (spinner_mount!!.text.length > 0)
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
        data.putExtra("name", edit_name?.text.toString())

        when(getCurrentType()){
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                data.putExtra("mount", spinner_mount?.text.toString())
                data.putExtra("focal_length_factor", Util.safeStr2Dobule(edit_fl_factor?.text.toString()))
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                data.putExtra("mount", spinner_mount?.text.toString())
            }
        }

        return data
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_accessory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setAdapters()

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent
        loadData(data, dao, savedInstanceState)
        dao.close()

        setEventListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putInt("type", getCurrentType())
        outState.putString("created", this.created)
        outState.putString("name", edit_name!!.text.toString())

        when(getCurrentType()) {
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                outState.putString("mount", spinner_mount!!.text.toString())
                outState.putDouble("focal_length_factor", Util.safeStr2Dobule(edit_fl_factor?.text.toString()))
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                outState.putString("mount", spinner_mount.text.toString())
            }
        }

        super.onSaveInstanceState(outState)
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
                            arrayOf(spinner_mount?.text.toString()))
                }
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
    }

    private fun askSave(){
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
                            arrayOf(spinner_mount?.text.toString()))
                }
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
            askSave()
        }
    }
}
