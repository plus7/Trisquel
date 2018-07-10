package net.tnose.app.trisquel;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        CameraFragment.OnListFragmentInteractionListener,
        LensFragment.OnListFragmentInteractionListener,
        FilmRollFragment.OnListFragmentInteractionListener,
        EmptyFragment.OnFragmentInteractionListener,
        AbstractDialogFragment.Callback{

    public final int REQCODE_EDIT_CAMERA = 1;
    public final int REQCODE_ADD_CAMERA = 2;
    public final int REQCODE_EDIT_LENS = 3;
    public final int REQCODE_ADD_LENS = 4;
    public final int REQCODE_EDIT_FILMROLL = 5;
    public final int REQCODE_ADD_FILMROLL = 6;
    public final int REQCODE_EDIT_PHOTO_LIST = 7;
    public final int REQCODE_BACKUP_DIR_CHOSEN = 8;

    public final int RETCODE_CAMERA_TYPE = 300;
    public final int RETCODE_OPEN_RELEASE_NOTES = 100;
    public final int RETCODE_DELETE_FILMROLL  = 101;
    public final int RETCODE_DELETE_CAMERA  = 102;
    public final int RETCODE_DELETE_LENS  = 103;
    public final int RETCODE_BACKUP_DB = 104;
    public final int RETCODE_SDCARD_PERM = 200;

    public final String RELEASE_NOTES_URL = "http://pentax.tnose.net/tag/trisquel_releasenotes/";
    //public final int REQCODE_ADD_PHOTO_LIST = 8;

    private FilmRollFragment filmroll_fragment;
    private CameraFragment cam_fragment;
    private LensFragment lens_fragment;
    private EmptyFragment empty_fragment;
    private int currentFragment; //0: filmroll, 1: cam, 2: lens

    final String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(savedInstanceState != null){
            currentFragment = savedInstanceState.getInt("current_fragment");
        }else{
            currentFragment = 0;
        }

        Fragment f;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (currentFragment){
            case 1:
                cam_fragment = new CameraFragment();
                f = cam_fragment;
                setTitle(R.string.title_activity_cam_list);
                break;
            case 2:
                lens_fragment = new LensFragment();
                f = lens_fragment;
                setTitle(R.string.title_activity_lens_list);
                break;
            default:
                filmroll_fragment = new FilmRollFragment();
                f = filmroll_fragment;
                setTitle(R.string.title_activity_filmroll_list);
        }
        //addではなくreplaceでないとonCreateが再び呼ばれたときに変になる（以前作ったfragmentの残骸が残って表示される）
        //この辺の処理は画面回転なども考えるとよろしくないが先送りする
        transaction.replace(R.id.container, f);
        transaction.commit();

        FloatingActionButton fab = findViewById(R.id.fab);
        switch (currentFragment){
            case 1:
                fab.setImageResource(R.drawable.ic_menu_camera_white);
                break;
            case 2:
                fab.setImageResource(R.drawable.ic_lens_white);
                break;
            default:
                fab.setImageResource(R.drawable.ic_filmroll_vector_white);
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                switch(currentFragment){
                    case 1:
                        AbstractDialogFragment fragment = new SelectDialogFragment.Builder()
                                .build(RETCODE_CAMERA_TYPE);
                        fragment.getArguments().putInt("id", -1); //dummy value
                        fragment.getArguments().putStringArray("items", new String[]{getString(R.string.register_ilc), getString(R.string.register_flc)});
                        fragment.showOn(MainActivity.this, "dialog");
                        break;
                    case 2:
                        intent = new Intent(getApplication(), EditLensActivity.class);
                        startActivityForResult(intent, REQCODE_ADD_LENS);
                        break;
                    default:
                        intent = new Intent(getApplication(), EditFilmRollActivity.class);
                        startActivityForResult(intent, REQCODE_ADD_FILMROLL);
                }
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int lastVersion = pref.getInt("last_version", 0);
        if(lastVersion < Util.TRISQUEL_VERSION){
            AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                    .build(RETCODE_OPEN_RELEASE_NOTES);
            fragment.getArguments().putString("title", "Trisquel");
            if(lastVersion != 0) {
                fragment.getArguments().putString("message", getString(R.string.warning_newversion));
            }else{
                fragment.getArguments().putString("message", getString(R.string.warning_firstrun));
            }
            fragment.getArguments().putString("positive", getString(R.string.show_release_notes));
            fragment.getArguments().putString("negative", getString(R.string.close));
            fragment.showOn(this, "dialog");
        }
        SharedPreferences.Editor e = pref.edit();
        e.putInt("last_version", Util.TRISQUEL_VERSION);
        e.apply();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_fragment", currentFragment);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplication(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }else if(id == R.id.action_release_notes) {
            Uri uri = Uri.parse(RELEASE_NOTES_URL); // + Integer.toString(Util.TRISQUEL_VERSION)
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(i);
        }else if(id == R.id.action_backup_sqlite){
            AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                    .build(RETCODE_BACKUP_DB);
            fragment.getArguments().putString("title", getString(R.string.title_backup));
            fragment.getArguments().putString("message", getString(R.string.description_backup));
            fragment.getArguments().putString("positive", getString(R.string.continue_));
            fragment.showOn(this, "dialog");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQCODE_ADD_CAMERA:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    CameraSpec c = new CameraSpec(
                            -1,
                            bundle.getInt("type"),
                            bundle.getString("mount"),
                            bundle.getString("manufacturer"),
                            bundle.getString("model_name"),
                            bundle.getInt("format"),
                            bundle.getInt("ss_grain_size"),
                            bundle.getDouble("fastest_ss"),
                            bundle.getDouble("slowest_ss"),
                            bundle.getInt("bulb_available") != 0,
                            "",
                            bundle.getInt("ev_grain_size"),
                            bundle.getInt("ev_width"));
                    cam_fragment.insertCamera(c);
                    if(c.type == 1){
                        LensSpec l = new LensSpec(
                                -1,
                                "",
                                c.id,
                                bundle.getString("manufacturer"),
                                bundle.getString("fixedlens_name"),
                                bundle.getString("fixedlens_focal_length"),
                                bundle.getString("fixedlens_f_steps")
                        );
                        TrisquelDao dao = new TrisquelDao(this);
                        dao.connection();
                        long id = dao.addLens(l);
                        dao.close();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_EDIT_CAMERA:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    CameraSpec c = new CameraSpec(
                            bundle.getInt("id"),
                            bundle.getInt("type"),
                            bundle.getString("created"),
                            Util.dateToStringUTC(new Date()),
                            bundle.getString("mount"),
                            bundle.getString("manufacturer"),
                            bundle.getString("model_name"),
                            bundle.getInt("format"),
                            bundle.getInt("ss_grain_size"),
                            bundle.getDouble("fastest_ss"),
                            bundle.getDouble("slowest_ss"),
                            bundle.getInt("bulb_available") != 0,
                            "",
                            bundle.getInt("ev_grain_size"),
                            bundle.getInt("ev_width"));
                    cam_fragment.updateCamera(c);
                    if(c.type == 1){
                        TrisquelDao dao = new TrisquelDao(this);
                        dao.connection();
                        int lensid = dao.getFixedLensIdByBody(c.id);
                        LensSpec l = new LensSpec(
                                lensid,
                                "",
                                c.id,
                                bundle.getString("manufacturer"),
                                bundle.getString("fixedlens_name"),
                                bundle.getString("fixedlens_focal_length"),
                                bundle.getString("fixedlens_f_steps")
                        );
                        dao.updateLens(l);
                        dao.close();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_ADD_LENS:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    LensSpec l = new LensSpec(
                            -1,
                            bundle.getString("mount"),
                            0,
                            bundle.getString("manufacturer"),
                            bundle.getString("model_name"),
                            bundle.getString("focal_length"),
                            bundle.getString("f_steps")
                    );
                    Log.d("new lens", l.toString());
                    lens_fragment.insertLens(l);
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_EDIT_LENS:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    LensSpec l = new LensSpec(
                            bundle.getInt("id"),
                            bundle.getString("created"),
                            Util.dateToStringUTC(new Date()),
                            bundle.getString("mount"),
                            0,
                            bundle.getString("manufacturer"),
                            bundle.getString("model_name"),
                            bundle.getString("focal_length"),
                            bundle.getString("f_steps")
                    );
                    //Log.d("new lens", l.toString());
                    lens_fragment.updateLens(l);
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_ADD_FILMROLL:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    TrisquelDao dao = new TrisquelDao(this.getApplicationContext());
                    dao.connection();
                    CameraSpec c = dao.getCamera(bundle.getInt("camera"));
                    dao.close();
                    FilmRoll f = new FilmRoll(
                            -1,
                            bundle.getString("name"),
                            c,
                            bundle.getString("manufacturer"),
                            bundle.getString("brand"),
                            bundle.getInt("iso"),
                            36
                    );
                    filmroll_fragment.insertFilmRoll(f);
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_EDIT_FILMROLL:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    TrisquelDao dao = new TrisquelDao(this.getApplicationContext());
                    dao.connection();
                    CameraSpec c = dao.getCamera(bundle.getInt("camera"));
                    dao.close();
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
                    filmroll_fragment.updateFilmRoll(f);
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_EDIT_PHOTO_LIST:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    filmroll_fragment.refreshFilmRoll(bundle.getInt("filmroll"));
                } else if (resultCode == RESULT_CANCELED) {
                }
                break;
            case REQCODE_BACKUP_DIR_CHOSEN:
                if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                    Bundle bundle = data.getExtras();
                    String dir = bundle.getString(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                    File sd = new File(dir);
                    File dbpath = this.getDatabasePath("trisquel.db");

                    if (sd.canWrite()) {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        File backupDB = new File(sd, "trisquel-" + sdf.format(calendar.getTime()) + ".db");

                        if (dbpath.exists()) {
                            try {
                                FileChannel src = new FileInputStream(dbpath).getChannel();
                                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                                dst.transferFrom(src, 0, src.size());
                                src.close();
                                dst.close();
                            }catch(FileNotFoundException e){
                                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                return;
                            }catch (IOException e){
                                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                return;
                            }
                            Toast.makeText(this, "Wrote to " + backupDB.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FloatingActionButton fab = findViewById(R.id.fab);
        if (id == R.id.nav_camera) {
            cam_fragment = new CameraFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, cam_fragment);
            transaction.commit();
            setTitle(R.string.title_activity_cam_list);
            fab.setImageResource(R.drawable.ic_menu_camera_white);
            currentFragment  = 1;
        } else if (id == R.id.nav_lens) {
            lens_fragment = new LensFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, lens_fragment);
            transaction.commit();
            setTitle(R.string.title_activity_lens_list);
            fab.setImageResource(R.drawable.ic_lens_white);
            currentFragment  = 2;
        } else if (id == R.id.nav_filmrolls) {
            filmroll_fragment = new FilmRollFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, filmroll_fragment);
            transaction.commit();
            setTitle(R.string.title_activity_filmroll_list);
            fab.setImageResource(R.drawable.ic_filmroll_vector_white);
            currentFragment  = 0;
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public void onListFragmentInteraction(CameraSpec camera, boolean isLong){
        if(isLong){
            TrisquelDao dao = new TrisquelDao(getApplicationContext());
            dao.connection();
            long usedCount = dao.getCameraUsageCount(camera.id);
            dao.close();
            if(usedCount>0){
                AbstractDialogFragment fragment = new AlertDialogFragment.Builder().build(99);
                fragment.getArguments().putString("title", "カメラを削除できません");
                fragment.getArguments().putString("message",
                        camera.modelName + "は既存のフィルム記録から参照されているため、削除することができません。");
                fragment.showOn(this, "dialog");
            }else {
                AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_CAMERA);
                fragment.getArguments().putString("title", "カメラの削除");
                fragment.getArguments().putString("message", camera.modelName + "を削除しますか？この操作は元に戻せません！");
                fragment.getArguments().putInt("id", camera.id);
                fragment.showOn(this, "dialog");
            }
        }else {
            Intent intent = new Intent(getApplication(), EditCameraActivity.class);
            Log.d("camera", "type="+Integer.toString(camera.type));
            intent.putExtra("id", camera.id);
            intent.putExtra("type", camera.type);
            startActivityForResult(intent, REQCODE_EDIT_CAMERA);
        }
    }
    public void onListFragmentInteraction(LensSpec lens, boolean isLong){
        if(isLong){

            TrisquelDao dao = new TrisquelDao(getApplicationContext());
            dao.connection();
            long usedCount = dao.getLensUsageCount(lens.id);
            dao.close();
            if(usedCount>0){
                AbstractDialogFragment fragment = new AlertDialogFragment.Builder().build(99);
                fragment.getArguments().putString("title", "レンズを削除できません");
                fragment.getArguments().putString("message",
                        lens.modelName + "は既存の写真記録から参照されているため、削除することができません。");
                fragment.showOn(this, "dialog");
            }else {
                AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_LENS);
                fragment.getArguments().putString("title", "レンズの削除");
                fragment.getArguments().putString("message", lens.modelName + "を削除しますか？この操作は元に戻せません！");
                fragment.getArguments().putInt("id", lens.id);
                fragment.showOn(this, "dialog");
            }
        }else {
            Intent intent = new Intent(getApplication(), EditLensActivity.class);
            intent.putExtra("id", lens.id);
            startActivityForResult(intent, REQCODE_EDIT_LENS);
        }
    }
    public void onListFragmentInteraction(FilmRoll filmRoll, boolean isLong){
        if(isLong){
            AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                                                  .build(RETCODE_DELETE_FILMROLL);
            fragment.getArguments().putString("title", "フィルムの削除");
            fragment.getArguments().putString("message", filmRoll.name + "を削除しますか？この操作は元に戻せません！");
            fragment.getArguments().putInt("id", filmRoll.id);
            fragment.showOn(this, "dialog");
        }else{
            Intent intent = new Intent(getApplication(), EditPhotoListActivity.class);
            intent.putExtra("id", filmRoll.id);
            startActivityForResult(intent, REQCODE_EDIT_PHOTO_LIST);
        }
    }
    public void onFragmentInteraction(Uri uri){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RETCODE_SDCARD_PERM) {
            onRequestSDCardAccessPermissionsResult(permissions, grantResults);
        }
    }

    void onRequestSDCardAccessPermissionsResult(String[] permissions, int[] grantResults) {
        int[] granted = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted)) {
            exportDBDialog();
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show();
        }
    }

    public void checkPermAndExportDB(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM);
            return;
        }
        exportDBDialog();
    }

    public void exportDBDialog(){
        final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);
        Log.d("path", Environment.getExternalStorageDirectory().getAbsolutePath());
        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("Trisquel")
                .allowReadOnlyDirectory(true)
                .allowNewDirectoryNameModification(true)
                .initialDirectory(Environment.getExternalStorageDirectory().getAbsolutePath())
                .build();
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
        startActivityForResult(chooserIntent, REQCODE_BACKUP_DIR_CHOSEN);
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case RETCODE_OPEN_RELEASE_NOTES:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    Uri uri = Uri.parse(RELEASE_NOTES_URL);
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(i);
                }
                break;
            case RETCODE_BACKUP_DB:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    checkPermAndExportDB();
                }
                break;
            case RETCODE_DELETE_FILMROLL:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        int id;
                        id = data.getIntExtra("id",-1);
                        if(id != -1) filmroll_fragment.deleteFilmRoll(id);
                    }
                }
                break;
            case RETCODE_DELETE_CAMERA:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        int id;
                        id = data.getIntExtra("id",-1);
                        if(id != -1) cam_fragment.deleteCamera(id);
                    }
                }
                break;
            case RETCODE_DELETE_LENS:
                if(resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        int id;
                        id = data.getIntExtra("id",-1);
                        if(id != -1) lens_fragment.deleteLens(id);
                    }
                }
                break;
            case RETCODE_CAMERA_TYPE:
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        int which;
                        which = data.getIntExtra("which", 0);
                        Intent intent = new Intent(getApplication(), EditCameraActivity.class);
                        intent.putExtra("type", which);
                        startActivityForResult(intent, REQCODE_ADD_CAMERA);
                    }
                }
            default:
        }
    }
    @Override
    public void onDialogCancelled(int requestCode) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
