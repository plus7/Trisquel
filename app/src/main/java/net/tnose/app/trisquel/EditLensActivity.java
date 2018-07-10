package net.tnose.app.trisquel;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditLensActivity extends AppCompatActivity implements AbstractDialogFragment.Callback {
    private int id;
    private String created;
    private ImmediateAutoCompleteTextView editMount, editManufacturer;
    private MaterialEditText editModelName, editFocalLength;
    private GridView fStopGridView;
    private FStepAdapter fsAdapter;
    private boolean isDirty;

    protected void findViews(){
        editManufacturer = findViewById(R.id.edit_manufacturer);
        editMount = findViewById(R.id.edit_mount);
        editFocalLength = findViewById(R.id.edit_focal_length);
        editModelName = findViewById(R.id.edit_model);
        fStopGridView = findViewById(R.id.fstop_gridview);
    }

    protected void loadData(Intent data, Bundle savedInstanceState){
        int id = data.getIntExtra("id", -1);
        this.id = id;
        fsAdapter = new FStepAdapter(this);
        if(id <= 0){
            if (savedInstanceState != null) {
                fsAdapter.setCheckedState(savedInstanceState.getString("FStepsString"));
            }
            fStopGridView.setAdapter(fsAdapter);
            return;
        }
        setTitle(R.string.title_activity_edit_lens);
        TrisquelDao dao = new TrisquelDao(getApplicationContext());
        dao.connection();
        LensSpec l = dao.getLens(id);
        dao.close();

        this.created = Util.dateToStringUTC(l.created);

        editManufacturer.setText(l.manufacturer);
        editMount.setText(l.mount);
        editModelName.setText(l.modelName);
        editFocalLength.setText(l.focalLength);

        if(savedInstanceState != null) {
            fsAdapter.setCheckedState(savedInstanceState.getString("FStepsString"));
        }else{
            fsAdapter.setCheckedState(l.fSteps);
        }
        fStopGridView.setAdapter(fsAdapter);
    }

    private boolean getFocalLengthOk(){
        if(!editFocalLength.getText().toString().isEmpty()) {
            Pattern zoom = Pattern.compile("(\\d++)-(\\d++)");
            Matcher m = zoom.matcher(editFocalLength.getText().toString());
            if (m.find()) {
                return true;
            }

            Pattern prime = Pattern.compile("(\\d++)");
            m = prime.matcher(editFocalLength.getText().toString());
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    protected void setEventListeners(){
        editMount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateOptionsMenu();
                isDirty = true;
            }
        });

        editMount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateOptionsMenu();
            }
        });

        editManufacturer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateOptionsMenu();
                isDirty = true;
            }
        });

        editModelName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateOptionsMenu();
                isDirty = true;
            }
        });

        editFocalLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateOptionsMenu();
                isDirty = true;
            }
        });

        editFocalLength.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b && editFocalLength.getText().toString().isEmpty()){
                    Pattern zoom = Pattern.compile(".*?(\\d++)-(\\d++)mm.*");
                    Matcher m = zoom.matcher(editModelName.getText().toString());
                    if(m.find()){
                        String suggestedWideFocalLength = m.group(1);
                        String suggestedTeleFocalLength = m.group(2);
                        editFocalLength.setText(suggestedWideFocalLength + "-" + suggestedTeleFocalLength);
                    }else {
                        Pattern prime = Pattern.compile(".*?(\\d++)mm.*");
                        m = prime.matcher(editModelName.getText().toString());
                        if (m.find()) {
                            String suggestedFocalLength = m.group(1);
                            editFocalLength.setText(suggestedFocalLength);
                        }
                    }
                }
            }
        });

        fStopGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateOptionsMenu();
                isDirty = true;
            }
        });
    }

    protected boolean canSave(){
        //マウントとモデル名とF値リストは必須。
        return (editMount.getText().length() > 0) && (editModelName.getText().length() > 0) && getFocalLengthOk();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_lens);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        findViews();

        ArrayAdapter<String> manufacturer_adapter =
                getSuggestListPref("lens_manufacturer",
                        R.array.lens_manufacturer,
                        android.R.layout.simple_dropdown_item_1line);
        editManufacturer.setAdapter(manufacturer_adapter);

        ArrayAdapter<String> mount_adapter =
                getSuggestListPref("camera_mounts",
                        R.array.camera_mounts,
                        android.R.layout.simple_dropdown_item_1line);
        editMount.setAdapter(mount_adapter);

        editFocalLength.setHelperText(getString(R.string.hint_zoom));

        Intent data = getIntent();
        if (data != null) {
            loadData(data, savedInstanceState);
        }else{
            this.id = -1;
        }

        setEventListeners();

        if(savedInstanceState != null){
            isDirty = savedInstanceState.getBoolean("isDirty");
        }else{
            isDirty = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("FStepsString", getFStepsString());
        outState.putBoolean("isDirty", isDirty);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    private String getFStepsString(){
        Log.d("getFStepsString", fsAdapter.getFStepsString());
        return fsAdapter.getFStepsString();
    }

    //dead copy from EditCameraActivity
    protected ArrayAdapter<String> getSuggestListPref(String prefkey, int defRscId, int resource){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefstr = pref.getString(prefkey, "[]");
        ArrayList<String> strArray = new ArrayList<String>();
        String[] defRsc = getResources().getStringArray(defRscId);
        try {
            JSONArray array = new JSONArray(prefstr);
            for(int i = 0; i < array.length(); i++){
                strArray.add(array.getString(i));
            }
        }catch(JSONException e){}

        for(int i = 0; i < defRsc.length; i++){
            if(strArray.contains(defRsc[i])) continue;
            strArray.add(defRsc[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, resource, strArray);
        return adapter;
    }

    protected void saveSuggestListPref(String prefkey, int defRscId, String newValue){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefstr = pref.getString(prefkey, "[]");
        ArrayList<String> strArray = new ArrayList<String>();
        String[] defRsc = getResources().getStringArray(defRscId);
        try {
            JSONArray array = new JSONArray(prefstr);
            for(int i = 0; i < array.length(); i++){
                strArray.add(array.getString(i));
            }
        }catch(JSONException e){}

        if(strArray.indexOf(newValue) >= 0) {
            strArray.remove(strArray.indexOf(newValue));
        }
        strArray.add(0, newValue);
        for(int i = 0; i < defRsc.length; i++){
            if(strArray.contains(defRsc[i])) continue;
            strArray.add(defRsc[i]);
        }
        JSONArray result = new JSONArray(strArray);
        SharedPreferences.Editor e = pref.edit();
        e.putString(prefkey, result.toString());
        e.apply();
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case 101:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    Intent resultData = getData();
                    setResult(RESULT_OK, resultData);
                    saveSuggestListPref("lens_manufacturer",
                            R.array.lens_manufacturer, editManufacturer.getText().toString());
                    saveSuggestListPref("camera_mounts",
                            R.array.camera_mounts, editMount.getText().toString());
                    finish();
                }else if(resultCode == DialogInterface.BUTTON_NEGATIVE) {
                    setResult(RESULT_CANCELED, data);
                    finish();
                }
                break;
            case 102:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    /* do nothing */
                }else if(resultCode == DialogInterface.BUTTON_NEGATIVE) {
                    setResult(RESULT_CANCELED, data);
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    public void onDialogCancelled(int requestCode) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    public Intent getData() {
        Intent data = new Intent();
        data.putExtra("id", id);
        data.putExtra("created", created);
        data.putExtra("mount", editMount.getText().toString());
        data.putExtra("manufacturer", editManufacturer.getText().toString());
        data.putExtra("model_name", editModelName.getText().toString());
        data.putExtra("focal_length", editFocalLength.getText().toString());
        data.putExtra("f_steps", getFStepsString());
        return data;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent data;
        switch(item.getItemId()) {
            case android.R.id.home:
                data = new Intent();
                if(isDirty) Log.d("Dirty?", "yes");
                else Log.d("Dirty?", "no");
                if(!isDirty) {
                    setResult(RESULT_CANCELED, data);
                    finish();
                }else{
                    if(canSave()) {
                        AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                                .build(101);
                        fragment.getArguments().putString("message", getString(R.string.msg_save_or_discard_data));
                        fragment.getArguments().putString("positive", getString(R.string.save));
                        fragment.getArguments().putString("negative", getString(R.string.discard));
                        fragment.showOn(this, "dialog");
                    }else{
                        AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                                .build(102);
                        fragment.getArguments().putString("message", getString(R.string.msg_continue_editing_or_discard_data));
                        fragment.getArguments().putString("positive", getString(R.string.continue_editing));
                        fragment.getArguments().putString("negative", getString(R.string.discard));
                        fragment.showOn(this, "dialog");
                    }
                }
                return true;
            case R.id.menu_save:
                data = getData();
                setResult(RESULT_OK, data);
                saveSuggestListPref("lens_manufacturer",
                        R.array.lens_manufacturer, editManufacturer.getText().toString());
                saveSuggestListPref("camera_mounts",
                        R.array.camera_mounts, editMount.getText().toString());
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save, menu);
        menu.findItem(R.id.menu_save).setEnabled(canSave());
        return true;
    }

    @Override
    public void onBackPressed(){
        Log.d("onBackPressed", "EditLensActivity");
        if(!isDirty) {
            setResult(RESULT_CANCELED, null);
            super.onBackPressed();
        }else{
            if(canSave()) {
                AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                        .build(101);
                fragment.getArguments().putString("message", getString(R.string.msg_save_or_discard_data));
                fragment.getArguments().putString("positive", getString(R.string.save));
                fragment.getArguments().putString("negative", getString(R.string.discard));
                fragment.showOn(this, "dialog");
            }else{
                AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                        .build(102);
                fragment.getArguments().putString("message", getString(R.string.msg_continue_editing_or_discard_data));
                fragment.getArguments().putString("positive", getString(R.string.continue_editing));
                fragment.getArguments().putString("negative", getString(R.string.discard));
                fragment.showOn(this, "dialog");
            }
        }
    }
}
