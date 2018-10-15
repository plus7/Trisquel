package net.tnose.app.trisquel;

import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class EditPhotoActivity extends AppCompatActivity implements AbstractDialogFragment.Callback{
    final int REQCODE_GET_LOCATION = 100;
    final int REQCODE_MOUNT_ADAPTERS = 103;
    final int REQCODE_DATE = 104;
    final int REQCODE_ACCESSORY = 105;
    private int evGrainSize = 3;
    private int evWidth = 3;
    private double flWideEnd = 31;
    private double flTeleEnd = 31;

    private FilmRoll filmroll;
    private Photo photo;
    private ArrayList<LensSpec> lenslist;
    LensAdapter lensadapter;
    ArrayAdapter<String> apertureAdapter;
    MaterialBetterSpinner apertureSpinner;
    MaterialEditText editDate;
    MaterialEditText editAccessories;
    TextView labelFocalLength;
    SeekBar sbFocalLength;
    MaterialBetterSpinner lensSpinner;
    MaterialBetterSpinner ssSpinner;
    MaterialEditText editLocation;
    MaterialEditText editMemo;
    SeekBar sbEV;
    SeekBar sbTTL;
    TextView currentCorrAmount, currentTTLLightMeter;
    ImageView getLocationButton, mountAdapterButton;

    private ArrayAdapter<String> ssAdapter;
    private int lensid;
    private LensSpec lens;
    private int id;
    private int index;
    private double latitude=999, longitude=999;
    private ArrayList<Integer> selectedAccessories;
    private boolean isDirty;

    protected void findViews(){
        apertureSpinner = findViewById(R.id.spinner_aperture);
        editDate = findViewById(R.id.edit_date);
        labelFocalLength = findViewById(R.id.label_focal_length);
        sbFocalLength = findViewById(R.id.seek_focal_length);
        lensSpinner = findViewById(R.id.spinner_lens);
        ssSpinner = findViewById(R.id.spinner_ss);
        editLocation = findViewById(R.id.edit_location);
        editMemo = findViewById(R.id.edit_memo);
        sbEV = findViewById(R.id.seek_exp_compensation);
        sbTTL = findViewById(R.id.seek_ttl_light_meter);
        currentCorrAmount = findViewById(R.id.label_ev);
        currentTTLLightMeter = findViewById(R.id.label_ttl);
        getLocationButton = findViewById(R.id.get_location);
        mountAdapterButton = findViewById(R.id.mount_adapters);
        editAccessories = findViewById(R.id.edit_accessories);
    }

    protected void updateLensList(int selectedId, TrisquelDao dao){
        if(filmroll.camera.type == 1){ //問答無用。selectedId等は完全無視
            lens = dao.getLens(dao.getFixedLensIdByBody(filmroll.camera.id));
            lensid = lens.id;
            lenslist = new ArrayList<LensSpec>();
            lenslist.add(lens);
            lensSpinner.setEnabled(false);
            mountAdapterButton.setEnabled(false);
            mountAdapterButton.setImageResource(R.drawable.ic_mount_adapter_disabled);
        }else{
            lenslist = dao.getLensesByMount(filmroll.camera.mount);
            for(String s: getSuggestListSubPref("mount_adapters", filmroll.camera.mount)){
                lenslist.addAll(dao.getLensesByMount(s));
            }
            if(selectedId > 0) {
                lens = dao.getLens(selectedId);
                if (lens != null && !this.filmroll.camera.mount.equals(lens.mount)) {
                    lenslist.add(0, lens);
                }
            }
        }
        lensadapter = new LensAdapter(this, android.R.layout.simple_spinner_item, lenslist);
        lensadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lensSpinner.setAdapter(lensadapter);
        if(lensadapter.getCount()==0){
            lensSpinner.setError(getString(R.string.error_nolens).replace("%s", filmroll.camera.mount));
        }

        if(selectedId > 0) {
            lensSpinner.setPosition(lensadapter.getPosition(selectedId));
        }else if(filmroll.camera.type == 1){
            lensSpinner.setPosition(0);
        }
    }

    private void setAccessories(TrisquelDao dao, ArrayList<Integer> accessories){
        selectedAccessories.clear();
        selectedAccessories.addAll(accessories);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(int id: accessories) {
            if(!first) sb.append(", ");
            Accessory a = dao.getAccessory(id);
            sb.append(a.getName());
            first = false;
        }
        editAccessories.setText(sb.toString());
    }

    protected void loadData(Intent data, TrisquelDao dao, Bundle savedInstanceState){
        this.id = data.getIntExtra("id", -1);
        this.index = data.getIntExtra("index", -1);
        int filmrollid = data.getIntExtra("filmroll", -1);
        this.filmroll = dao.getFilmRoll(filmrollid);
        this.photo = dao.getPhoto(id);
        if(photo != null){
            lensid = photo.lensid;
        } else {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean autocomplete = pref.getBoolean("autocomplete_from_previous_shot", false);
            if(autocomplete){
                ArrayList<Photo> ps = dao.getPhotosByFilmRollId(filmrollid);
                Photo lastPhoto;
                if(this.index > 0) {
                    lensid = ps.get(this.index - 1).lensid;
                }else if(this.index < 0 && ps.size() > 0){
                    lensid = ps.get(ps.size() - 1).lensid;
                }else{
                    lensid = -1;
                }
            }else{
                lensid = -1;
            }
        }

        if(photo != null && !photo.date.isEmpty()) {
            editDate.setText(photo.date);
        }else{
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            editDate.setText(sdf.format(calendar.getTime()));
        }

        updateLensList(lensid, dao);
        /*
        if(filmroll.camera.type == 1){
            lens = dao.getLens(dao.getFixedLensIdByBody(filmroll.camera.id));
            lensid = lens.id;
            lenslist = new ArrayList<LensSpec>();
            lenslist.add(lens);
            lensSpinner.setEnabled(false);
        }else{
            lens = dao.getLens(lensid);
            lenslist = dao.getLensesByMount(this.filmroll.camera.mount);
            //Javaなので左から評価されることは保証されている
            if(lens != null && !this.filmroll.camera.mount.equals(lens.mount)){
                lenslist.add(0, lens);
            }
        }
        */

        if(lens != null) {
            if (lens.focalLength.indexOf("-") > 0) {
                String[] range = lens.focalLength.split("-");
                flWideEnd = Double.parseDouble(range[0]);
                flTeleEnd = Double.parseDouble(range[1]);
            } else {
                flWideEnd = Double.parseDouble(lens.focalLength);
                flTeleEnd = flWideEnd;
            }
            refreshApertureAdapter(lens);
            refreshFocalLength(lens);
        }else{
            flWideEnd = 0;
            flTeleEnd = 0;
        }
        this.evGrainSize = filmroll.camera.evGrainSize;
        this.evWidth = filmroll.camera.evWidth;
        if(photo != null) {
            if(photo.shutterSpeed > 0) ssSpinner.setText(Util.doubleToStringShutterSpeed(photo.shutterSpeed));
            editLocation.setText(photo.location);
            editMemo.setText(photo.memo);
        }

        if(savedInstanceState != null) {
            setLatLng(savedInstanceState.getDouble("latitude"), savedInstanceState.getDouble("longitude"));
            setAccessories(dao, savedInstanceState.getIntegerArrayList("selected_accessories"));
        }else{
            if(photo != null){
                setLatLng(photo.latitude, photo.longitude);
                setAccessories(dao, photo.accessories);
            }else{
                setLatLng(999, 999);
            }
        }
    }

    protected void setEventListeners(){
        final AdapterView.OnItemClickListener oldListener = lensSpinner.getOnItemClickListener(); // これやらないとgetPositionがおかしくなる
        lensSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                lens = lenslist.get(i);
                lensid = lens.id;
                refreshApertureAdapter(lenslist.get(i));
                refreshFocalLength(lenslist.get(i));
                invalidateOptionsMenu();
                isDirty = true;
                oldListener.onItemClick(adapterView, view, i, l);
            }
        });

        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateDialogOnActivity();
            }
        });

        editDate.addTextChangedListener(new TextWatcher() {
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


        editAccessories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAccessoryDialogOnActivity();
            }
        });

        editAccessories.addTextChangedListener(new TextWatcher() {
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

        apertureSpinner.addTextChangedListener(new TextWatcher() {
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
        ssSpinner.addTextChangedListener(new TextWatcher() {
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

        sbEV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                BigDecimal bd = new BigDecimal((double)(i - evGrainSize*evWidth) / (double)evGrainSize);
                BigDecimal bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN);
                currentCorrAmount.setText( (bd2.signum() > 0? "+" : "") + bd2.toPlainString() + "EV");
                isDirty = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbTTL.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                BigDecimal bd = new BigDecimal((double)(i - evGrainSize*evWidth) / (double)evGrainSize);
                BigDecimal bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN);
                currentTTLLightMeter.setText( (bd2.signum() > 0? "+" : "") + bd2.toPlainString() + "EV");
                isDirty = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbFocalLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                labelFocalLength.setText(Double.toString(sbFocalLength.getProgress() + flWideEnd)+"mm");
                isDirty = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        editLocation.addTextChangedListener(new TextWatcher() {
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
        editMemo.addTextChangedListener(new TextWatcher() {
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
        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), MapsActivity.class);
                if(latitude != 999 && longitude != 999){
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                }
                startActivityForResult(intent, REQCODE_GET_LOCATION);
            }
        });
        mountAdapterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbstractDialogFragment fragment = new CheckListDialogFragment.Builder()
                        .build(REQCODE_MOUNT_ADAPTERS);
                TrisquelDao dao = new TrisquelDao(getApplicationContext());
                dao.connection();
                ArrayList<String> availableLensMounts = dao.getAvailableMountList();
                dao.close();

                String mount = filmroll.camera.mount;
                if(availableLensMounts.indexOf(mount) >= 0) {
                    availableLensMounts.remove(mount);
                }
                String[] availableLensMountsArray = availableLensMounts.toArray(new String[availableLensMounts.size()]);

                ArrayList<String> checkedMounts = getSuggestListSubPref("mount_adapters", mount);
                ArrayList<Integer> checkedIndices = new ArrayList<>();
                for(int i = 0; i < checkedMounts.size(); i++){
                    if(availableLensMounts.indexOf(checkedMounts.get(i)) >= 0){
                        checkedIndices.add(availableLensMounts.indexOf(checkedMounts.get(i)));
                    }
                }

                fragment.getArguments().putStringArray("items", availableLensMountsArray);
                fragment.getArguments().putIntegerArrayList("checked_indices", checkedIndices);
                fragment.getArguments().putString("title", getString(R.string.msg_select_mount_adapters).replace("%s", mount));
                fragment.getArguments().putString("positive", getString(android.R.string.yes));
                fragment.getArguments().putString("negative", getString(android.R.string.no));
                fragment.showOn(EditPhotoActivity.this, "dialog");
            }
        });
    }

    protected boolean canSave(){
        return lensSpinner.getPosition() >= 0;
    }

    protected void refreshApertureAdapter(LensSpec lens){
        if(lens == null){
            apertureAdapter.clear();
        }else {
            ArrayList<String> list = new ArrayList<String>();
            Double[] d = lens.fSteps;
            apertureAdapter.clear();
            for (int j = 0; j < d.length; j++) { //ダサい
                apertureAdapter.add(Double.toString(d[j]));
            }
        }
        apertureSpinner.setAdapter(apertureAdapter);
        apertureSpinner.setEnabled(true);
    }

    public void refreshFocalLength(LensSpec lens){
        if(lens == null){
            flWideEnd = 0;
            flTeleEnd = 0;
        }else {
            if (lens.focalLength.indexOf("-") > 0) {
                String[] range = lens.focalLength.split("-");
                flWideEnd = Double.parseDouble(range[0]);
                flTeleEnd = Double.parseDouble(range[1]);
            } else {
                flWideEnd = Double.parseDouble(lens.focalLength);
                flTeleEnd = flWideEnd;
            }
        }
        if(flWideEnd == flTeleEnd) {
            labelFocalLength.setText(Double.toString(flWideEnd) + "mm (" + getString(R.string.prime) + ")");
            sbFocalLength.setVisibility(View.GONE);
        }else{
            labelFocalLength.setText(Double.toString(flWideEnd)+"mm");
            //labelFocalLength.setText(Double.toString(flWideEnd) + "-" + Double.toString(flTeleEnd) + "mm");
            sbFocalLength.setVisibility(View.VISIBLE);
        }
        sbFocalLength.setMax((int)(flTeleEnd - flWideEnd)); // API Level 26以降ならsetMinが使えるのだが…
    }

    protected ArrayList<String> getSuggestListSubPref(String parentkey, String subkey){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefstr = pref.getString(parentkey, "{}");
        ArrayList<String> strArray = new ArrayList<String>();
        try {
            JSONObject obj = new JSONObject(prefstr);
            JSONArray array = obj.getJSONArray(subkey);
            if(array == null) return strArray;
            for(int i = 0; i < array.length(); i++){
                strArray.add(array.getString(i));
            }
        }catch(JSONException e){}
        return strArray;
    }

    protected void saveSuggestListSubPref(String parentkey, String subkey, ArrayList<String> values){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefstr = pref.getString(parentkey, "{}");
        JSONObject obj = null;
        try {
            JSONArray array;
            obj = new JSONObject(prefstr);
        }catch(JSONException e){
            obj = new JSONObject();
        }

        JSONArray result = new JSONArray(values);
        SharedPreferences.Editor e = pref.edit();
        try {
            obj.put(subkey, result);
            e.putString(parentkey, obj.toString());
            e.apply();
        }catch (JSONException e1){}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photo);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        findViews();

        editLocation.setHelperTextColor(R.color.common_google_signin_btn_text_light_default);

        TrisquelDao dao = new TrisquelDao(getApplicationContext());
        dao.connection();
        Intent data = getIntent();

        apertureAdapter = new ArrayAdapter<String>(this, /* これでいいのか？ */
                android.R.layout.simple_dropdown_item_1line);
        apertureSpinner.setEnabled(false);

        selectedAccessories = new ArrayList<Integer>();

        lensid = -1;
        lens = null;
        if (data != null) {
            loadData(data, dao, savedInstanceState);
        }else {
            this.id = -1;
            lenslist = dao.getAllLenses();
            lensadapter = new LensAdapter(this, android.R.layout.simple_spinner_item, lenslist);
            lensadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            lensSpinner.setAdapter(lensadapter);
            if(lensadapter.getCount()==0){
                lensSpinner.setError(getString(R.string.error_nolens).replace("%s", filmroll.camera.mount));
            }
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            editDate.setText(sdf.format(calendar.getTime()));
        }
        dao.close();

        String[] ssArray;
        switch(filmroll.camera.shutterSpeedGrainSize){
            case 1:
                ssArray = getResources().getStringArray(R.array.shutter_speeds_one);
                break;
            case 2:
                ssArray = getResources().getStringArray(R.array.shutter_speeds_half);
                break;
            default:
                ssArray = getResources().getStringArray(R.array.shutter_speeds_one_third);
                break;
        }
        List<String> ssList = new ArrayList<>(Arrays.asList(ssArray));
        for(int i = ssList.size()-1; i >= 0; i--){
            Double ssval = Util.stringToDoubleShutterSpeed(ssList.get(i));
            if(ssval > filmroll.camera.slowestShutterSpeed || ssval < filmroll.camera.fastestShutterSpeed){
                ssList.remove(i);
            }
        }

        ssAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line ,ssList.toArray(new String[ssList.size()]));
        ssAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ssSpinner.setAdapter(ssAdapter);

        /*if(lensid > 0) {
            refreshApertureAdapter(lens);
            refreshFocalLength(lens);
        }*/

        if(photo != null){
            if(photo.aperture > 0) apertureSpinner.setText(Util.doubleToStringShutterSpeed(photo.aperture)); //これもごまかし
        }else{
            // 写ルンです（シャッタースピード・絞り固定）を考慮
            if(ssAdapter.getCount() == 1){ ssSpinner.setPosition(0); }
            if(apertureAdapter.getCount() == 1){ apertureSpinner.setPosition(0); }
        }

        sbEV.setMax(evWidth * 2 * evGrainSize);
        sbTTL.setMax(evWidth * 2 * evGrainSize);
        if(photo != null){
            if(flTeleEnd != flWideEnd) {
                sbFocalLength.setProgress((int)(photo.focalLength - flWideEnd));
                labelFocalLength.setText(Double.toString(photo.focalLength) + "mm");
            }

            sbEV.setProgress((int)((evWidth + photo.expCompensation) * evGrainSize));
            BigDecimal bd = new BigDecimal(photo.expCompensation);
            BigDecimal bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN);
            currentCorrAmount.setText( (bd2.signum() > 0? "+" : "") + bd2.toPlainString() + "EV");

            sbTTL.setProgress((int)((evWidth + photo.ttlLightMeter) * evGrainSize));
            BigDecimal bd3 = new BigDecimal(photo.ttlLightMeter);
            BigDecimal bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN);
            currentTTLLightMeter.setText( (bd4.signum() > 0? "+" : "") + bd4.toPlainString() + "EV");
        }else{
            sbEV.setProgress(evWidth * evGrainSize);
            sbTTL.setProgress(evWidth * evGrainSize);
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
        outState.putDouble("latitude", latitude);
        outState.putDouble("longitude", longitude);
        outState.putInt("lensid", lensid);
        outState.putIntegerArrayList("selected_accessories", selectedAccessories);
        super.onSaveInstanceState(outState);
    }

    void showDateDialogOnActivity() {
        new DatePickerFragment.Builder()
                .build(REQCODE_DATE)
                .showOn(this, "dialog");
    }

    void showAccessoryDialogOnActivity() {
        AbstractDialogFragment fragment = new CheckListDialogFragment.Builder()
                .build(REQCODE_ACCESSORY);
        TrisquelDao dao = new TrisquelDao(getApplicationContext());
        dao.connection();
        ArrayList<Accessory> accessories = dao.getAccessories();
        dao.close();

        ArrayList<String> a_str = new ArrayList<>();
        ArrayList<Integer> chkidx = new ArrayList<>();
        ArrayList<Integer> tags = new ArrayList<>();
        for(int i = 0; i < accessories.size(); i++){
            Accessory a = accessories.get(i);
            a_str.add(a.getName());
            if(selectedAccessories.contains(a.getId())){
                chkidx.add(i);
            }
            tags.add(a.getId());
        }

        fragment.getArguments().putStringArray("items", a_str.toArray(new String[a_str.size()]));
        fragment.getArguments().putIntegerArrayList("tags", tags);
        fragment.getArguments().putIntegerArrayList("checked_indices", chkidx);
        fragment.getArguments().putString("title", "a");
        fragment.getArguments().putString("positive", getString(android.R.string.yes));
        fragment.getArguments().putString("negative", getString(android.R.string.no));
        fragment.showOn(EditPhotoActivity.this, "dialog");
    }

    private boolean sameArrayList(ArrayList<Integer> list1, ArrayList<Integer> list2){
        if(list1.size() != list2.size()) return false;
        for(int i = 0; i < list1.size(); i++){
            if(!list1.get(i).equals(list2.get(i))) return false;
        }
        return true;
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case REQCODE_DATE:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        int year = data.getIntExtra(DatePickerFragment.EXTRA_YEAR, 1970);
                        int month = data.getIntExtra(DatePickerFragment.EXTRA_MONTH, 0);
                        int day = data.getIntExtra(DatePickerFragment.EXTRA_DAY, 0);
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month);
                        c.set(Calendar.DAY_OF_MONTH, day);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                        editDate.setText(sdf.format(c.getTime()));
                    }
                }
                break;
            case 101:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    Intent resultData = getData();
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
            case REQCODE_MOUNT_ADAPTERS:
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    Bundle bundle = data.getExtras();
                    saveSuggestListSubPref("mount_adapters",
                            filmroll.camera.mount,
                            bundle.getStringArrayList("checked_items"));
                    TrisquelDao dao = new TrisquelDao(getApplicationContext());
                    dao.connection();
                    int id = -1;
                    if(lensSpinner.getPosition() >= 0){
                        id = lenslist.get(lensSpinner.getPosition()).id;
                    }
                    updateLensList(id, dao);
                    dao.close();
                    lensSpinner.clearFocus(); //こうしないとドロップダウンリストが更新されない…気がする。バグか？
                }
                break;
            case REQCODE_ACCESSORY:
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        ArrayList<Integer> tags = bundle.getIntegerArrayList("checked_tags");
                        if(!sameArrayList(tags, selectedAccessories)) {
                            TrisquelDao dao = new TrisquelDao(getApplicationContext());
                            dao.connection();
                            setAccessories(dao, tags);
                            isDirty = true;
                            dao.close();
                        }
                    }
                }
                break;
            default:
        }
    }

    @Override
    public void onDialogCancelled(int requestCode) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        menu.findItem(R.id.menu_save).setEnabled(canSave());
        return true;
    }

    public Intent getData(){
        Intent data = new Intent();
        data.putExtra("id", id);
        data.putExtra("index", index);
        data.putExtra("filmroll", filmroll.id);
        data.putExtra("date", editDate.getText().toString());
        data.putExtra("lens", lenslist.get(lensSpinner.getPosition()).id);
        data.putExtra("focal_length", (sbFocalLength.getProgress() + flWideEnd));
        Double a;
        try{
            a = Double.parseDouble(apertureSpinner.getText().toString());
        }catch (NumberFormatException e){
            a = 0.0;
        }
        data.putExtra("aperture", a);
        data.putExtra("shutter_speed", Util.stringToDoubleShutterSpeed(ssSpinner.getText().toString()));

        BigDecimal bd = new BigDecimal((double)(sbEV.getProgress() - evGrainSize*evWidth) / (double)evGrainSize);
        BigDecimal bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN);
        data.putExtra("exp_compensation", Double.parseDouble(bd2.toPlainString()));

        BigDecimal bd3 = new BigDecimal((double)(sbTTL.getProgress() - evGrainSize*evWidth) / (double)evGrainSize);
        BigDecimal bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN);
        data.putExtra("ttl_light_meter", Double.parseDouble(bd4.toPlainString()));

        data.putExtra("location", editLocation.getText().toString());
        data.putExtra("latitude", latitude);
        data.putExtra("longitude", longitude);
        data.putExtra("memo", editMemo.getText().toString());

        StringBuilder sb = new StringBuilder("/");
        for (Integer accessory: selectedAccessories) {
            sb.append(accessory);
            sb.append('/');
        }
        data.putExtra("accessories", sb.toString());
        return data;
    }

    private double getExpCompensation(){
        BigDecimal bd = new BigDecimal((double)(sbEV.getProgress() - evGrainSize*evWidth) / (double)evGrainSize);
        BigDecimal bd2 = bd.setScale(1, BigDecimal.ROUND_DOWN);
        return Double.parseDouble(bd2.toPlainString());
    }

    private double getTtlLightMeter(){
        BigDecimal bd3 = new BigDecimal((double)(sbTTL.getProgress() - evGrainSize*evWidth) / (double)evGrainSize);
        BigDecimal bd4 = bd3.setScale(1, BigDecimal.ROUND_DOWN);
        return Double.parseDouble(bd4.toPlainString());
    }

    private String getPhotoText(){
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.label_date) + ": " + editDate.getText().toString() + "\n");
        if(lensSpinner.getPosition() >= 0) {
            LensSpec l = lenslist.get(lensSpinner.getPosition());
            sb.append(getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n");
            if (apertureSpinner.getText().length() > 0)
                sb.append(getString(R.string.label_aperture) + ": " + apertureSpinner.getText() + "\n");
            if (ssSpinner.getText().length() > 0)
                sb.append(getString(R.string.label_shutter_speed) + ": " + ssSpinner.getText().toString() + "\n");
            if (getExpCompensation() != 0)
                sb.append(getString(R.string.label_exposure_compensation) + ": " + getExpCompensation() + "\n");
            if (getTtlLightMeter() != 0)
                sb.append(getString(R.string.label_ttl_light_meter) + ": " + getTtlLightMeter() + "\n");
            if (editLocation.getText().length() > 0)
                sb.append(getString(R.string.label_location) + ": " + editLocation.getText().toString() + "\n");
            if (latitude != 999 && longitude != 999)
                sb.append(getString(R.string.label_coordinate) + ": " + Double.toString(latitude)+", "+Double.toString(longitude) + "\n");
            if (editMemo.getText().length() > 0)
                sb.append(getString(R.string.label_memo) + ": " + editMemo.getText().toString() + "\n");
            if (editAccessories.getText().length() > 0)
                sb.append(getString(R.string.label_accessories) + ": " + editAccessories.getText().toString() + "\n");
        }
        return sb.toString();
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
                finish();
                return true;
            case R.id.menu_copy:
                String s = getPhotoText();
                if(s.isEmpty()) { return true; }
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if(cm == null) return true;
                cm.setPrimaryClip(ClipData.newPlainText("", s));
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        Log.d("onBackPressed", "EditPhotoActivity");
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

    private void setLatLng(double newlatitude, double newlongitude){
        StringBuilder sb = new StringBuilder();
        if(newlatitude == 999 || newlongitude == 999){
            latitude = 999;
            longitude = 999;
            editLocation.setHelperText(null);
            editLocation.setHelperTextAlwaysShown(false);
            getLocationButton.setImageResource(R.drawable.ic_place_gray_24dp);
        }else{
            latitude = newlatitude;
            longitude = newlongitude;
            editLocation.setHelperText(
                    getString(R.string.label_coordinate) +
                    ": "+ Double.toString(newlatitude) + ", " + Double.toString(newlongitude));
            editLocation.setHelperTextAlwaysShown(true);
            getLocationButton.setImageResource(R.drawable.ic_place_black_24dp);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQCODE_GET_LOCATION:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    double newlat, newlog;
                    newlat = bundle.getDouble("latitude");
                    newlog = bundle.getDouble("longitude");
                    if(newlat != latitude || newlog != longitude) isDirty = true;
                    setLatLng(newlat, newlog);
                    editLocation.setText(bundle.getString("location"));
                }else if (resultCode == MapsActivity.RESULT_DELETE){
                    if(999 != latitude || 999 != longitude) isDirty = true;
                    setLatLng(999, 999);
                }
                break;
        }
    }
}
