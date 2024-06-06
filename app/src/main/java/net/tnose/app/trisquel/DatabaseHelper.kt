package net.tnose.app.trisquel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


/**
 * Created by user on 2018/02/07.
 */
@Entity(tableName = "camera")
data class CameraEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")            val id: Int,
    @ColumnInfo(name = "created")        val created: String,
    @ColumnInfo(name = "type")           val type: Int?,
    @ColumnInfo(name = "last_modified")  val lastModified: String,
    @ColumnInfo(name = "mount")          val mount: String,
    @ColumnInfo(name = "manufacturer")   val manufacturer: String,
    @ColumnInfo(name = "model_name")     val modelName: String,
    @ColumnInfo(name = "format")         val format: Int?,
    @ColumnInfo(name = "ss_grain_size")  val ssGrainSize: Int?,
    @ColumnInfo(name = "fastest_ss")     val fastestSs: Double?,
    @ColumnInfo(name = "slowest_ss")     val slowestSs: Double?,
    @ColumnInfo(name = "bulb_available") val bulbAvailable: Int?,
    @ColumnInfo(name = "shutter_speeds") val shutterSpeeds: String,
    @ColumnInfo(name = "ev_grain_size")  val evGrainSize: Int?,
    @ColumnInfo(name = "ev_width")       val evWidth: Int?
)

@Entity(tableName = "lens")
data class LensEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")           val id: Int,
    @ColumnInfo(name = "created")       val created: String,
    @ColumnInfo(name = "last_modified") val lastModified: String,
    @ColumnInfo(name = "mount")         val mount: String,
    @ColumnInfo(name = "body")          val body: Int?,
    @ColumnInfo(name = "manufacturer")  val manufacturer: String,
    @ColumnInfo(name = "model_name")    val modelName: String,
    @ColumnInfo(name = "focal_length")  val focalLength: String,
    @ColumnInfo(name = "f_steps")       val fSteps: String,
)

@Entity(tableName = "filmroll")
data class FilmRollEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")           val id: Int,
    @ColumnInfo(name = "created")       val created: String,
    @ColumnInfo(name = "name")          val name: String,
    @ColumnInfo(name = "last_modified") val lastModified: String,
    @ColumnInfo(name = "camera")        val camera: Int?,
    @ColumnInfo(name = "format")        val format: String,
    @ColumnInfo(name = "manufacturer")  val manufacturer: String,
    @ColumnInfo(name = "brand")         val brand: String,
    @ColumnInfo(name = "iso")           val iso: String
)

@Entity(tableName = "photo")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")              val id: Int,
    @ColumnInfo(name = "filmroll")         val filmroll: Int?,
    @ColumnInfo(name = "_index")           val _index: Int?,
    @ColumnInfo(name = "date")             val date: String,
    @ColumnInfo(name = "camera")           val camera: Int?,
    @ColumnInfo(name = "lens")             val lens: Int?,
    @ColumnInfo(name = "focal_length")     val focalLength: Double?,
    @ColumnInfo(name = "aperture")         val aperture: Double?,
    @ColumnInfo(name = "shutter_speed")    val shutterSpeed: Double?,
    @ColumnInfo(name = "exp_compensation") val expCompensation: Double?,
    @ColumnInfo(name = "ttl_light_meter")  val ttlLightMeter: Double?,
    @ColumnInfo(name = "location")         val location: String,
    @ColumnInfo(name = "latitude")         val latitude: Double?,
    @ColumnInfo(name = "longitude")        val longitude: Double?,
    @ColumnInfo(name = "memo")             val memo: String,
    @ColumnInfo(name = "accessories")      val accessories: String,
    @ColumnInfo(name = "suppimgs")         val suppimgs: String,
    @ColumnInfo(name = "favorite")         val favorite: Int?,
)

@Entity(tableName = "accessory")
data class AccessoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")                 val id: Int,
    @ColumnInfo(name = "created")             val created: String,
    @ColumnInfo(name = "last_modified")       val lastModified: String,
    @ColumnInfo(name = "type")                val type: Int?,
    @ColumnInfo(name = "name")                val name: String,
    @ColumnInfo(name = "mount")               val mount: String,
    @ColumnInfo(name = "focal_length_factor") val focalLengthFactor: Double?,
)

@Entity(tableName = "suppimg")
data class SuppimgEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")      val id: Int,
    @ColumnInfo(name = "photo_id") val photoId: Int?,
    @ColumnInfo(name = "path")     val path: String,
    @ColumnInfo(name = "_index")   val _index: Int?,
)

@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")    val id: Int,
    @ColumnInfo(name = "label")  val label: String,
    @ColumnInfo(name = "refcnt") val refcnt: Int?,
)

@Entity(tableName = "tagmap")
data class TagMapEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")         val id: Int,
    @ColumnInfo(name = "photo_id")    val photoId: Int?,
    @ColumnInfo(name = "tag_id")      val tagId: Int?,
    @ColumnInfo(name = "filmroll_id") val filmrollId: Int?,
)

@Entity(tableName = "trisquel_metadata")
data class TrisquelMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")            val id: Int,
    @ColumnInfo(name = "path_conv_done") val pathConvDone: Int?,
)

data class ExpDate(
    val date: String
)
data class FilmRollAndRels(
    @Embedded
    val filmRoll: FilmRollEntity,

    @Relation(parentColumn = "camera", entityColumn = "_id")
    val camera: CameraEntity,

    @Relation(parentColumn = "_id", entityColumn = "filmroll") //, projection = arrayOf("date"), entity = PhotoEntity.class)
    val photos: List<PhotoEntity>
)
@Dao
interface TrisquelDao2 {

