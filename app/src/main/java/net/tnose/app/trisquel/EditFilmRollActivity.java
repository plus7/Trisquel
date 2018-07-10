package net.tnose.app.trisquel;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditFilmRollActivity extends AppCompatActivity implements AbstractDialogFragment.Callback {
    private int id;
    private String created;
    private ArrayList<CameraSpec> cameralist;
    private CameraAdapter cadapter;
    private ImmediateAutoCompleteTextView editBrand, editManufacturer, editIso;
    private MaterialBetterSpinner cameraSpinner;
    private MaterialEditText editName;
    private boolean isDirty;

    protected void findViews(){
        cameraSpinner = findViewById(R.id.spinner_camera);
        editManufacturer = findViewById(R.id.edit_manufacturer);
        editBrand = findViewById(R.id.edit_brand);
        editIso = findViewById(R.id.edit_iso);
        editName = findViewById(R.id.edit_name);
    }

    protected void loadData(Intent data, TrisquelDao dao){
        int id = data.getIntExtra("id", -1);
        this.id = id;
        if(id <= 0) return;
        setTitle(R.string.title_activity_edit_filmroll);
        FilmRoll f = dao.getFilmRoll(id);
        this.created = Util.dateToStringUTC(f.created);

        editName.setText(f.name);

        if(f.camera.id > 0) {
            cameraSpinner.setPosition(cadapter.getPosition(f.camera.id));
        }

        editManufacturer.setText(f.manufacturer);
        editBrand.setText(f.brand);
        if(f.iso> 0) editIso.setText(Integer.toString(f.iso));
    }

    protected void setEventListeners(){
        editName.addTextChangedListener(new TextWatcher() {
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
        final AdapterView.OnItemClickListener oldListener = cameraSpinner.getOnItemClickListener();// これやらないとgetPositionがおかしくなる
        cameraSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateOptionsMenu();
                isDirty = true;
                oldListener.onItemClick(parent, view, position, id);
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

        editBrand.addTextChangedListener(new TextWatcher() {
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

        editIso.addTextChangedListener(new TextWatcher() {
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
    }

    protected boolean canSave(){
        return (editName.getText().toString().length() > 0) && cameraSpinner.getPosition() >= 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_film_roll);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        findViews();

        TrisquelDao dao = new TrisquelDao(getApplicationContext());
        dao.connection();
        cameralist = dao.getAllCameras();

        cadapter = new CameraAdapter(this, android.R.layout.simple_spinner_item, cameralist);
        cadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cameraSpinner.setAdapter(cadapter);

        ArrayAdapter<String> manufacturer_adapter =
                getSuggestListPref("film_manufacturer",
                        R.array.film_manufacturer,
                        android.R.layout.simple_dropdown_item_1line);
        editManufacturer.setAdapter(manufacturer_adapter);

        editBrand.setHelperText("銘柄の自動補完は未実装");

        ArrayAdapter<CharSequence> iso_adapter =
                ArrayAdapter.createFromResource(this, R.array.film_iso, android.R.layout.simple_dropdown_item_1line);

        editIso.setAdapter(iso_adapter);
        editIso.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b && editIso.getText().toString().isEmpty()){
                    Pattern zoom = Pattern.compile(".*?(\\d++).*");
                    Matcher m = zoom.matcher(editBrand.getText().toString());
                    if(m.find()){
                        String suggestedISO = m.group(1);
                        editIso.setText(suggestedISO);
                    }
                }
            }
        });

        Intent data = getIntent();
        if (data != null) {
            loadData(data, dao);
        }else{
            this.id = -1;
        }
        dao.close();

        if(cameralist.size() == 0){
            Toast.makeText(this, getString(R.string.error_please_reg_cam), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED, null);
            finish();
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
        outState.putBoolean("isDirty", isDirty);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save, menu);
        menu.findItem(R.id.menu_save).setEnabled(canSave());
        return true;
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
                    saveSuggestListPref("film_manufacturer",
                            R.array.film_manufacturer, editManufacturer.getText().toString());
                    setResult(RESULT_OK, resultData);
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
        data.putExtra("name", editName.getText().toString());
        data.putExtra("created", created);
        data.putExtra("camera", cameralist.get(cameraSpinner.getPosition()).id);
        data.putExtra("manufacturer", editManufacturer.getText().toString());
        data.putExtra("brand", editBrand.getText().toString());
        int iso;
        try{
            iso = Integer.parseInt(editIso.getText().toString());
        }catch (NumberFormatException e){
            iso = 0;
        }
        data.putExtra("iso", iso);
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
                saveSuggestListPref("film_manufacturer",
                        R.array.film_manufacturer, editManufacturer.getText().toString());
                setResult(RESULT_OK, data);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        Log.d("onBackPressed", "EditFilmRollActivity");
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
