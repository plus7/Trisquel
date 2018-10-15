package net.tnose.app.trisquel;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

public class EditPhotoListActivity extends AppCompatActivity
        implements PhotoFragment.OnListFragmentInteractionListener,
        AbstractDialogFragment.Callback {
    final int REQCODE_ADD_PHOTO = 100;
    final int REQCODE_EDIT_PHOTO = 101;
    final int REQCODE_EDIT_FILMROLL = 102;

    private Toolbar toolbar;
    private FilmRoll mFilmRoll;
    private PhotoFragment photo_fragment;
    private TextView namelabel, cameralabel, brandlabel;

    protected void findViews(){
        namelabel = findViewById(R.id.label_name);
        cameralabel = findViewById(R.id.label_camera);
        brandlabel = findViewById(R.id.label_filmbrand);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photo_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        int id = -1;
        Intent data = getIntent();
        if (data != null) {
            id = data.getIntExtra("id", -1);
        }

        findViews();

        TrisquelDao dao = new TrisquelDao(getApplicationContext());
        dao.connection();
        mFilmRoll = dao.getFilmRoll(id);
        dao.close();

        if(mFilmRoll.name.isEmpty()) {
            namelabel.setText(R.string.empty_name);
            namelabel.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
            setTitle(R.string.empty_name);
        }else{
            namelabel.setText(mFilmRoll.name);
            namelabel.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            setTitle(mFilmRoll.name);
        }
        cameralabel.setText(mFilmRoll.camera.manufacturer + " " + mFilmRoll.camera.modelName);
        brandlabel.setText(mFilmRoll.manufacturer + " " + mFilmRoll.brand);
        toolbar.setSubtitle(
                mFilmRoll.camera.manufacturer + " " + mFilmRoll.camera.modelName + " / " +
                mFilmRoll.manufacturer + " " + mFilmRoll.brand);

        LinearLayout filmroll_layout = findViewById(R.id.layout_filmroll);
        filmroll_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplication(), EditFilmRollActivity.class);
                intent.putExtra("id", mFilmRoll.id);
                startActivityForResult(intent, REQCODE_EDIT_FILMROLL);
            }
        });

        photo_fragment = PhotoFragment.newInstance(1, id);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, photo_fragment);
        transaction.commit();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplication(), EditPhotoActivity.class);
                intent.putExtra("filmroll", mFilmRoll.id);
                startActivityForResult(intent, REQCODE_ADD_PHOTO);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_filmroll, menu);
        return true;
    }

    private String getFilmRollText(){
        StringBuilder sb = new StringBuilder();
        sb.append(mFilmRoll.name + "\n");
        if(mFilmRoll.manufacturer.length() > 0) sb.append(getString(R.string.label_manufacturer) + ": " + mFilmRoll.manufacturer + "\n");
        if(mFilmRoll.brand.length() > 0)         sb.append(getString(R.string.label_brand) + ": " + mFilmRoll.brand + "\n");
        if(mFilmRoll.iso > 0)                    sb.append(getString(R.string.label_iso) + ": "); sb.append(mFilmRoll.iso); sb.append('\n');
        CameraSpec c = mFilmRoll.camera;
        sb.append(getString(R.string.label_camera) + ": " + c.manufacturer + " "+ c.modelName + "\n");

        TrisquelDao dao = new TrisquelDao(this);
        dao.connection();
        ArrayList<Photo> ps = dao.getPhotosByFilmRollId(mFilmRoll.id);
        for(int i = 0; i < ps.size(); i++) {
            Photo p = ps.get(i);
            LensSpec l = dao.getLens(p.lensid);
            sb.append("------[No. " + (p.index+1) + "]------\n");
            sb.append(getString(R.string.label_date) + ": " + p.date + "\n");
            sb.append(getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n");
            if(p.aperture > 0) sb.append(getString(R.string.label_aperture) +  ": " + p.aperture + "\n");
            if(p.shutterSpeed > 0) sb.append(getString(R.string.label_shutter_speed) +  ": " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "\n");
            if(p.expCompensation != 0) sb.append(getString(R.string.label_exposure_compensation) +  ": " + p.expCompensation + "\n");
            if(p.ttlLightMeter != 0) sb.append(getString(R.string.label_ttl_light_meter) +  ": " + p.ttlLightMeter + "\n");
            if(p.location.length() > 0) sb.append(getString(R.string.label_location) +  ": " + p.location + "\n");
            if (p.latitude != 999 && p.longitude != 999) sb.append(getString(R.string.label_coordinate) + ": " + Double.toString(p.latitude)+", "+Double.toString(p.longitude) + "\n");
            if(p.memo.length() > 0) sb.append(getString(R.string.label_memo) +  ": " + p.memo + "\n");
            if(p.accessories.size() > 0) {
                sb.append(getString(R.string.label_accessories) + ": ");
                boolean first = true;
                for(int a: p.accessories){
                    if(!first) sb.append(", ");
                    sb.append(dao.getAccessory(a).getName());
                    first = false;
                }
                sb.append("\n");
            }
        }
        dao.close();
        return sb.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent data;
        switch (item.getItemId()) {
            case android.R.id.home:
                data = new Intent();
                data.putExtra("filmroll", this.mFilmRoll.id);
                setResult(RESULT_OK, data);
                finish();
                return true;
            case R.id.menu_edit_film:
                Intent intent = new Intent(getApplication(), EditFilmRollActivity.class);
                intent.putExtra("id", mFilmRoll.id);
                startActivityForResult(intent, REQCODE_EDIT_FILMROLL);
                return true;
            case R.id.menu_copy:
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if(cm == null) return true;
                cm.setPrimaryClip(ClipData.newPlainText("", getFilmRollText()));
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_export_pdf:
                intent = new Intent(getApplication(), PrintPreviewActivity.class);
                intent.putExtra("filmroll", mFilmRoll.id);
                startActivity(intent);
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed(){
        Log.d("onBackPressed", "java");
        Intent data = new Intent();
        data.putExtra("filmroll", this.mFilmRoll.id);
        setResult(RESULT_OK, data);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQCODE_ADD_PHOTO:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    Photo p = new Photo(
                            -1,
                            bundle.getInt("filmroll"),
                            bundle.getInt("index"),
                            bundle.getString("date"),
                            bundle.getInt("camera"),
                            bundle.getInt("lens"),
                            bundle.getDouble("focal_length"),
                            bundle.getDouble("aperture"),
                            bundle.getDouble("shutter_speed"),
                            bundle.getDouble("exp_compensation"),
                            bundle.getDouble("ttl_light_meter"),
                            bundle.getString("location"),
                            bundle.getDouble("latitude"),
                            bundle.getDouble("longitude"),
                            bundle.getString("memo"),
                            bundle.getString("accessories"));
                    photo_fragment.insertPhoto(p);
                }
                break;
            case REQCODE_EDIT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    Log.d("ActivityResult: lens", Integer.toString(bundle.getInt("lens")));
                    Photo p = new Photo(
                            bundle.getInt("id"),
                            bundle.getInt("filmroll"),
                            bundle.getInt("index"),
                            bundle.getString("date"),
                            bundle.getInt("camera"),
                            bundle.getInt("lens"),
                            bundle.getDouble("focal_length"),
                            bundle.getDouble("aperture"),
                            bundle.getDouble("shutter_speed"),
                            bundle.getDouble("exp_compensation"),
                            bundle.getDouble("ttl_light_meter"),
                            bundle.getString("location"),
                            bundle.getDouble("latitude"),
                            bundle.getDouble("longitude"),
                            bundle.getString("memo"),
                            bundle.getString("accessories"));
                    photo_fragment.updatePhoto(p);
                }
                break;
            case REQCODE_EDIT_FILMROLL:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    TrisquelDao dao = new TrisquelDao(this.getApplicationContext());
                    dao.connection();
                    CameraSpec c = dao.getCamera(bundle.getInt("camera"));
                    FilmRoll f = new FilmRoll(
                            bundle.getInt("id"),
                            bundle.getString("name"),
                            bundle.getString("created"),
                            Util.dateToStringUTC(new Date()),
                            c,
                            bundle.getString("manufacturer"),
                            bundle.getString("brand"),
                            bundle.getInt("iso"),
                            36
                    );

                    if(f.name.isEmpty()) {
                        namelabel.setText(R.string.empty_name);
                        namelabel.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
                        setTitle(R.string.empty_name);
                    }else{
                        namelabel.setText(f.name);
                        namelabel.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        setTitle(f.name);
                    }
                    cameralabel.setText(f.camera.manufacturer + " " + f.camera.modelName);
                    brandlabel.setText(f.manufacturer + " " + f.brand);
                    toolbar.setSubtitle(
                            f.camera.manufacturer + " " + f.camera.modelName + " / " +
                                    f.manufacturer + " " + f.brand);
                    //TODO:
                    dao.updateFilmRoll(f);
                    dao.close();
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            default:
        }
    }

    public void onListFragmentInteraction(Photo item, boolean isLong){
        if(isLong){
            AbstractDialogFragment fragment = new SelectDialogFragment.Builder()
                    .build(200);
            fragment.getArguments().putInt("id", item.id);
            fragment.getArguments().putStringArray("items", new String[]{getString(R.string.delete), getString(R.string.add_photo_above)});
            fragment.showOn(this, "dialog");
        }else {
            Intent intent = new Intent(getApplication(), EditPhotoActivity.class);
            intent.putExtra("filmroll", mFilmRoll.id);
            intent.putExtra("id", item.id);
            intent.putExtra("index", item.index);
            startActivityForResult(intent, REQCODE_EDIT_PHOTO);
        }
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case 200:
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        int which, id, index;
                        which = data.getIntExtra("which", -1);
                        id = data.getIntExtra("id", -1);
                        TrisquelDao dao = new TrisquelDao(this.getApplicationContext());
                        dao.connection();
                        Photo p = dao.getPhoto(id);
                        dao.close();
                        index = p.index;
                        switch(which){
                            case 0:
                                if(id != -1) photo_fragment.deletePhoto(id);
                                break;
                            case 1:
                                Intent intent = new Intent(getApplication(), EditPhotoActivity.class);
                                intent.putExtra("filmroll", mFilmRoll.id);
                                intent.putExtra("index", index);
                                startActivityForResult(intent, REQCODE_ADD_PHOTO);
                                break;
                        }
                        Log.d("PHOTOLIST_SELECTION", Integer.toString(which));
                    }
                }
                break;
        }
    }

    @Override
    public void onDialogCancelled(int requestCode) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
