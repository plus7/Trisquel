package net.tnose.app.trisquel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by user on 2018/02/07.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "trisquel.db";

    static final int DATABASE_VERSION = 9;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate( SQLiteDatabase db ) {
        db.execSQL(
                "create table camera ("
                        + "_id  integer primary key autoincrement not null,"
                        + "created text not null,"
                        + "type integer,"
                        + "last_modified text not null,"
                        + "mount text not null,"
                        + "manufacturer text not null,"
                        + "model_name text not null,"
                        + "format integer,"
                        + "ss_grain_size integer,"
                        + "fastest_ss real,"
                        + "slowest_ss real,"
                        + "bulb_available integer,"
                        + "shutter_speeds text not null,"
                        + "ev_grain_size integer,"
                        + "ev_width integer"
                        + ");"
        );

        db.execSQL(
                "create table lens ("
                        + "_id  integer primary key autoincrement not null,"
                        + "created text not null,"
                        + "last_modified text not null,"
                        + "mount text not null,"
                        + "body integer,"
                        + "manufacturer text not null,"
                        + "model_name text not null,"
                        + "focal_length text not null,"
                        + "f_steps text not null"
                        + ");"
        );

        db.execSQL(
                "create table filmroll ("
                        + "_id  integer primary key autoincrement not null,"
                        + "created text not null,"
                        + "name text not null,"
                        + "last_modified text not null,"
                        + "camera integer,"
                        + "format text not null,"
                        + "manufacturer text not null,"
                        + "brand text not null,"
                        + "iso text not null"
                        + ");"
        );

        //TODO: indexに対応させる
        db.execSQL(
                "create table photo ("
                        + "_id  integer primary key autoincrement not null,"
                        + "filmroll integer,"
                        + "_index integer,"
                        + "date text not null,"
                        + "camera integer,"
                        + "lens integer,"
                        + "focal_length real,"
                        + "aperture real,"
                        + "shutter_speed real,"
                        + "exp_compensation real,"
                        + "ttl_light_meter real,"
                        + "location text not null,"
                        + "latitude real,"
                        + "longitude real,"
                        + "memo text not null"
                        + ");"
        );
        // expMeter
        // expCompensation
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        /*
        if( oldVersion <= 4 ){
            db.execSQL(
                    "create table tmp_photo ("
                            + "_id  integer primary key autoincrement not null,"
                            + "filmroll integer,"
                            + "_index integer,"
                            + "date text not null,"
                            + "camera integer,"
                            + "lens integer,"
                            + "focal_length real,"
                            + "aperture real,"
                            + "shutter_speed real,"
                            + "exp_compensation real,"
                            + "location text not null,"
                            + "memo text not null"
                            + ");"
            );
            db.execSQL("insert into tmp_photo select * from photo;");
            db.execSQL("drop table photo;");
            db.execSQL("alter table tmp_photo rename to photo;");
        }

        if(oldVersion <= 5) {
            db.execSQL("alter table camera rename to tmp_camera;");
            db.execSQL(
                    "create table camera ("
                            + "_id  integer primary key autoincrement not null,"
                            + "created text not null,"
                            + "type integer,"
                            + "last_modified text not null,"
                            + "mount text not null,"
                            + "manufacturer text not null,"
                            + "model_name text not null,"
                            + "format integer,"
                            + "ss_grain_size integer,"
                            + "fastest_ss real,"
                            + "slowest_ss real,"
                            + "bulb_available integer,"
                            + "shutter_speeds text not null,"
                            + "ev_grain_size integer,"
                            + "ev_width integer"
                            + ");"
            );

            db.execSQL("insert into camera(_id, created, last_modified, mount, manufacturer, model_name, format, ss_grain_size, fastest_ss, slowest_ss, bulb_available, shutter_speeds, ev_grain_size, ev_width) select _id, created, last_modified, mount, manufacturer, model_name, format, ss_grain_size, fastest_ss, slowest_ss, bulb_available, shutter_speeds, ev_grain_size, ev_width from tmp_camera;");
            db.execSQL("drop table tmp_camera;");
        }

        if(oldVersion <= 6){
            db.execSQL("alter table lens rename to tmp_lens;");
            db.execSQL(
                    "create table lens ("
                            + "_id  integer primary key autoincrement not null,"
                            + "created text not null,"
                            + "last_modified text not null,"
                            + "mount text not null,"
                            + "body integer,"
                            + "manufacturer text not null,"
                            + "model_name text not null,"
                            + "focal_length text not null,"
                            + "f_steps text not null"
                            + ");"
            );
            db.execSQL("insert into lens(_id, created, last_modified, mount, manufacturer, model_name, focal_length, f_steps) select _id, created, last_modified, mount, manufacturer, model_name, focal_length, f_steps from tmp_lens;");
            db.execSQL("drop table tmp_lens;");
        }
        */

        if(oldVersion <= 7){
            db.execSQL("alter table photo rename to tmp_photo;");
            db.execSQL(
                    "create table photo ("
                            + "_id  integer primary key autoincrement not null,"
                            + "filmroll integer,"
                            + "_index integer,"
                            + "date text not null,"
                            + "camera integer,"
                            + "lens integer,"
                            + "focal_length real,"
                            + "aperture real,"
                            + "shutter_speed real,"
                            + "exp_compensation real,"
                            + "ttl_light_meter real,"
                            + "location text not null,"
                            + "memo text not null"
                            + ");"
            );
            db.execSQL("insert into photo(_id, filmroll, _index, date, camera, lens, focal_length, aperture, shutter_speed, exp_compensation, location, memo) select _id, filmroll, _index, date, camera, lens, focal_length, aperture, shutter_speed, exp_compensation, location, memo from tmp_photo;");
            db.execSQL("drop table tmp_photo;");
        }

        if(oldVersion <= 8) {
            db.execSQL("alter table photo rename to tmp_photo;");
            db.execSQL(
                    "create table photo ("
                            + "_id  integer primary key autoincrement not null,"
                            + "filmroll integer,"
                            + "_index integer,"
                            + "date text not null,"
                            + "camera integer,"
                            + "lens integer,"
                            + "focal_length real,"
                            + "aperture real,"
                            + "shutter_speed real,"
                            + "exp_compensation real,"
                            + "ttl_light_meter real,"
                            + "location text not null,"
                            + "latitude real,"
                            + "longitude real,"
                            + "memo text not null"
                            + ");"
            );
            db.execSQL("insert into photo(_id, filmroll, _index, date, camera, lens, focal_length, aperture, shutter_speed, exp_compensation, ttl_light_meter, location, memo) select _id, filmroll, _index, date, camera, lens, focal_length, aperture, shutter_speed, exp_compensation, ttl_light_meter, location, memo from tmp_photo;");
            db.execSQL("drop table tmp_photo;");
        }
    }

    public SQLiteDatabase open() {
        return super.getWritableDatabase();
    }

    public void close(){
        super.close();
    }
}

