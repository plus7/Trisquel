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
import net.tnose.app.trisquel.databinding.ActivityEditAccessoryBinding
import org.json.JSONArray
import org.json.JSONException

class EditAccessoryActivity : AppCompatActivity(), AbstractDialogFragment.Callback  {
    private var id: Int = -1
    private var created: String = ""
    private var name: String = ""
    private var mount: String = ""
    private var isResumed: Boolean = false
    private var isDirty: Boolean = false
    private lateinit var binding: ActivityEditAccessoryBinding

    fun setAdapters(){
        val typeArray = ArrayList<String>()
        typeArray.add(getString(R.string.label_accessory_filter))
        typeArray.add(getString(R.string.label_accessory_tc))
        typeArray.add(getString(R.string.label_accessory_wc))
        typeArray.add(getString(R.string.label_accessory_ext_tube))
        typeArray.add(getString(R.string.label_accessory_unknown))

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, typeArray)
        binding.spinnerAccessoryType.setAdapter(adapter)

        binding.spinnerMount.setAdapter(getSuggestListPref("camera_mounts", R.array.camera_mounts, android.R.layout.simple_spinner_item))
    }

    fun loadData(data: Intent, dao: TrisquelDao, savedInstanceState: Bundle?){
        val id = data.getIntExtra("id", 0)
        this.id = id
        // savedInstanceStateに関係ない部分
        if(id < 0)
            setTitle(R.string.title_activity_add_accessory)
        else
            setTitle(R.string.title_activity_edit_accessory)

        if(id > 0 && savedInstanceState == null) { //既存データを開きたて
            val a = dao.getAccessory(id)
            this.created = Util.dateToStringUTC(a!!.created)

            binding.editName.setText(a.name)
            when(a.type){
                Accessory.ACCESSORY_TELE_CONVERTER,
                Accessory.ACCESSORY_WIDE_CONVERTER -> {
                    binding.spinnerMount.setText(a.mount)
                    binding.editFlFactor.setText(a.focal_length_factor.toString())
                }
                Accessory.ACCESSORY_EXT_TUBE -> {
                    binding.spinnerMount.setText(a.mount)
                }
            }
            binding.spinnerAccessoryType.position = (a.type + 4) % 5
            refreshUIforType(a.type)
        }else if(savedInstanceState != null){ //復帰データあり
            this.created = savedInstanceState.getString("created") ?: ""

            binding.editName.setText(savedInstanceState.getString("name"))

            val type = savedInstanceState.getInt("type")
            binding.spinnerAccessoryType.position = (type + 4) % 5

            when(type){
                Accessory.ACCESSORY_TELE_CONVERTER,
                Accessory.ACCESSORY_WIDE_CONVERTER -> {
                    binding.spinnerMount.setText(savedInstanceState.getString("mount"))
                    binding.editFlFactor.setText(savedInstanceState.getDouble("focal_length_factor").toString())
                }
                Accessory.ACCESSORY_EXT_TUBE -> {
                    binding.spinnerMount.setText(savedInstanceState.getString("mount"))
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
                binding.spinnerMount.visibility = View.GONE
                binding.editFlFactor.visibility = View.GONE
            }
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                binding.spinnerMount.visibility = View.VISIBLE
                binding.editFlFactor.visibility = View.VISIBLE
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                binding.spinnerMount.visibility = View.VISIBLE
                binding.editFlFactor.visibility = View.GONE
            }
        }
        invalidateFocalLengthFactor()
    }

    fun invalidateFocalLengthFactor(){
        val factor = Util.safeStr2Dobule(binding.editFlFactor.text.toString())
        when(getCurrentType()){
            Accessory.ACCESSORY_TELE_CONVERTER -> {
                if(factor <= 1.0){
                    binding.editFlFactor.error = getString(R.string.error_flfactor_toosmall)
                }else{
                    binding.editFlFactor.error = null
                }
            }
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                if(factor >= 1.0 || factor == 0.0){
                    binding.editFlFactor.error = getString(R.string.error_flfactor_toobig)
                }else{
                    binding.editFlFactor.error = null
                }
            }
        }
    }

    fun setEventListeners(){
        val oldListenerAT = binding.spinnerAccessoryType.onItemClickListener
        binding.spinnerAccessoryType.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long){
                oldListenerAT?.onItemClick(parent, view, position, id)
                if(isResumed) isDirty = true
                refreshUIforType((position+1) % 5)
                invalidateOptionsMenu()
            }
        }

        binding.editName.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        })

        val oldListenerM = binding.spinnerMount.onItemClickListener
        binding.spinnerMount.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long){
                oldListenerM?.onItemClick(parent, view, position, id)
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        }

        binding.spinnerMount.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if(isResumed) isDirty = true
                invalidateOptionsMenu()
            }
        })

        binding.editFlFactor.addTextChangedListener(object : TextWatcher{
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
        return (binding.spinnerAccessoryType.position+1) % 5
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
        val baseCond = (binding.spinnerAccessoryType.position >= 0) and (binding.editName.text?.isNotEmpty() ?: false)
        val flFactor = Util.safeStr2Dobule(binding.editFlFactor.text.toString())
        val mountIsNotEmpty = binding.spinnerMount.text?.isNotEmpty() ?: false
        val additionalCond = when (binding.spinnerAccessoryType.position){
            0 -> true
            1 -> mountIsNotEmpty and (flFactor > 1.0)
            2 -> mountIsNotEmpty and (flFactor < 1.0) and (flFactor > 0.0)
            3 -> mountIsNotEmpty
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
        data.putExtra("name", binding.editName.text.toString())

        when(getCurrentType()){
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                data.putExtra("mount", binding.spinnerMount.text.toString())
                data.putExtra("focal_length_factor", Util.safeStr2Dobule(binding.editFlFactor.text.toString()))
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                data.putExtra("mount", binding.spinnerMount.text.toString())
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
        binding = ActivityEditAccessoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        outState.putString("name", binding.editName.text.toString())

        when(getCurrentType()) {
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                outState.putString("mount", binding.spinnerMount.text.toString())
                outState.putDouble("focal_length_factor", Util.safeStr2Dobule(binding.editFlFactor.text.toString()))
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                outState.putString("mount", binding.spinnerMount.text.toString())
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
                            arrayOf(binding.spinnerMount.text.toString()))
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
                            arrayOf(binding.spinnerMount.text.toString()))
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
