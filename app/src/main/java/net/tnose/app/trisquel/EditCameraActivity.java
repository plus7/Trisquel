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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditCameraActivity extends AppCompatActivity implements AbstractDialogFragment.Callback {
    private int id;
    private int type;
    private String created;
    private ArrayAdapter<CharSequence> formatAdapter;
    private ArrayAdapter<CharSequence> ssAdapterOne, ssAdapterHalf, ssAdapterOneThird;
    private RadioButton radioStopOne, radioStopHalf, radioStopOneThird;
    private MaterialBetterSpinner spSsUb, spSsLb;
    private ImmediateAutoCompleteTextView editMount, editManufacturer;
    private TextView editModelName;
    private MaterialBetterSpinner spFormat;
    private Spinner spEvGrain;
    private Spinner spEvWidth;
    private CheckBox cbBulb;
    private LinearLayout layoutFLC;
    private MaterialEditText editFocalLength;
    private MaterialEditText editFixedLensName;
    private GridView fsGridView;
    private FStepAdapter fsAdapter;
    private boolean isDirty;
    private boolean userIsInteracting = false;

    private void refreshShutterSpeedSpinners(){
        if(radioStopOne.isChecked()){
            spSsUb.setAdapter(ssAdapterOne);
            spSsLb.setAdapter(ssAdapterOne);
        }else if(radioStopHalf.isChecked()){
            spSsUb.setAdapter(ssAdapterHalf);
            spSsLb.setAdapter(ssAdapterHalf);
        }else{
            spSsUb.setAdapter(ssAdapterOneThird);
            spSsLb.setAdapter(ssAdapterOneThird);
        }
    }

    private boolean getShutterSpeedRangeOk(){
        double fastestSS = Util.stringToDoubleShutterSpeed(spSsUb.getText().toString());
        double slowestSS = Util.stringToDoubleShutterSpeed(spSsLb.getText().toString());
        if(fastestSS == 0.0 || slowestSS == 0.0) {
            return false;
        }else return !(fastestSS > slowestSS);
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

    private boolean doShutterSpeedValidation(){
        boolean ret = true;
        double fastestSS = Util.stringToDoubleShutterSpeed(spSsUb.getText().toString());
        double slowestSS = Util.stringToDoubleShutterSpeed(spSsLb.getText().toString());
        if(fastestSS == 0.0 || slowestSS == 0.0) { //未設定なだけなのでエラー表示にはしない
            spSsUb.setError(null);
            spSsLb.setError(null);
        }else if(fastestSS > slowestSS){
            spSsUb.setError("okashii");
            spSsLb.setError("");
            ret = false;
        }else{
            spSsUb.setError(null);
            spSsLb.setError(null);
        }
        return ret;
    }

    protected void findViews(){
        radioStopOne = findViewById(R.id.radio_stop_one);
        radioStopHalf = findViewById(R.id.radio_stop_half);
        radioStopOneThird = findViewById(R.id.radio_stop_one_third);
        spSsUb = findViewById(R.id.spinner_fastest_ss);
        spSsLb = findViewById(R.id.spinner_slowest_ss);
        editManufacturer = findViewById(R.id.edit_manufacturer);
        editMount = findViewById(R.id.edit_mount);
        editModelName = findViewById(R.id.edit_model);
        spFormat = findViewById(R.id.spinner_format);
        spEvGrain = findViewById(R.id.spinner_ev_grain_size);
        spEvWidth = findViewById(R.id.spinner_ev_width);
        cbBulb = findViewById(R.id.check_bulb_available);
        layoutFLC = findViewById(R.id.layout_flc);
        editFocalLength = findViewById(R.id.edit_focal_length_flc);
        editFixedLensName = findViewById(R.id.edit_lens_name_flc);
        fsGridView = findViewById(R.id.gridview);
    }

    protected void loadData(Intent data, Bundle savedInstanceState){
        int id = data.getIntExtra("id", -1);
        this.id = id;
        int type = data.getIntExtra("type", 0);
        this.type = type;
        if(type == 1) {
            editMount.setVisibility(View.GONE);
            layoutFLC.setVisibility(View.VISIBLE);
            setTitle(R.string.title_activity_edit_cam_and_lens);
            fsAdapter = new FStepAdapter(this);
            if (savedInstanceState != null) {
                fsAdapter.setCheckedState(savedInstanceState.getString("FStepsString"));
            }
        }else{
            setTitle(R.string.title_activity_edit_cam);
        }

        if(id <= 0){
            if(type == 1) fsGridView.setAdapter(fsAdapter);
            return;
        }

        TrisquelDao dao = new TrisquelDao(getApplicationContext());
        dao.connection();
        CameraSpec c = dao.getCamera(id);
        LensSpec l = null;
        if(c.type == 1){
            l = dao.getLens(dao.getFixedLensIdByBody(c.id));
        }
        dao.close();
        this.created = Util.dateToStringUTC(c.created);
        editMount.setText(c.mount);
        editManufacturer.setText(c.manufacturer);
        editModelName.setText(c.modelName);
        if(c.format<0) c.format = 0;
        spFormat.setPosition(c.format);
        switch(c.shutterSpeedGrainSize){
            case 1:
                radioStopOne.setChecked(true);
                break;
            case 2:
                radioStopHalf.setChecked(true);
                break;
            case 3:
                radioStopOneThird.setChecked(true);
                break;
            default:
        }
        spSsUb.setText(Util.doubleToStringShutterSpeed(c.fastestShutterSpeed));
        spSsLb.setText(Util.doubleToStringShutterSpeed(c.slowestShutterSpeed));
        cbBulb.setChecked(c.bulbAvailable);
        spEvGrain.setSelection(c.evGrainSize - 1, false);
        spEvWidth.setSelection(c.evWidth - 1, false);

        if(c.type == 1) {
            if (savedInstanceState == null) {
                fsAdapter.setCheckedState(l.fSteps);
            }
            fsGridView.setAdapter(fsAdapter);
            editFixedLensName.setText(l.modelName);
            editFocalLength.setText(l.focalLength);
        }
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
                isDirty = true;
                invalidateOptionsMenu();
            }
        });

        editMount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateOptionsMenu();
                isDirty = true;
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
                isDirty = true;
                invalidateOptionsMenu();
                if(editModelName.getText().toString().equals("jenkinsushi")){
                    Toast.makeText(EditCameraActivity.this,
                            getString(R.string.google_maps_key),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        spFormat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                isDirty = true;
            }
        });

        RadioGroup rg = findViewById(R.id.radiogroup_ss_stop);
        rg.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        refreshShutterSpeedSpinners();
                        isDirty = true;
                    }
                }
        );

        spSsUb.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doShutterSpeedValidation();
                invalidateOptionsMenu();
                isDirty = true;
            }
        });

        spSsLb.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doShutterSpeedValidation();
                invalidateOptionsMenu();
                isDirty = true;
            }
        });

        cbBulb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isDirty = true;
            }
        });


        spEvGrain.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(userIsInteracting) isDirty = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if(userIsInteracting) isDirty = true;
            }
        });

        spEvWidth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(userIsInteracting) isDirty = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if(userIsInteracting) isDirty = true;
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

        editFixedLensName.addTextChangedListener(new TextWatcher() {
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

        fsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateOptionsMenu();
                isDirty = true;
            }
        });
    }

    protected boolean canSave(){
        boolean cameraOk, lensOk;
        //マウントが空でない
        //モデル名が空でない
        //シャッタースピードレンジが設定されていて内容に矛盾がない
        cameraOk = (type == 1 || (type == 0 && editMount.getText().length() > 0)) &&
                (editModelName.getText().length() > 0) &&
                getShutterSpeedRangeOk();

        //レンズ付きモデルの場合
        //焦点距離が設定されている
        //F値リストに少なくとも1つチェックが入っている
        if(type == 1) {
            lensOk = getFocalLengthOk() && !fsAdapter.getFStepsString().isEmpty();
        }else{
            lensOk = true;
        }
        return cameraOk && lensOk;
    }

    protected boolean isDataChanged(){
        return false;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_camera);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        findViews();

        ssAdapterOne = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one, android.R.layout.simple_spinner_item);
        ssAdapterHalf = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_half, android.R.layout.simple_spinner_item);
        ssAdapterOneThird = ArrayAdapter.createFromResource(this, R.array.shutter_speeds_one_third, android.R.layout.simple_spinner_item);

        ssAdapterOne.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ssAdapterHalf.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ssAdapterOneThird.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spSsUb.setAdapter(ssAdapterOne);
        spSsLb.setAdapter(ssAdapterOne);

        spSsUb.setHelperText(getString(R.string.label_shutter_speed_fastest));
        spSsLb.setHelperText(getString(R.string.label_shutter_speed_slowest));

        formatAdapter = ArrayAdapter.createFromResource(this, R.array.film_formats, android.R.layout.simple_spinner_item);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFormat.setAdapter(formatAdapter);
        spFormat.setPosition(FilmFormat.FULL_FRAME.ordinal());

        ArrayAdapter<String> manufacturer_adapter =
                getSuggestListPref("camera_manufacturer",
                        R.array.camera_manufacturer,
                        android.R.layout.simple_dropdown_item_1line);
        editManufacturer.setAdapter(manufacturer_adapter);

        ArrayAdapter<String> mount_adapter =
                getSuggestListPref("camera_mounts",
                        R.array.camera_mounts,
                        android.R.layout.simple_dropdown_item_1line);
        editMount.setAdapter(mount_adapter);

        Intent data = getIntent();
        if (data != null) {
            loadData(data, savedInstanceState);
        }else{
            this.id = -1;
            this.type = 0;
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
        outState.putString("FStepsString", fsAdapter.getFStepsString());
        outState.putBoolean("isDirty", isDirty);
        super.onSaveInstanceState(outState);
    }

    private int getSSGrainSize(){
        if(radioStopOne.isChecked()){
            return 1;
        }else if(radioStopHalf.isChecked()){
            return 2;
        }else{
            return 3;
        }
    }

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
                    saveSuggestListPref("camera_manufacturer",
                            R.array.camera_manufacturer, editManufacturer.getText().toString());
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
        data.putExtra("id", this.id);
        data.putExtra("type", this.type);
        data.putExtra("created", this.created);
        data.putExtra("mount", editMount.getText().toString());
        data.putExtra("manufacturer", editManufacturer.getText().toString());
        data.putExtra("model_name", editModelName.getText().toString());
        data.putExtra("format", spFormat.getPosition());
        data.putExtra("ss_grain_size", getSSGrainSize());
        data.putExtra("fastest_ss", Util.stringToDoubleShutterSpeed(spSsUb.getText().toString()));
        data.putExtra("slowest_ss", Util.stringToDoubleShutterSpeed(spSsLb.getText().toString()));
        data.putExtra("bulb_available", cbBulb.isChecked() ? 1 : 0);
                /*
                  "shutter_speeds"はまだ対応しない
                */
        data.putExtra("ev_grain_size", spEvGrain.getSelectedItemPosition() + 1);
        data.putExtra("ev_width", spEvWidth.getSelectedItemPosition() + 1);
        if(type == 1){
            data.putExtra("fixedlens_name", editFixedLensName.getText().toString());
            data.putExtra("fixedlens_focal_length", editFocalLength.getText().toString());
            data.putExtra("fixedlens_f_steps", fsAdapter.getFStepsString());
        }
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
                saveSuggestListPref("camera_manufacturer",
                        R.array.camera_manufacturer, editManufacturer.getText().toString());
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
        Log.d("onBackPressed", "EditCameraActivity");
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
