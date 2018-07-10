package net.tnose.app.trisquel;

/**
 * Created by user on 2018/02/07.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import static android.database.Cursor.FIELD_TYPE_NULL;

public class TrisquelDao extends DatabaseHelper {
    protected Context mContext = null;
    protected SQLiteDatabase mDb = null;

    public TrisquelDao(Context context) {
        super(context);
        mContext = context;
    }

    public void connection() {
        DatabaseHelper helper = new DatabaseHelper(mContext);
        mDb = helper.open();
    }

    public void close() {
        mDb.close();
        mDb = null;
    }

    /* Camera */
    public long addCamera(CameraSpec camera){
        ContentValues val = new ContentValues();
        val.put( "type", camera.type);
        val.put( "created", Util.dateToStringUTC(camera.created));
        val.put( "last_modified", Util.dateToStringUTC(camera.lastModified));
        val.put( "mount", camera.mount );
        val.put( "manufacturer", camera.manufacturer );
        val.put( "model_name", camera.modelName );
        val.put( "format", camera.format );
        val.put( "ss_grain_size", camera.shutterSpeedGrainSize );
        val.put( "fastest_ss", camera.fastestShutterSpeed );
        val.put( "slowest_ss", camera.slowestShutterSpeed );
        val.put( "bulb_available", camera.bulbAvailable ? 1 : 0);
        if(camera.shutterSpeedSteps != null) {
            val.put("shutter_speeds",
                    Arrays.toString(camera.shutterSpeedSteps)
                            .replace("[", "")
                            .replace("]", "") // Dirty code...
            );
        }else{
            val.put("shutter_speeds", "");
        }
        val.put( "ev_grain_size", camera.evGrainSize);
        val.put( "ev_width", camera.evWidth);
        return mDb.insert( "camera", null, val);
    }

    public void deleteCamera(int id){
        String[] selectArgs = new String[]{Integer.toString(id)};
        mDb.delete("camera", "_id = ?", selectArgs);
    }

    public int updateCamera(CameraSpec camera){
        String[] selectArgs = new String[]{ Integer.toString(camera.id) };
        ContentValues val = new ContentValues();
        val.put( "type", camera.type);
        val.put( "created", Util.dateToStringUTC(camera.created));
        val.put( "last_modified", Util.dateToStringUTC(camera.lastModified));
        val.put( "mount", camera.mount );
        val.put( "manufacturer", camera.manufacturer );
        val.put( "model_name", camera.modelName );
        val.put( "format", camera.format );
        val.put( "ss_grain_size", camera.shutterSpeedGrainSize );
        val.put( "fastest_ss", camera.fastestShutterSpeed);
        val.put( "slowest_ss", camera.slowestShutterSpeed);
        val.put( "bulb_available", camera.bulbAvailable);
        val.put( "shutter_speeds",
                Arrays.toString(camera.shutterSpeedSteps)
                        .replace("\\[","")
                        .replace("\\]",""));
        val.put( "ev_grain_size", camera.evGrainSize);
        val.put( "ev_width", camera.evWidth);

        return mDb.update( "camera",
                val,
                "_id = ?",
                selectArgs);
    }

    public ArrayList<CameraSpec> getAllCameras(){
        ArrayList<CameraSpec> cameraList = new ArrayList<CameraSpec>();

        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from camera order by created desc;", null);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                String mount = cursor.getString(cursor.getColumnIndex("mount"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String modelName = cursor.getString(cursor.getColumnIndex("model_name"));
                int format = cursor.getInt(cursor.getColumnIndex("format"));
                int shutterSpeedGrainSize = cursor.getInt(cursor.getColumnIndex("ss_grain_size"));
                double fastestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("fastest_ss"));
                double slowestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("slowest_ss"));
                boolean bulbAvailable = cursor.getInt(cursor.getColumnIndex("bulb_available")) != 0;
                String shutterSpeedSteps = cursor.getString(cursor.getColumnIndex("shutter_speeds"));
                int evBiasStep = cursor.getInt(cursor.getColumnIndex("ev_grain_size"));
                int evBiasWidth = cursor.getInt(cursor.getColumnIndex("ev_width"));
                cameraList.add(
                        new CameraSpec(id, type, created, lastModified, mount, manufacturer, modelName, format,
                                       shutterSpeedGrainSize, fastestShutterSpeed, slowestShutterSpeed,
                                       bulbAvailable, shutterSpeedSteps, evBiasStep, evBiasWidth));
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }

        return cameraList;
    }

    public CameraSpec getCamera(int id){
        CameraSpec c = null;
        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from camera where _id = ?;", new String[]{Integer.toString(id)});

            if(cursor.moveToFirst()){
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                String mount = cursor.getString(cursor.getColumnIndex("mount"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String modelName = cursor.getString(cursor.getColumnIndex("model_name"));
                int format = cursor.getInt(cursor.getColumnIndex("format"));
                int shutterSpeedGrainSize = cursor.getInt(cursor.getColumnIndex("ss_grain_size"));
                double fastestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("fastest_ss"));
                double slowestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("slowest_ss"));
                boolean bulbAvailable = cursor.getInt(cursor.getColumnIndex("bulb_available")) != 0;
                String shutterSpeedSteps = cursor.getString(cursor.getColumnIndex("shutter_speeds"));
                int evBiasStep = cursor.getInt(cursor.getColumnIndex("ev_grain_size"));
                int evBiasWidth = cursor.getInt(cursor.getColumnIndex("ev_width"));

                c = new CameraSpec(id, type, created, lastModified, mount, manufacturer, modelName, format,
                        shutterSpeedGrainSize, fastestShutterSpeed, slowestShutterSpeed,
                        bulbAvailable, shutterSpeedSteps, evBiasStep, evBiasWidth);

            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }
        //nullの場合どうする？
        return c;
    }

    public long getCameraUsageCount(int id){
        long filmrolls = DatabaseUtils.queryNumEntries(mDb, "filmroll","camera = ?",new String[]{ Integer.toString(id) });
        long photos = DatabaseUtils.queryNumEntries(mDb, "photo","camera = ?",new String[]{ Integer.toString(id) });
        return filmrolls + photos;
    }

    /* Lens */
    public long addLens(LensSpec lens){
        ContentValues val = new ContentValues();
        val.put( "created", Util.dateToStringUTC(lens.created));
        val.put( "last_modified", Util.dateToStringUTC(lens.lastModified));
        val.put( "mount", lens.mount );
        val.put( "body", lens.body );
        val.put( "manufacturer", lens.manufacturer );
        val.put( "model_name", lens.modelName );
        val.put( "focal_length", lens.focalLength );
        val.put( "f_steps",
                Arrays.toString(lens.fSteps)
                        .replace("[","")
                        .replace("]","") // Dirty code...
        );
        return mDb.insert( "lens", null, val);
    }

    public void deleteLens(int id){
        String[] selectArgs = new String[]{Integer.toString(id)};
        mDb.delete("lens", "_id = ?", selectArgs);
    }

    public int updateLens(LensSpec lens){
        String[] selectArgs = new String[]{ Integer.toString(lens.id) };
        ContentValues val = new ContentValues();
        val.put( "created", Util.dateToStringUTC(lens.created));
        val.put( "last_modified", Util.dateToStringUTC(lens.lastModified));
        val.put( "mount", lens.mount );
        val.put( "body", lens.body );
        val.put( "manufacturer", lens.manufacturer );
        val.put( "model_name", lens.modelName );
        val.put( "focal_length", lens.focalLength );
        val.put( "f_steps",
                Arrays.toString(lens.fSteps)
                        .replace("[","")
                        .replace("]","") // Dirty code...
        );
        return mDb.update( "lens",
                val,
                "_id = ?",
                selectArgs);
    }

    public ArrayList<LensSpec> getAllVisibleLenses(){
        ArrayList<LensSpec> lensList = new ArrayList<LensSpec>();

        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from lens order by created desc;", null);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                String mount = cursor.getString(cursor.getColumnIndex("mount"));
                int body = cursor.getInt(cursor.getColumnIndex("body"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String modelName = cursor.getString(cursor.getColumnIndex("model_name"));
                String focalLength = cursor.getString(cursor.getColumnIndex("focal_length"));
                String fSteps = cursor.getString(cursor.getColumnIndex("f_steps"));
                if(body == 0) lensList.add(new LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps));
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }

        return lensList;
    }

    public ArrayList<LensSpec> getAllLenses(){
        ArrayList<LensSpec> lensList = new ArrayList<LensSpec>();

        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from lens order by created desc;", null);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                String mount = cursor.getString(cursor.getColumnIndex("mount"));
                int body = cursor.getInt(cursor.getColumnIndex("body"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String modelName = cursor.getString(cursor.getColumnIndex("model_name"));
                String focalLength = cursor.getString(cursor.getColumnIndex("focal_length"));
                String fSteps = cursor.getString(cursor.getColumnIndex("f_steps"));
                lensList.add(new LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps));
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }

        return lensList;
    }

    public ArrayList<LensSpec> getLensesByMount(String mount){

        ArrayList<LensSpec> lensList = new ArrayList<LensSpec>();

        Cursor cursor = null;
        try{
            String[] selectArgs = new String[]{ mount };
            cursor = mDb.rawQuery("select * from lens where mount = ? order by created desc;", selectArgs);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                //String mount = cursor.getString(cursor.getColumnIndex("mount"));
                int body = cursor.getInt(cursor.getColumnIndex("body"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String modelName = cursor.getString(cursor.getColumnIndex("model_name"));
                String focalLength = cursor.getString(cursor.getColumnIndex("focal_length"));
                String fSteps = cursor.getString(cursor.getColumnIndex("f_steps"));
                lensList.add(new LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps));
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }

        return lensList;
    }

    public int getFixedLensIdByBody(int body){
        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select _id from lens where body = ?;", new String[]{Integer.toString(body)});

            if(cursor.moveToFirst()){
                return cursor.getInt(cursor.getColumnIndex("_id"));
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }
        //nullの場合どうする？
        return -1;
    }

    public LensSpec getLens(int id){
        LensSpec l = null;
        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from lens where _id = ?;", new String[]{Integer.toString(id)});

            if(cursor.moveToFirst()){
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                String mount = cursor.getString(cursor.getColumnIndex("mount"));
                int body = cursor.getInt(cursor.getColumnIndex("body"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String modelName = cursor.getString(cursor.getColumnIndex("model_name"));
                String focalLength = cursor.getString(cursor.getColumnIndex("focal_length"));
                String fSteps = cursor.getString(cursor.getColumnIndex("f_steps"));
                l = new LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps);
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }
        //nullの場合どうする？
        return l;
    }

    public long getLensUsageCount(int id){
        return DatabaseUtils.queryNumEntries(mDb, "photo","lens = ?",new String[]{ Integer.toString(id) });
    }

    /* Film roll */
    public long addFilmRoll(FilmRoll f){
        ContentValues val = new ContentValues();
        val.put( "name", f.name );
        val.put( "created", Util.dateToStringUTC(f.created));
        val.put( "last_modified", Util.dateToStringUTC(f.lastModified));
        val.put( "camera", f.camera.id );
        val.put( "format", f.camera.format);
        val.put( "manufacturer", f.manufacturer );
        val.put( "brand", f.brand );
        val.put( "iso", f.iso );
        return mDb.insert( "filmroll", null, val);
    }

    public int updateFilmRoll(FilmRoll f){
        String[] selectArgs = new String[]{ Integer.toString(f.id) };
        ContentValues val = new ContentValues();
        val.put( "name", f.name);
        val.put( "created", Util.dateToStringUTC(f.created));
        val.put( "last_modified", Util.dateToStringUTC(f.lastModified));
        val.put( "camera", f.camera.id );
        val.put( "format", f.camera.format);
        val.put( "manufacturer", f.manufacturer );
        val.put( "brand", f.brand );
        val.put( "iso", f.iso );
        return mDb.update( "filmroll",
                val,
                "_id = ?",
                selectArgs);
    }

    public ArrayList<FilmRoll> getAllFilmRolls(){
        ArrayList<FilmRoll> filmList = new ArrayList<FilmRoll>();

        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from filmroll order by created desc;", null);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                int camera = cursor.getInt(cursor.getColumnIndex("camera"));
                Log.d("unchi", Integer.toString(camera));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String brand = cursor.getString(cursor.getColumnIndex("brand"));
                int iso = cursor.getInt(cursor.getColumnIndex("iso"));

                FilmRoll f = new FilmRoll(id, name, created, lastModified, getCamera(camera), manufacturer, brand, iso, 36);
                filmList.add(f);
                f.photos = getPhotosByFilmRollId(f.id);
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }

        return filmList;
    }

    public void deleteFilmRoll(int id){
         String[] selectArgs = new String[]{Integer.toString(id)};
         mDb.delete("photo", "filmroll = ?", selectArgs);
         mDb.delete("filmroll", "_id = ?", selectArgs);
    }

    public FilmRoll getFilmRoll(int id){
        FilmRoll f = null;
        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from filmroll where _id = ?;", new String[]{Integer.toString(id)});

            if(cursor.moveToFirst()){
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String created = cursor.getString(cursor.getColumnIndex("created"));
                String lastModified = cursor.getString(cursor.getColumnIndex("last_modified"));
                int camera = cursor.getInt(cursor.getColumnIndex("camera"));
                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                String brand = cursor.getString(cursor.getColumnIndex("brand"));
                int iso = cursor.getInt(cursor.getColumnIndex("iso"));
                f = new FilmRoll(id, name, created, lastModified, getCamera(camera), manufacturer, brand, iso, 36/* temp value */);
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }
        //nullの場合どうする？
        return f;
    }

    public ArrayList<Photo> getPhotosByFilmRollId(int filmRollId){

        ArrayList<Photo> photos = new ArrayList<Photo>();

        Cursor cursor = null;
        try{
            String[] selectArgs = new String[]{ Integer.toString(filmRollId) };
            cursor = mDb.rawQuery("select * from photo where filmroll = ? order by _index asc;", selectArgs);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                int index = cursor.getInt(cursor.getColumnIndex("_index"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                int camera = cursor.getInt(cursor.getColumnIndex("camera"));
                int lens = cursor.getInt(cursor.getColumnIndex("lens"));
                double focalLength = cursor.getDouble(cursor.getColumnIndex("focal_length"));
                double aperture = cursor.getDouble(cursor.getColumnIndex("aperture"));
                double shutterSpeed = cursor.getDouble(cursor.getColumnIndex("shutter_speed"));
                double ev = cursor.getDouble(cursor.getColumnIndex("exp_compensation"));
                double ttl = cursor.getDouble(cursor.getColumnIndex("ttl_light_meter"));
                String location = cursor.getString(cursor.getColumnIndex("location"));
                String memo = cursor.getString(cursor.getColumnIndex("memo"));
                double latitude, longitude;
                if(cursor.getType(cursor.getColumnIndex("latitude")) == FIELD_TYPE_NULL){
                    latitude = 999;
                }else{
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                }
                if(cursor.getType(cursor.getColumnIndex("longitude")) == FIELD_TYPE_NULL){
                    longitude = 999;
                }else{
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                }
                photos.add(new Photo(id, filmRollId, index, date, camera, lens, focalLength,
                        aperture, shutterSpeed, ev, ttl, location, latitude, longitude, memo));
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }

        return photos;
    }

    /* Photo */
    public long addPhoto(Photo p){
        ContentValues val = new ContentValues();
        val.put( "filmroll", p.filmrollid );
        val.put( "_index", p.index);
        val.put( "date", p.date);
        val.put( "camera", p.cameraid );
        val.put( "lens", p.lensid );
        Log.d("addPhoto", "lens="+Integer.toString(p.lensid));
        val.put( "focal_length", p.focalLength );
        val.put( "aperture", p.aperture );
        val.put( "shutter_speed", p.shutterSpeed );
        val.put( "exp_compensation", p.expCompensation );
        val.put( "ttl_light_meter", p.ttlLightMeter );
        val.put( "location", p.location );
        val.put( "latitude", p.latitude );
        val.put( "longitude", p.longitude );
        val.put( "memo", p.memo );
        return mDb.insert( "photo", null, val);
    }

    public int updatePhoto(Photo p){
        String[] selectArgs = new String[]{ Integer.toString(p.id) };
        ContentValues val = new ContentValues();
        val.put( "filmroll", p.filmrollid );
        val.put( "_index", p.index);
        val.put( "date", p.date);
        val.put( "camera", p.cameraid );
        val.put( "lens", p.lensid );
        Log.d("updatePhoto", "lens="+Integer.toString(p.lensid));
        val.put( "focal_length", p.focalLength );
        val.put( "aperture", p.aperture );
        val.put( "shutter_speed", p.shutterSpeed );
        val.put( "exp_compensation", p.expCompensation );
        val.put( "ttl_light_meter", p.ttlLightMeter );
        val.put( "location", p.location );
        val.put( "latitude", p.latitude );
        val.put( "longitude", p.longitude );
        val.put( "memo", p.memo );
        return mDb.update( "photo",
                val,
                "_id = ?",
                selectArgs);
    }

    public void deletePhoto(int id){
        String[] selectArgs = new String[]{Integer.toString(id)};
        mDb.delete("photo", "_id = ?", selectArgs);
    }

    public Photo getPhoto(int id){
        Photo p = null;
        Cursor cursor = null;
        try{
            cursor = mDb.rawQuery("select * from photo where _id = ?;", new String[]{Integer.toString(id)});
            if(cursor.moveToFirst()){
                //int id = cursor.getInt(cursor.getColumnIndex("_id"));
                int index = cursor.getInt(cursor.getColumnIndex("_index"));
                int filmroll = cursor.getInt(cursor.getColumnIndex("filmroll"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                int camera = cursor.getInt(cursor.getColumnIndex("camera"));
                int lens = cursor.getInt(cursor.getColumnIndex("lens"));
                Log.d("getPhoto", "lens="+Integer.toString(lens));
                double focalLength = cursor.getDouble(cursor.getColumnIndex("focal_length"));
                double aperture = cursor.getDouble(cursor.getColumnIndex("aperture"));
                double shutterSpeed = cursor.getDouble(cursor.getColumnIndex("shutter_speed"));
                double ev = cursor.getDouble(cursor.getColumnIndex("exp_compensation"));
                double ttl = cursor.getDouble(cursor.getColumnIndex("ttl_light_meter"));
                String location = cursor.getString(cursor.getColumnIndex("location"));
                String memo = cursor.getString(cursor.getColumnIndex("memo"));
                double latitude, longitude;
                if(cursor.getType(cursor.getColumnIndex("latitude")) == FIELD_TYPE_NULL){
                    latitude = 999;
                }else{
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                }
                if(cursor.getType(cursor.getColumnIndex("longitude")) == FIELD_TYPE_NULL){
                    longitude = 999;
                }else{
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                }
                p = new Photo(id, filmroll, index, date, camera, lens, focalLength,
                        aperture, shutterSpeed, ev, ttl, location, latitude, longitude, memo);
            }
        }
        finally{
            if( cursor != null ){
                cursor.close();
            }
        }
        //nullの場合どうする？
        return p;
    }
}