    @Query("select * from camera order by created desc;")
    fun allCameras () : LiveData<List<CameraEntity>>

    @Query("SELECT * from filmroll where _id = :id")
    fun getFilmRoll(id : Int): LiveData<FilmRollEntity>

    @Query("SELECT * from filmroll order by created desc")
    fun allFilmRollAndRels(): LiveData<List<FilmRollAndRels>>
    @Query("SELECT * from filmroll WHERE cast(camera as text) LIKE :camera AND brand LIKE :filmbrand;")
    fun allFilmRollAndRelsWithFilter(camera : String, filmbrand : String): LiveData<List<FilmRollAndRels>>
    @Query("SELECT * from filmroll WHERE cast(camera as text) LIKE :camera AND brand LIKE :filmbrand order by name asc; ")
    fun allFilmRollAndRelsSortByName(camera : String, filmbrand : String): LiveData<List<FilmRollAndRels>>
    // フィルムブランドやカメラでのソートの実装が難しい

    @Upsert
    suspend fun upsertFilmRoll(entity: FilmRollEntity)
    @Delete
    suspend fun deleteFilmRoll(vararg entity: FilmRollEntity)

    @Query("select * from accessory order by created desc;")
    fun allAccessories() : LiveData<List<AccessoryEntity>>
    @Query("select * from accessory order by name asc;") //ダサいがこうするしかない？
    fun allAccessoriesSortByName() : LiveData<List<AccessoryEntity>>
    @Query("select * from accessory order by type asc;")
    fun allAccessoriesSortByType() : LiveData<List<AccessoryEntity>>

    @Upsert
    suspend fun upsertAccessory(entity: AccessoryEntity)

    @Delete
    suspend fun deleteAccessory(vararg entity: AccessoryEntity)

    @Query("select * from tag;")
    fun allTags() : LiveData<List<TagEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMetadata(metadata: TrisquelMetadataEntity)
}

// 元々のバージョンが17だったので、一つ上の18に設定する
@Database(entities = [CameraEntity::class,
    LensEntity::class,
    FilmRollEntity::class,
    PhotoEntity::class,
    AccessoryEntity::class,
    SuppimgEntity::class,
    TagEntity::class,
    TagMapEntity::class,
    TrisquelMetadataEntity::class ], version = 18)
abstract class TrisquelRoomDatabase : RoomDatabase() {
    abstract fun trisquelDao(): TrisquelDao2
    companion object {

        const val USER_DB_NAME = "trisquel.db"

        @Volatile
        private var INSTANCE: TrisquelRoomDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): TrisquelRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    TrisquelRoomDatabase::class.java,
                    USER_DB_NAME
                )
                    .addMigrations(MIGRATION_17_18)
                    .addCallback(object : Callback() {
                        private val applicationScope = CoroutineScope(SupervisorJob())
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            applicationScope.launch(Dispatchers.IO) {
                                populateDatabase()
                            }
                            // XXX: ここjoinでブロックしないといけなかったりしないか？
                        }
                        private suspend fun populateDatabase() {
                            getInstance(context).trisquelDao().insertMetadata(TrisquelMetadataEntity(0,1))
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
        // Migration継承objectを定義
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    // 新しいテーブルを一時テーブルとして構築
                    database.execSQL("create table tmp_trisquel_metadata( _id integer primary key autoincrement not null, path_conv_done integer );" )

                    // 旧テーブルのデータを全て一時テーブルに追加
                    database.execSQL("""
                insert into tmp_trisquel_metadata (path_conv_done)
                select path_conv_done from trisquel_metadata
                """.trimIndent()
                    )
                    // 旧テーブルを削除
                    database.execSQL("drop table trisquel_metadata")
                    // 新テーブルをリネーム
                    database.execSQL("alter table tmp_trisquel_metadata rename to trisquel_metadata")

                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
    }
}
open class DatabaseHelper(context: Context?){ //} : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    /*
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
                        + "accessories text not null,"
                        + "suppimgs text not null,"
                        + "favorite integer"
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

        db.execSQL(
                "create table suppimg ("
                        + "_id  integer primary key autoincrement not null,"
                        + "photo_id integer,"
                        + "path text not null,"
                        + "_index integer"
                        + ");"
        )

        db.execSQL(
                "create table tag ("
                        + "_id  integer primary key autoincrement not null,"
                        + "label text not null,"
                        + "refcnt integer"
                        + ");"
        )

        db.execSQL(
                "create table tagmap ("
                        + "_id  integer primary key autoincrement not null,"
                        + "photo_id integer,"
                        + "tag_id integer,"
                        + "filmroll_id integer"
                        + ");"
        )

        //db.execSQL( "create table trisquel_metadata( path_conv_done integer );" )
        db.execSQL( "create table trisquel_metadata( _id integer primary key autoincrement not null, path_conv_done integer );" )
        db.execSQL( "insert into trisquel_metadata(path_conv_done) values(1);" )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        /*
        もっと昔のバージョンを使っている人が統計上ほぼいなくなったので、oldVersionが16以前の処理は削除
        */
    }

    fun open(): SQLiteDatabase {
        return super.getWritableDatabase()
    }*/

    companion object {
        internal val DATABASE_NAME = "trisquel.db"

        internal val DATABASE_VERSION = 18
    }
}

