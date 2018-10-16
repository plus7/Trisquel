package net.tnose.app.trisquel

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by user on 2018/02/07.
 */

open class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
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
        )

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
        )

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
        )

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
                        + "memo text not null,"
                        + "accessories text not null"
                        + ");"
        )

        db.execSQL(
                "create table accessory ("
                        + "_id  integer primary key autoincrement not null,"
                        + "created text not null,"
                        + "last_modified text not null,"
                        + "type integer,"
                        + "name text not null,"
                        + "mount text not null,"
                        + "focal_length_factor real"
                        + ");"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        /*
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
        }*/
        /*if(oldVersion <= 9){
            db.execSQL(
                    "create table accessory ("
                            + "_id  integer primary key autoincrement not null,"
                            + "created text not null,"
                            + "last_modified text not null,"
                            + "type integer,"
                            + "name text not null,"
                            + "lens_mount text not null,"
                            + "camera_mount text not null,"
                            + "mount text not null,"
                            + "focal_length_factor real"
                            + ");"
            );
        }*/
        /*if(oldVersion <= 10){
            db.execSQL("alter table photo add column accessories text not null default '' ;");
        }*/
        /*
        if(oldVersion <= 11){
            db.execSQL("drop table accessory;");
            db.execSQL(
                    "create table accessory ("
                            + "_id  integer primary key autoincrement not null,"
                            + "created text not null,"
                            + "last_modified text not null,"
                            + "type integer,"
                            + "name text not null,"
                            + "mount text not null,"
                            + "focal_length_factor real"
                            + ");"
            );
        }
        */
        if (oldVersion <= 11) {
            db.execSQL("alter table photo add column accessories text not null default '' ;")
            db.execSQL(
                    "create table accessory ("
                            + "_id  integer primary key autoincrement not null,"
                            + "created text not null,"
                            + "last_modified text not null,"
                            + "type integer,"
                            + "name text not null,"
                            + "mount text not null,"
                            + "focal_length_factor real"
                            + ");"
            )
        }
    }

    fun open(): SQLiteDatabase {
        return super.getWritableDatabase()
    }

    companion object {
        internal val DATABASE_NAME = "trisquel.db"

        internal val DATABASE_VERSION = 12
    }
}

