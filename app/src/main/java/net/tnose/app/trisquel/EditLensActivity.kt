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
import net.tnose.app.trisquel.databinding.ActivityEditLensBinding
import org.json.JSONArray
import org.json.JSONException
import java.util.Date
import java.util.regex.Pattern

class EditLensActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
    private var id: Int = -1
    private var created: String = ""
    private var fsAdapter: FStepAdapter? = null
    private var isResumed: Boolean = false
    private var isDirty: Boolean = false
    private lateinit var binding: ActivityEditLensBinding

    private val focalLengthOk: Boolean
        get() {
            if (!binding.editFocalLength.text.toString().isEmpty()) {
                val zoom = Pattern.compile("(\\d++)-(\\d++)")
                var m = zoom.matcher(binding.editFocalLength.text.toString())
                if (m.find()) {
                    return true
                }

                val prime = Pattern.compile("(\\d++)")
                m = prime.matcher(binding.editFocalLength.text.toString())
                if (m.find()) {
                    return true
                }
            }
            return false
        }

    private val fStepsString: String
        get() = fsAdapter!!.fStepsString

    val data: Intent
        get() {
            val data = Intent()
            val l = LensSpec(id,
                    if(created.isNotEmpty()) created else Util.dateToStringUTC(Date()),
                    Util.dateToStringUTC(Date()),
                    binding.editMount.text.toString(), 0,
                    binding.editManufacturer.text.toString(),
                    binding.editModel.text.toString(),
                    binding.editFocalLength.text.toString(),
                    fStepsString)
            data.putExtra("lensspec", l)
            return data
        }

    protected fun loadData(data: Intent, savedInstanceState: Bundle?) {
        val id = data.getIntExtra("id", -1)
        this.id = id

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val l = dao.getLens(id)
        dao.close()

        if(id < 0)
            setTitle(R.string.title_activity_reg_lens)
        else
            setTitle(R.string.title_activity_edit_lens)

        fsAdapter = FStepAdapter(this)

        if(id >= 0 && savedInstanceState == null) { //既存データを開きたて
            this.created = Util.dateToStringUTC(l!!.created)
            binding.editManufacturer.setText(l.manufacturer)
            binding.editMount.setText(l.mount)
            binding.editModel.setText(l.modelName)
            binding.editFocalLength.setText(l.focalLength)
            fsAdapter!!.setCheckedState(l.fSteps)
        }else if(savedInstanceState != null){ //復帰データあり
            this.created = savedInstanceState.getString("created")?:""
            binding.editManufacturer.setText(savedInstanceState.getString("manufacturer"))
            binding.editMount.setText(savedInstanceState.getString("mount"))
            binding.editModel.setText(savedInstanceState.getString("model_name"))
            binding.editFocalLength.setText(savedInstanceState.getString("focal_length"))
            fsAdapter!!.setCheckedState(savedInstanceState.getString("FStepsString")?:"")
        }else{ //未入力開きたて
            this.created = ""
        }
        binding.fstopGridview.adapter = fsAdapter
    }

    protected fun setEventListeners() {
        binding.editMount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.editMount.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> invalidateOptionsMenu() }

        binding.editManufacturer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.editModel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.editFocalLength.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                invalidateOptionsMenu()
                if(isResumed) isDirty = true
            }
        })

        binding.editFocalLength.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b && binding.editFocalLength.text.toString().isEmpty()) {
                val zoom = Pattern.compile(".*?(\\d++)-(\\d++)mm.*")
                var m = zoom.matcher(binding.editModel.text.toString())
                if (m.find()) {
                    val suggestedWideFocalLength = m.group(1)
                    val suggestedTeleFocalLength = m.group(2)
                    binding.editFocalLength.setText("$suggestedWideFocalLength-$suggestedTeleFocalLength")
                } else {
                    val prime = Pattern.compile(".*?(\\d++)mm.*")
                    m = prime.matcher(binding.editModel.text.toString())
                    if (m.find()) {
                        val suggestedFocalLength = m.group(1)
                        binding.editFocalLength.setText(suggestedFocalLength)
                    }
                }
            }
        }

        binding.fstopGridview.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            invalidateOptionsMenu()
            if(isResumed) isDirty = true
        }
    }

    protected fun canSave(): Boolean {
        //マウントとモデル名とF値リストは必須。
        return (binding.editMount.text?.isNotEmpty() ?: false) &&
                (binding.editModel.text?.isNotEmpty() ?: false) && focalLengthOk
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditLensBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        val manufacturer_adapter = getSuggestListPref("lens_manufacturer",
                R.array.lens_manufacturer,
                android.R.layout.simple_dropdown_item_1line)
        binding.editManufacturer.setAdapter(manufacturer_adapter)

        val mount_adapter = getSuggestListPref("camera_mounts",
                R.array.camera_mounts,
                android.R.layout.simple_dropdown_item_1line)
        binding.editMount.setAdapter(mount_adapter)

        binding.editFocalLength.setHelperText(getString(R.string.hint_zoom))

        val data = intent
        loadData(data, savedInstanceState)
        setEventListeners()

        if (savedInstanceState != null) {
            isDirty = savedInstanceState.getBoolean("isDirty")
        } else {
            isDirty = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("FStepsString", fStepsString)
        outState.putString("created", created)
        outState.putString("manufacturer", binding.editManufacturer.text.toString())
        outState.putString("mount", binding.editMount.text.toString())
        outState.putString("model_name", binding.editModel.text.toString())
        outState.putString("focal_length", binding.editFocalLength.text.toString())
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
                saveSuggestListPref("lens_manufacturer",
                        R.array.lens_manufacturer, binding.editManufacturer.text.toString())
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
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
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
                saveSuggestListPref("lens_manufacturer",
                        R.array.lens_manufacturer, binding.editManufacturer.text.toString())
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
