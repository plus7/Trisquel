package net.tnose.app.trisquel

/**
 * Created by user on 2018/02/07.
 */

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_NULL
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class TrisquelDao(context: Context?) : DatabaseHelper(context) {
    protected val mContext = context
    protected var mDb: SQLiteDatabase? = null

    val allCameras: ArrayList<CameraSpec>
        get() {
            val cameraList = ArrayList<CameraSpec>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select * from camera order by created desc;", null)
                while (cursor!!.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndex("_id"))
                    val type = cursor.getInt(cursor.getColumnIndex("type"))
                    val created = cursor.getString(cursor.getColumnIndex("created"))
                    val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                    val mount = cursor.getString(cursor.getColumnIndex("mount"))
                    val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                    val modelName = cursor.getString(cursor.getColumnIndex("model_name"))
                    val format = cursor.getInt(cursor.getColumnIndex("format"))
                    val shutterSpeedGrainSize = cursor.getInt(cursor.getColumnIndex("ss_grain_size"))
                    val fastestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("fastest_ss"))
                    val slowestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("slowest_ss"))
                    val bulbAvailable = cursor.getInt(cursor.getColumnIndex("bulb_available")) != 0
                    val shutterSpeedSteps = cursor.getString(cursor.getColumnIndex("shutter_speeds"))
                    val evBiasStep = cursor.getInt(cursor.getColumnIndex("ev_grain_size"))
                    val evBiasWidth = cursor.getInt(cursor.getColumnIndex("ev_width"))
                    cameraList.add(
                            CameraSpec(id, type, created, lastModified, mount, manufacturer, modelName, format,
                                    shutterSpeedGrainSize, fastestShutterSpeed, slowestShutterSpeed,
                                    bulbAvailable, shutterSpeedSteps, evBiasStep, evBiasWidth))
                }
            } finally {
                cursor?.close()
            }

            return cameraList
        }

    fun getAllEntriesJSON(type: String): JSONArray {
        val ja = JSONArray()

        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from "+type+" order by _id desc;", null)
            while (cursor!!.moveToNext()) {
                val obj = JSONObject()
                for (i in 0..cursor.columnCount - 1) {
                    val cname = cursor.getColumnName(i)
                    val ctype = cursor.getType(i)
                    when(ctype){
                        Cursor.FIELD_TYPE_NULL -> obj.put(cname, null)
                        Cursor.FIELD_TYPE_FLOAT -> obj.put(cname, cursor.getDouble(i))
                        Cursor.FIELD_TYPE_INTEGER -> obj.put(cname, cursor.getInt(i))
                        Cursor.FIELD_TYPE_STRING -> obj.put(cname, cursor.getString(i))
                    }
                }
                ja.put(obj)
            }
        } finally {
            cursor?.close()
        }
        return ja
    }

    fun mergeCameraJSON(obj: JSONObject): Pair<Int, Int>{
        var oldId: Int = -1
        val cval = ContentValues()
        for(key in obj.keys()){
            if(key == "_id") oldId = obj.getInt(key)
            else{
                val value = obj.get(key)
                when(value){
                    is Int -> cval.put(key, obj.getInt(key))
                    is Double -> cval.put(key, obj.getDouble(key))
                    is String -> cval.put(key, obj.getString(key))
                }
            }
        }

        val newId = mDb!!.insert("camera", null, cval)
        return Pair(oldId, newId.toInt())
    }

    fun mergeLensJSON(obj: JSONObject, cameraOld2NewId: Map<Int, Int>): Pair<Int, Int>{
        var oldId: Int = -1
        val cval = ContentValues()
        for(key in obj.keys()){
            if(key == "_id") oldId = obj.getInt(key)
            else if(key == "body") cval.put("body", cameraOld2NewId[obj.getInt(key)])
            else{
                val value = obj.get(key)
                when(value){
                    is Int -> cval.put(key, obj.getInt(key))
                    is Double -> cval.put(key, obj.getDouble(key))
                    is String -> cval.put(key, obj.getString(key))
                }
            }
        }

        val newId = mDb!!.insert("lens", null, cval)
        return Pair(oldId, newId.toInt())
    }

    fun mergeAccessoryJSON(obj: JSONObject): Pair<Int, Int>{
        var oldId: Int = -1
        val cval = ContentValues()
        for(key in obj.keys()){
            if(key == "_id") oldId = obj.getInt(key)
            else{
                val value = obj.get(key)
                when(value){
                    is Int -> cval.put(key, obj.getInt(key))
                    is Double -> cval.put(key, obj.getDouble(key))
                    is String -> cval.put(key, obj.getString(key))
                }
            }
        }

        val newId = mDb!!.insert("accessory", null, cval)
        return Pair(oldId, newId.toInt())
    }

    fun mergeFilmRollJSON(obj: JSONObject, cameraOld2NewId: Map<Int, Int>): Pair<Int, Int>{
        var oldId: Int = -1
        val cval = ContentValues()
        for(key in obj.keys()){
            if(key == "_id") oldId = obj.getInt(key)
            else if(key == "camera") cval.put("camera", cameraOld2NewId[obj.getInt(key)])
            else{
                val value = obj.get(key)
                when(value){
                    is Int -> cval.put(key, obj.getInt(key))
                    is Double -> cval.put(key, obj.getDouble(key))
                    is String -> cval.put(key, obj.getString(key))
                }
            }
        }

        val newId = mDb!!.insert("filmroll", null, cval)
        return Pair(oldId, newId.toInt())
    }

    fun mergePhotoJSON(obj: JSONObject,
                       newpaths: ArrayList<String>,
                       importErrorStr: String,
                       cameraOld2NewId: Map<Int, Int>,
                       lensOld2NewId: Map<Int, Int>,
                       filmrollOld2NewId: Map<Int, Int>,
                       accessoryOld2NewId: Map<Int, Int>): Pair<Int, Int>{
        var oldId: Int = -1
        val cval = ContentValues()
        for(key in obj.keys()){
            if(key == "_id") oldId = obj.getInt(key)
            else if(key == "camera") cval.put(key, cameraOld2NewId[obj.getInt(key)])
            else if(key == "lens") cval.put(key, lensOld2NewId[obj.getInt(key)])
            else if(key == "filmroll") cval.put(key, filmrollOld2NewId[obj.getInt(key)])
            else if(key == "accessories"){ //アクセサリは少し特殊。横着してLIKEで検索できるようにしたかったため。
                val accessories = obj.getString(key)
                        .split(Photo.splitter)
                        .toTypedArray()
                        .filterNot { it.isEmpty() }
                        .map { Integer.parseInt(it) }
                        .map { accessoryOld2NewId[it] }
                        .map { it.toString() }.joinToString("/","/", "/")
                cval.put(key, accessories)
            }
            else if(key == "suppimgs"){
                // newpathsで置換
                cval.put(key, JSONArray(newpaths).toString())
            }
            else if(key.startsWith("suppimgs_")){
                // do nothing
            }
            else if(key == "memo"){
                val value = obj.getString(key)
                val appendStr =
                        if(!importErrorStr.isEmpty()){
                            (if(value.isEmpty()) "" else "\n") + importErrorStr
                        }else{
                            ""
                        }
                cval.put(key, value + appendStr)
            }
            else{
                val value = obj.get(key)
                when(value){
                    is Int -> cval.put(key, obj.getInt(key))
                    is Double -> cval.put(key, obj.getDouble(key))
                    is String -> cval.put(key, obj.getString(key))
                }
            }
        }

        val newId = mDb!!.insert("photo", null, cval)
        return Pair(oldId, newId.toInt())
    }

    //tagはそれなりに特殊
    fun mergeTagMapJSON(tagmaps: JSONArray, tags: JSONArray,
                        filmrollOld2NewId: Map<Int, Int>, photoOld2NewId: Map<Int, Int>){
        val tagOldId2Label: HashMap<Int, String> = HashMap()
        for(i in 0 until tags.length()){
            val tag = tags.getJSONObject(i)
            tagOldId2Label.put(tag.getInt("_id"), tag.getString("label"))
        }

        val tagmapsArray: ArrayList<Triple<Int, Int, String>> = ArrayList()
        for(i in 0 until tagmaps.length()){
            val tagmap = tagmaps.getJSONObject(i)
            tagmapsArray.add(Triple(photoOld2NewId[tagmap.getInt("photo_id")] ?: -1,
                                    filmrollOld2NewId[tagmap.getInt("filmroll_id")] ?: -1,
                                    tagOldId2Label[tagmap.getInt("tag_id")] ?: ""))
        }

        for(group in tagmapsArray.groupBy { it.first }){ // group by photo_id
            if(group.key == -1){
                Log.d("mergeTagMapJSON", "invalid photo_id")
                continue
            }
            val filmrolls = group.value.distinctBy { it.second } // filmroll_id must be same
            if(filmrolls.size > 1 || filmrolls[0].second == -1){
                continue
            }
            tagPhoto(group.key, group.value[0].second, ArrayList(group.value.map { it.third }))
        }
    }

    fun deleteAll(){
        for(table in listOf("camera", "lens", "filmroll", "photo", "accessory", "tag", "tagmap")) {
            mDb!!.delete(table, null, null)

            //reset autoincrement
            val cval = ContentValues()
            cval.put("seq", 0)
            mDb!!.update("sqlite_sequence", cval, "name = ?", arrayOf(table))
        }
    }

    val allVisibleLenses: ArrayList<LensSpec>
        get() {
            val lensList = ArrayList<LensSpec>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select * from lens order by created desc;", null)
                while (cursor!!.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndex("_id"))
                    val created = cursor.getString(cursor.getColumnIndex("created"))
                    val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                    val mount = cursor.getString(cursor.getColumnIndex("mount"))
                    val body = cursor.getInt(cursor.getColumnIndex("body"))
                    val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                    val modelName = cursor.getString(cursor.getColumnIndex("model_name"))
                    val focalLength = cursor.getString(cursor.getColumnIndex("focal_length"))
                    val fSteps = cursor.getString(cursor.getColumnIndex("f_steps"))
                    if (body == 0) lensList.add(LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps))
                }
            } finally {
                cursor?.close()
            }

            return lensList
        }

    val allLenses: ArrayList<LensSpec>
        get() {
            val lensList = ArrayList<LensSpec>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select * from lens order by created desc;", null)
                while (cursor!!.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndex("_id"))
                    val created = cursor.getString(cursor.getColumnIndex("created"))
                    val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                    val mount = cursor.getString(cursor.getColumnIndex("mount"))
                    val body = cursor.getInt(cursor.getColumnIndex("body"))
                    val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                    val modelName = cursor.getString(cursor.getColumnIndex("model_name"))
                    val focalLength = cursor.getString(cursor.getColumnIndex("focal_length"))
                    val fSteps = cursor.getString(cursor.getColumnIndex("f_steps"))
                    lensList.add(LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps))
                }
            } finally {
                cursor?.close()
            }

            return lensList
        }

    val availableMountList: ArrayList<String>
        get() {
            val mounts = ArrayList<String>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select distinct mount from lens order by mount;", null)
                while (cursor!!.moveToNext()) {
                    val mount = cursor.getString(cursor.getColumnIndex("mount"))
                    if (!mount.isEmpty()) mounts.add(mount)
                }
            } finally {
                cursor?.close()
            }

            return mounts
        }


    val availableFilmBrandList: ArrayList<Pair<String,String>>
        get() {
            val fbs = ArrayList<Pair<String,String>>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select distinct manufacturer, brand from filmroll order by manufacturer;", null)
                while (cursor!!.moveToNext()) {
                    val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                    val brand = cursor.getString(cursor.getColumnIndex("brand"))
                    if (!brand.isEmpty()) fbs.add(Pair<String, String>(manufacturer, brand))
                }
            } finally {
                cursor?.close()
            }

            return fbs
        }

    val allFilmRolls: ArrayList<FilmRoll>
        get() {
            val filmList = ArrayList<FilmRoll>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select * from filmroll order by created desc;", null)
                while (cursor!!.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndex("_id"))
                    val name = cursor.getString(cursor.getColumnIndex("name"))
                    val created = cursor.getString(cursor.getColumnIndex("created"))
                    val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                    val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                    val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                    val brand = cursor.getString(cursor.getColumnIndex("brand"))
                    val iso = cursor.getInt(cursor.getColumnIndex("iso"))

                    val f = FilmRoll(id, name, created, lastModified, getCamera(camera)!!, manufacturer, brand, iso, 36)
                    filmList.add(f)
                    f.photos = getPhotosByFilmRollId(f.id)
                }
            } finally {
                cursor?.close()
            }

            return filmList
        }

    val allTags: ArrayList<Tag>
        get() {
            val tagList = ArrayList<Tag>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select * from tag;", null)
                while (cursor!!.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndex("_id"))
                    val label = cursor.getString(cursor.getColumnIndex("label"))
                    val refcnt = cursor.getInt(cursor.getColumnIndex("refcnt"))
                    tagList.add(Tag(id, label, refcnt))
                }
            } finally {
                cursor?.close()
            }

            return tagList
        }

    fun getFilmRollsByCamera(cameraid: Int): ArrayList<FilmRoll>{
        val filmList = ArrayList<FilmRoll>()

        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from filmroll where camera = ? order by created desc;", arrayOf(Integer.toString(cameraid)))
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                val brand = cursor.getString(cursor.getColumnIndex("brand"))
                val iso = cursor.getInt(cursor.getColumnIndex("iso"))

                val f = FilmRoll(id, name, created, lastModified, getCamera(camera)!!, manufacturer, brand, iso, 36)
                filmList.add(f)
                f.photos = getPhotosByFilmRollId(f.id)
            }
        } finally {
            cursor?.close()
        }

        return filmList
    }

    fun getFilmRollsByFilmBrand(manufacturer: String, brand: String): ArrayList<FilmRoll>{
        val filmList = ArrayList<FilmRoll>()

        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from filmroll where brand = ? and manufacturer = ? order by created desc;", arrayOf(brand, manufacturer))
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                //val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                //val brand = cursor.getString(cursor.getColumnIndex("brand"))
                val iso = cursor.getInt(cursor.getColumnIndex("iso"))

                val f = FilmRoll(id, name, created, lastModified, getCamera(camera)!!, manufacturer, brand, iso, 36)
                filmList.add(f)
                f.photos = getPhotosByFilmRollId(f.id)
            }
        } finally {
            cursor?.close()
        }

        return filmList
    }

    val accessories: ArrayList<Accessory>
        get() {
            val accessories = ArrayList<Accessory>()

            var cursor: Cursor? = null
            try {
                cursor = mDb!!.rawQuery("select * from accessory order by created desc;", null)
                while (cursor!!.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndex("_id"))
                    val type = cursor.getInt(cursor.getColumnIndex("type"))
                    val created = cursor.getString(cursor.getColumnIndex("created"))
                    val last_modified = cursor.getString(cursor.getColumnIndex("last_modified"))
                    val name = cursor.getString(cursor.getColumnIndex("name"))
                    val mount = cursor.getString(cursor.getColumnIndex("mount"))
                    val focal_length_factor = cursor.getDouble(cursor.getColumnIndex("focal_length_factor"))
                    accessories.add(Accessory(id, created, last_modified, type, name, mount, focal_length_factor))
                }
            } finally {
                cursor?.close()
            }

            return accessories
        }

    fun connection() {
        val helper = DatabaseHelper(mContext)
        mDb = helper.open()
    }

    override fun close() {
        mDb?.close()
        mDb = null
    }

    /* Camera */
    fun addCamera(camera: CameraSpec): Long {
        val cval = ContentValues()
        cval.put("type", camera.type)
        cval.put("created", Util.dateToStringUTC(camera.created))
        cval.put("last_modified", Util.dateToStringUTC(camera.lastModified))
        cval.put("mount", camera.mount)
        cval.put("manufacturer", camera.manufacturer)
        cval.put("model_name", camera.modelName)
        cval.put("format", camera.format)
        cval.put("ss_grain_size", camera.shutterSpeedGrainSize)
        cval.put("fastest_ss", camera.fastestShutterSpeed)
        cval.put("slowest_ss", camera.slowestShutterSpeed)
        cval.put("bulb_available", if (camera.bulbAvailable) 1 else 0)
        cval.put("shutter_speeds", camera.shutterSpeedSteps.joinToString(","))
        cval.put("ev_grain_size", camera.evGrainSize)
        cval.put("ev_width", camera.evWidth)
        return mDb!!.insert("camera", null, cval)
    }

    fun deleteCamera(id: Int) {
        val selectArgs = arrayOf(Integer.toString(id))
        mDb!!.delete("camera", "_id = ?", selectArgs)
    }

    fun updateCamera(camera: CameraSpec): Int {
        val selectArgs = arrayOf(Integer.toString(camera.id))
        val cval = ContentValues()
        cval.put("type", camera.type)
        cval.put("created", Util.dateToStringUTC(camera.created))
        cval.put("last_modified", Util.dateToStringUTC(camera.lastModified))
        cval.put("mount", camera.mount)
        cval.put("manufacturer", camera.manufacturer)
        cval.put("model_name", camera.modelName)
        cval.put("format", camera.format)
        cval.put("ss_grain_size", camera.shutterSpeedGrainSize)
        cval.put("fastest_ss", camera.fastestShutterSpeed)
        cval.put("slowest_ss", camera.slowestShutterSpeed)
        cval.put("bulb_available", camera.bulbAvailable)
        cval.put("shutter_speeds", camera.shutterSpeedSteps.joinToString(","))
        cval.put("ev_grain_size", camera.evGrainSize)
        cval.put("ev_width", camera.evWidth)

        return mDb!!.update("camera",
                cval,
                "_id = ?",
                selectArgs)
    }

    fun getCamera(id: Int): CameraSpec? {
        var c: CameraSpec? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from camera where _id = ?;", arrayOf(Integer.toString(id)))

            if (cursor!!.moveToFirst()) {
                val type = cursor.getInt(cursor.getColumnIndex("type"))
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                val mount = cursor.getString(cursor.getColumnIndex("mount"))
                val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                val modelName = cursor.getString(cursor.getColumnIndex("model_name"))
                val format = cursor.getInt(cursor.getColumnIndex("format"))
                val shutterSpeedGrainSize = cursor.getInt(cursor.getColumnIndex("ss_grain_size"))
                val fastestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("fastest_ss"))
                val slowestShutterSpeed = cursor.getDouble(cursor.getColumnIndex("slowest_ss"))
                val bulbAvailable = cursor.getInt(cursor.getColumnIndex("bulb_available")) != 0
                val shutterSpeedSteps = cursor.getString(cursor.getColumnIndex("shutter_speeds"))
                val evBiasStep = cursor.getInt(cursor.getColumnIndex("ev_grain_size"))
                val evBiasWidth = cursor.getInt(cursor.getColumnIndex("ev_width"))

                c = CameraSpec(id, type, created, lastModified, mount, manufacturer, modelName, format,
                        shutterSpeedGrainSize, fastestShutterSpeed, slowestShutterSpeed,
                        bulbAvailable, shutterSpeedSteps, evBiasStep, evBiasWidth)

            }
        } finally {
            cursor?.close()
        }
        //nullの場合どうする？
        return c
    }

    fun getCameraUsageCount(id: Int): Long {
        val filmrolls = DatabaseUtils.queryNumEntries(mDb, "filmroll", "camera = ?", arrayOf(Integer.toString(id)))
        val photos = DatabaseUtils.queryNumEntries(mDb, "photo", "camera = ?", arrayOf(Integer.toString(id)))
        return filmrolls + photos
    }

    /* Lens */
    fun addLens(lens: LensSpec): Long {
        val cval = ContentValues()
        cval.put("created", Util.dateToStringUTC(lens.created))
        cval.put("last_modified", Util.dateToStringUTC(lens.lastModified))
        cval.put("mount", lens.mount)
        cval.put("body", lens.body)
        cval.put("manufacturer", lens.manufacturer)
        cval.put("model_name", lens.modelName)
        cval.put("focal_length", lens.focalLength)
        cval.put("f_steps", lens.fSteps.joinToString(","))
        return mDb!!.insert("lens", null, cval)
    }

    fun deleteLens(id: Int) {
        val selectArgs = arrayOf(Integer.toString(id))
        mDb!!.delete("lens", "_id = ?", selectArgs)
    }

    fun updateLens(lens: LensSpec): Int {
        val selectArgs = arrayOf(Integer.toString(lens.id))
        val cval = ContentValues()
        cval.put("created", Util.dateToStringUTC(lens.created))
        cval.put("last_modified", Util.dateToStringUTC(lens.lastModified))
        cval.put("mount", lens.mount)
        cval.put("body", lens.body)
        cval.put("manufacturer", lens.manufacturer)
        cval.put("model_name", lens.modelName)
        cval.put("focal_length", lens.focalLength)
        cval.put("f_steps", lens.fSteps.joinToString(","))
        return mDb!!.update("lens",
                cval,
                "_id = ?",
                selectArgs)
    }

    fun getLensesByMount(mount: String): ArrayList<LensSpec> {

        val lensList = ArrayList<LensSpec>()

        var cursor: Cursor? = null
        try {
            val selectArgs = arrayOf(mount)
            cursor = mDb!!.rawQuery("select * from lens where mount = ? order by created desc;", selectArgs)
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                //String mount = cursor.getString(cursor.getColumnIndex("mount"));
                val body = cursor.getInt(cursor.getColumnIndex("body"))
                val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                val modelName = cursor.getString(cursor.getColumnIndex("model_name"))
                val focalLength = cursor.getString(cursor.getColumnIndex("focal_length"))
                val fSteps = cursor.getString(cursor.getColumnIndex("f_steps"))
                lensList.add(LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps))
            }
        } finally {
            cursor?.close()
        }

        return lensList
    }

    fun getFixedLensIdByBody(body: Int): Int {
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select _id from lens where body = ?;", arrayOf(Integer.toString(body)))

            if (cursor!!.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex("_id"))
            }
        } finally {
            cursor?.close()
        }
        //nullの場合どうする？
        return -1
    }

    fun getLens(id: Int): LensSpec? {
        var l: LensSpec? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from lens where _id = ?;", arrayOf(Integer.toString(id)))

            if (cursor!!.moveToFirst()) {
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                val mount = cursor.getString(cursor.getColumnIndex("mount"))
                val body = cursor.getInt(cursor.getColumnIndex("body"))
                val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                val modelName = cursor.getString(cursor.getColumnIndex("model_name"))
                val focalLength = cursor.getString(cursor.getColumnIndex("focal_length"))
                val fSteps = cursor.getString(cursor.getColumnIndex("f_steps"))
                l = LensSpec(id, created, lastModified, mount, body, manufacturer, modelName, focalLength, fSteps)
            }
        } finally {
            cursor?.close()
        }
        //nullの場合どうする？
        return l
    }

    fun getLensUsageCount(id: Int): Long {
        return DatabaseUtils.queryNumEntries(mDb, "photo", "lens = ?", arrayOf(Integer.toString(id)))
    }

    /* Film roll */
    fun addFilmRoll(f: FilmRoll): Long {
        val cval = ContentValues()
        cval.put("name", f.name)
        cval.put("created", Util.dateToStringUTC(f.created))
        cval.put("last_modified", Util.dateToStringUTC(f.lastModified))
        cval.put("camera", f.camera.id)
        cval.put("format", f.camera.format)
        cval.put("manufacturer", f.manufacturer)
        cval.put("brand", f.brand)
        cval.put("iso", f.iso)
        return mDb!!.insert("filmroll", null, cval)
    }

    fun updateFilmRoll(f: FilmRoll): Int {
        val selectArgs = arrayOf(Integer.toString(f.id))
        val cval = ContentValues()
        cval.put("name", f.name)
        cval.put("created", Util.dateToStringUTC(f.created))
        cval.put("last_modified", Util.dateToStringUTC(f.lastModified))
        cval.put("camera", f.camera.id)
        cval.put("format", f.camera.format)
        cval.put("manufacturer", f.manufacturer)
        cval.put("brand", f.brand)
        cval.put("iso", f.iso)
        return mDb!!.update("filmroll",
                cval,
                "_id = ?",
                selectArgs)
    }

    fun deleteFilmRoll(id: Int) {
        val selectArgs = arrayOf(Integer.toString(id))
        val photos = getPhotosByFilmRollId(id)
        for(p in photos){
            deletePhoto(p.id) //tagのリファレンスカウントも管理する必要があるため
        }
        mDb!!.delete("filmroll", "_id = ?", selectArgs)
    }

    fun getFilmRoll(id: Int): FilmRoll? {
        var f: FilmRoll? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from filmroll where _id = ?;", arrayOf(Integer.toString(id)))

            if (cursor!!.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val lastModified = cursor.getString(cursor.getColumnIndex("last_modified"))
                val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                val manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"))
                val brand = cursor.getString(cursor.getColumnIndex("brand"))
                val iso = cursor.getInt(cursor.getColumnIndex("iso"))
                f = FilmRoll(id, name, created, lastModified, getCamera(camera)!!, manufacturer, brand, iso, 36/* temp value */)
            }
        } finally {
            cursor?.close()
        }
        //nullの場合どうする？
        return f
    }

    fun getAllFavedPhotos(): ArrayList<Photo> {
        val photos = ArrayList<Photo>()

        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from photo where favorite = 1 order by date desc;", null)
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val filmroll = cursor.getInt(cursor.getColumnIndex("filmroll"))
                val index = cursor.getInt(cursor.getColumnIndex("_index"))
                val date = cursor.getString(cursor.getColumnIndex("date"))
                val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                val lens = cursor.getInt(cursor.getColumnIndex("lens"))
                val focalLength = cursor.getDouble(cursor.getColumnIndex("focal_length"))
                val aperture = cursor.getDouble(cursor.getColumnIndex("aperture"))
                val shutterSpeed = cursor.getDouble(cursor.getColumnIndex("shutter_speed"))
                val ev = cursor.getDouble(cursor.getColumnIndex("exp_compensation"))
                val ttl = cursor.getDouble(cursor.getColumnIndex("ttl_light_meter"))
                val location = cursor.getString(cursor.getColumnIndex("location"))
                val memo = cursor.getString(cursor.getColumnIndex("memo"))
                val accessoriesStr = cursor.getString(cursor.getColumnIndex("accessories"))
                val latitude: Double
                val longitude: Double
                if (cursor.getType(cursor.getColumnIndex("latitude")) == FIELD_TYPE_NULL) {
                    latitude = 999.0
                } else {
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"))
                }
                if (cursor.getType(cursor.getColumnIndex("longitude")) == FIELD_TYPE_NULL) {
                    longitude = 999.0
                } else {
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"))
                }
                val supplementalImgStr = cursor.getString(cursor.getColumnIndex("suppimgs"))
                val favorite = cursor.getInt(cursor.getColumnIndex("favorite"))
                photos.add(Photo(id, filmroll, index, date, camera, lens, focalLength,
                        aperture, shutterSpeed, ev, ttl, location, latitude, longitude, memo,
                        accessoriesStr, supplementalImgStr, favorite != 0))
            }
        } finally {
            cursor?.close()
        }

        return photos
    }

    fun getPhotosByFilmRollId(filmRollId: Int): ArrayList<Photo> {

        val photos = ArrayList<Photo>()

        var cursor: Cursor? = null
        try {
            val selectArgs = arrayOf(Integer.toString(filmRollId))
            cursor = mDb!!.rawQuery("select * from photo where filmroll = ? order by _index asc;", selectArgs)
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val index = cursor.getInt(cursor.getColumnIndex("_index"))
                val date = cursor.getString(cursor.getColumnIndex("date"))
                val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                val lens = cursor.getInt(cursor.getColumnIndex("lens"))
                val focalLength = cursor.getDouble(cursor.getColumnIndex("focal_length"))
                val aperture = cursor.getDouble(cursor.getColumnIndex("aperture"))
                val shutterSpeed = cursor.getDouble(cursor.getColumnIndex("shutter_speed"))
                val ev = cursor.getDouble(cursor.getColumnIndex("exp_compensation"))
                val ttl = cursor.getDouble(cursor.getColumnIndex("ttl_light_meter"))
                val location = cursor.getString(cursor.getColumnIndex("location"))
                val memo = cursor.getString(cursor.getColumnIndex("memo"))
                val accessoriesStr = cursor.getString(cursor.getColumnIndex("accessories"))
                val latitude: Double
                val longitude: Double
                if (cursor.getType(cursor.getColumnIndex("latitude")) == FIELD_TYPE_NULL) {
                    latitude = 999.0
                } else {
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"))
                }
                if (cursor.getType(cursor.getColumnIndex("longitude")) == FIELD_TYPE_NULL) {
                    longitude = 999.0
                } else {
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"))
                }
                val supplementalImgStr = cursor.getString(cursor.getColumnIndex("suppimgs"))
                val favorite = cursor.getInt(cursor.getColumnIndex("favorite"))
                photos.add(Photo(id, filmRollId, index, date, camera, lens, focalLength,
                        aperture, shutterSpeed, ev, ttl, location, latitude, longitude, memo,
                        accessoriesStr, supplementalImgStr, favorite != 0))
            }
        } finally {
            cursor?.close()
        }

        return photos
    }

    /* Photo */
    fun addPhoto(p: Photo): Long {
        val cval = ContentValues()
        cval.put("filmroll", p.filmrollid)
        cval.put("_index", p.frameIndex)
        cval.put("date", p.date)
        cval.put("camera", p.cameraid)
        cval.put("lens", p.lensid)
        cval.put("focal_length", p.focalLength)
        cval.put("aperture", p.aperture)
        cval.put("shutter_speed", p.shutterSpeed)
        cval.put("exp_compensation", p.expCompensation)
        cval.put("ttl_light_meter", p.ttlLightMeter)
        cval.put("location", p.location)
        cval.put("latitude", p.latitude)
        cval.put("longitude", p.longitude)
        cval.put("memo", p.memo)
        cval.put("accessories", p.accessoriesStr)
        cval.put("suppimgs", p.supplementalImagesStr)
        cval.put("favorite", if(p.favorite) 1 else 0)
        return mDb!!.insert("photo", null, cval)
    }

    fun updatePhoto(p: Photo): Int {
        val selectArgs = arrayOf(Integer.toString(p.id))
        val cval = ContentValues()
        cval.put("filmroll", p.filmrollid)
        cval.put("_index", p.frameIndex)
        cval.put("date", p.date)
        cval.put("camera", p.cameraid)
        cval.put("lens", p.lensid)
        cval.put("focal_length", p.focalLength)
        cval.put("aperture", p.aperture)
        cval.put("shutter_speed", p.shutterSpeed)
        cval.put("exp_compensation", p.expCompensation)
        cval.put("ttl_light_meter", p.ttlLightMeter)
        cval.put("location", p.location)
        cval.put("latitude", p.latitude)
        cval.put("longitude", p.longitude)
        cval.put("memo", p.memo)
        cval.put("accessories", p.accessoriesStr)
        cval.put("suppimgs", p.supplementalImagesStr)
        cval.put("favorite", if(p.favorite) 1 else 0)
        return mDb!!.update("photo",
                cval,
                "_id = ?",
                selectArgs)
    }

    fun deletePhoto(id: Int) {
        val tagmaps = getTagMapsByPhoto(id)
        val tags = resolveTagMaps(tagmaps)
        for(tm in tagmaps) {
            deleteTagMap(tm.id)
        }
        for(t in tags){
            if(t.refcnt == 1){
                deleteTag(t.id)
            }else{
                updateTag(Tag(t.id, t.label, t.refcnt - 1))
            }
        }

        val selectArgs = arrayOf(Integer.toString(id))
        mDb!!.delete("photo", "_id = ?", selectArgs)
    }

    fun getPhoto(id: Int): Photo? {
        var p: Photo? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from photo where _id = ?;", arrayOf(Integer.toString(id)))
            if (cursor!!.moveToFirst()) {
                //int id = cursor.getInt(cursor.getColumnIndex("_id"));
                val index = cursor.getInt(cursor.getColumnIndex("_index"))
                val filmroll = cursor.getInt(cursor.getColumnIndex("filmroll"))
                val date = cursor.getString(cursor.getColumnIndex("date"))
                val camera = cursor.getInt(cursor.getColumnIndex("camera"))
                val lens = cursor.getInt(cursor.getColumnIndex("lens"))
                val focalLength = cursor.getDouble(cursor.getColumnIndex("focal_length"))
                val aperture = cursor.getDouble(cursor.getColumnIndex("aperture"))
                val shutterSpeed = cursor.getDouble(cursor.getColumnIndex("shutter_speed"))
                val ev = cursor.getDouble(cursor.getColumnIndex("exp_compensation"))
                val ttl = cursor.getDouble(cursor.getColumnIndex("ttl_light_meter"))
                val location = cursor.getString(cursor.getColumnIndex("location"))
                val memo = cursor.getString(cursor.getColumnIndex("memo"))
                val accessoriesStr = cursor.getString(cursor.getColumnIndex("accessories"))
                val latitude: Double
                val longitude: Double
                if (cursor.getType(cursor.getColumnIndex("latitude")) == FIELD_TYPE_NULL) {
                    latitude = 999.0
                } else {
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"))
                }
                if (cursor.getType(cursor.getColumnIndex("longitude")) == FIELD_TYPE_NULL) {
                    longitude = 999.0
                } else {
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"))
                }
                val supplementalImgStr = cursor.getString(cursor.getColumnIndex("suppimgs"))
                val favorite = cursor.getInt(cursor.getColumnIndex("favorite"))
                p = Photo(id, filmroll, index, date, camera, lens, focalLength,
                        aperture, shutterSpeed, ev, ttl, location, latitude, longitude, memo, accessoriesStr,
                        supplementalImgStr, favorite != 0)
            }
        } finally {
            cursor?.close()
        }
        //nullの場合どうする？
        return p
    }

    fun addAccessory(a: Accessory): Long {
        val cval = ContentValues()
        cval.put("name", a.name)
        cval.put("type", a.type)
        cval.put("created", Util.dateToStringUTC(a.created))
        cval.put("last_modified", Util.dateToStringUTC(a.last_modified))
        cval.put("mount", a.mount)
        cval.put("focal_length_factor", a.focal_length_factor)
        return mDb!!.insert("accessory", null, cval)
    }

    fun updateAccessory(a: Accessory): Int {
        val selectArgs = arrayOf(Integer.toString(a.id))
        val cval = ContentValues()
        cval.put("name", a.name)
        cval.put("type", a.type)
        cval.put("created", Util.dateToStringUTC(a.created))
        cval.put("last_modified", Util.dateToStringUTC(a.last_modified))
        cval.put("mount", a.mount)
        cval.put("focal_length_factor", a.focal_length_factor)
        return mDb!!.update("accessory",
                cval,
                "_id = ?",
                selectArgs)
    }

    fun deleteAccessory(id: Int) {
        val selectArgs = arrayOf(Integer.toString(id))
        mDb!!.delete("accessory", "_id = ?", selectArgs)
    }

    fun getAccessory(id: Int): Accessory? {
        var a: Accessory? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from accessory where _id = ?;", arrayOf(Integer.toString(id)))
            if (cursor!!.moveToFirst()) {
                val type = cursor.getInt(cursor.getColumnIndex("type"))
                val created = cursor.getString(cursor.getColumnIndex("created"))
                val last_modified = cursor.getString(cursor.getColumnIndex("last_modified"))
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val mount = cursor.getString(cursor.getColumnIndex("mount"))
                val focal_length_factor = cursor.getDouble(cursor.getColumnIndex("focal_length_factor"))
                a = Accessory(id, created, last_modified, type, name, mount, focal_length_factor)
            }
        } finally {
            cursor?.close()
        }
        //nullの場合どうする？
        return a
    }

    fun getAccessoryUsageCount(id: Int): Long {
        //long photos = DatabaseUtils.queryNumEntries(mDb, "photo","camera = ?",new String[]{ Integer.toString(id) });
        //return filmrolls + photos;
        return 0
    }

    fun getAccessoryUsed(id: Int): Boolean {
        var cursor: Cursor? = null
        var result = false
        try {
            cursor = mDb!!.rawQuery("select * from photo where accessories like '%/" + Integer.toString(id) + "/%';", null)
            while (cursor!!.moveToNext()) {
                result = true
                break
            }
        } finally {
            cursor?.close()
        }
        return result
    }

    // add, get, delete, update
    // 他とはちょっとインターフェースが違うぞい
    fun addTag(t: Tag): Long{
        val cval = ContentValues()
        cval.put("label", t.label)
        cval.put("refcnt", t.refcnt)
        return mDb!!.insert("tag", null, cval)
    }

    fun updateTag(t: Tag): Int {
        val selectArgs = arrayOf(Integer.toString(t.id))
        val cval = ContentValues()
        cval.put("label", t.label)
        cval.put("refcnt", t.refcnt)
        return mDb!!.update("tag",
                cval,
                "_id = ?",
                selectArgs)
    }

    fun deleteTag(id: Int){
        val selectArgs = arrayOf(Integer.toString(id))
        mDb!!.delete("tag", "_id = ?", selectArgs)
    }

    fun addTagMap(tm: TagMap): Long{
        val cval = ContentValues()
        cval.put("tag_id", tm.tagId)
        cval.put("photo_id", tm.photoId)
        cval.put("filmroll_id", tm.filmRollId)
        return mDb!!.insert("tagmap", null, cval)
    }

    fun deleteTagMap(tmid: Int){
        val selectArgs = arrayOf(Integer.toString(tmid))
        mDb!!.delete("tagmap", "_id = ?", selectArgs)
    }

    fun getTagById(tagid: Int): Tag?{
        var t: Tag? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from tag where _id = ?;", arrayOf(Integer.toString(tagid)))
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val label = cursor.getString(cursor.getColumnIndex("label"))
                val refcnt = cursor.getInt(cursor.getColumnIndex("refcnt"))
                t = Tag(id, label, refcnt)
            }
        } finally {
            cursor?.close()
        }
        return t
    }

    fun getTagByLabel(label: String): Tag?{
        var t: Tag? = null
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from tag where label = ?;", arrayOf(label))
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val label = cursor.getString(cursor.getColumnIndex("label"))
                val refcnt = cursor.getInt(cursor.getColumnIndex("refcnt"))
                t = Tag(id, label, refcnt)
            }
        } finally {
            cursor?.close()
        }
        return t
    }

    fun tagPhoto(photoId: Int, filmRollId: Int, tags: ArrayList<String>): Boolean {
        val currentTagMaps = getTagMapsByPhoto(photoId)
        val currentTags = resolveTagMaps(currentTagMaps)
        val createList = ArrayList<String>()
        for(label in tags){
            val existingTag = currentTags.find { it.label == label }
            if (existingTag == null){
                createList.add(label)
            }else{
                currentTags.remove(existingTag) //existingTagは現状維持の対象
                for(tm in currentTagMaps){ // minSdkVersionが24からじゃないとremoveIfが使えない
                    if(tm.tagId == existingTag.id){
                        currentTagMaps.remove(tm)
                        break
                    }
                }
            }
        }
        //作成またはrefcntをインクリメント
        for(label in createList){
            val t = getTagByLabel(label)
            if(t == null) {
                val tagId = addTag(Tag(-1, label, 1))
                addTagMap(TagMap(-1, photoId, filmRollId, tagId.toInt()))
            }else{
                updateTag(Tag(t.id, label, t.refcnt + 1))
                addTagMap(TagMap(-1, photoId, filmRollId, t.id))
            }
        }

        //currentTagsとcurrentTagMapsに残ったものはrefcntをデクリメントもしくは削除の対象
        for(t in currentTags){
            val tm = currentTagMaps.find { it.tagId == t.id }
            if(tm != null) deleteTagMap(tm.id)
            if(t.refcnt == 1){ // 削除対象
                deleteTag(t.id)
            }else{ // デクリメントだけ
                updateTag(Tag(t.id, t.label, t.refcnt - 1))
            }
        }

        return false
    }

    fun getTagMapsByPhoto(photoId: Int): ArrayList<TagMap>{
        val tagMapList = ArrayList<TagMap>()

        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select * from tagmap where photo_id = ?;", arrayOf(Integer.toString(photoId)))
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val photoId = cursor.getInt(cursor.getColumnIndex("photo_id"))
                val tagId = cursor.getInt(cursor.getColumnIndex("tag_id"))
                val filmRollId = cursor.getInt(cursor.getColumnIndex("filmroll_id"))
                tagMapList.add(TagMap(id, photoId, filmRollId, tagId))
            }
        } finally {
            cursor?.close()
        }
        return tagMapList
    }

    fun resolveTagMaps(tml: ArrayList<TagMap>): ArrayList<Tag>{
        val tagList = ArrayList<Tag>()
        for(tm in tml){
            val t = getTagById(tm.tagId)
            if (t != null) tagList.add(t)
        }
        return tagList
    }

    fun getTagsByPhoto(photoId: Int): ArrayList<Tag>{
        val tagMapList = getTagMapsByPhoto(photoId)
        return resolveTagMaps(tagMapList)
    }

    fun makeArray(size: Int):String{
        val sb = StringBuilder("?")
        if(size == 1) return sb.toString()
        for(i in 1.rangeTo(size-1)){
            sb.append(",?")
        }
        return sb.toString()
    }

    fun getPhotoFromCursor(cursor: Cursor):Photo{
        Log.d("getPhotoFromCursor", cursor.getInt(cursor.getColumnIndex("count(*)")).toString())
        val id = cursor.getInt(cursor.getColumnIndex("_id"))
        val filmroll = cursor.getInt(cursor.getColumnIndex("filmroll"))
        val index = cursor.getInt(cursor.getColumnIndex("_index"))
        val date = cursor.getString(cursor.getColumnIndex("date"))
        val camera = cursor.getInt(cursor.getColumnIndex("camera"))
        val lens = cursor.getInt(cursor.getColumnIndex("lens"))
        val focalLength = cursor.getDouble(cursor.getColumnIndex("focal_length"))
        val aperture = cursor.getDouble(cursor.getColumnIndex("aperture"))
        val shutterSpeed = cursor.getDouble(cursor.getColumnIndex("shutter_speed"))
        val ev = cursor.getDouble(cursor.getColumnIndex("exp_compensation"))
        val ttl = cursor.getDouble(cursor.getColumnIndex("ttl_light_meter"))
        val location = cursor.getString(cursor.getColumnIndex("location"))
        val memo = cursor.getString(cursor.getColumnIndex("memo"))
        val accessoriesStr = cursor.getString(cursor.getColumnIndex("accessories"))
        val latitude: Double
        val longitude: Double
        if (cursor.getType(cursor.getColumnIndex("latitude")) == FIELD_TYPE_NULL) {
            latitude = 999.0
        } else {
            latitude = cursor.getDouble(cursor.getColumnIndex("latitude"))
        }
        if (cursor.getType(cursor.getColumnIndex("longitude")) == FIELD_TYPE_NULL) {
            longitude = 999.0
        } else {
            longitude = cursor.getDouble(cursor.getColumnIndex("longitude"))
        }
        val supplementalImgStr = cursor.getString(cursor.getColumnIndex("suppimgs"))
        val favorite = cursor.getInt(cursor.getColumnIndex("favorite"))
        return Photo(id, filmroll, index, date, camera, lens, focalLength,
                aperture, shutterSpeed, ev, ttl, location, latitude, longitude, memo,
                accessoriesStr, supplementalImgStr, favorite != 0)
    }

    fun getPhotosByAndQuery(tags: ArrayList<Tag>): ArrayList<Photo>{
        val photos = ArrayList<Photo>()
        var cursor: Cursor? = null
        try {
            val selectionArgs = tags.map { it.label }
            cursor = mDb!!.rawQuery(
                    "select p.*, count(*) from photo p, tagmap tm, tag t "+
                            "where tm.tag_id = t._id "+
                            "and (t.label in ("+makeArray(tags.size)+")) "+
                            "and p._id = tm.photo_id "+
                            "group by p._id "+
                            "having count(*) = "+tags.size.toString()+";", selectionArgs.toTypedArray())
                                               //having count(*) = ?ではダメらしい。
            while (cursor!!.moveToNext()) {
                photos.add(getPhotoFromCursor(cursor))
            }
        } finally {
            cursor?.close()
        }

        return photos
    }

    fun getPhotosByOrQuery(tags: ArrayList<Tag>): ArrayList<Photo>{
        val photos = ArrayList<Photo>()
        var cursor: Cursor? = null
        try {
            val selectionArgs = tags.map { it.label }
            cursor = mDb!!.rawQuery(
                    "select p.* from photo p, tagmap tm, tag t "+
                            "where tm.tag_id = t._id "+
                            "and (t.label in ("+makeArray(tags.size)+")) "+
                            "and p._id = tm.photo_id "+
                            "group by p._id;", selectionArgs.toTypedArray())
            while (cursor!!.moveToNext()) {
                photos.add(getPhotoFromCursor(cursor))
            }
        } finally {
            cursor?.close()
        }

        return photos
    }

    fun getConversionState() : Int {
        var cursor: Cursor? = null
        try {
            cursor = mDb!!.rawQuery("select path_conv_done from trisquel_metadata;", null,null)
            if(cursor.moveToFirst()){
                return cursor.getInt(cursor.getColumnIndex("path_conv_done"))
            }
        } finally {
            cursor?.close()
        }
        return 0
    }

    fun setConversionState(value : Int){
        val cval = ContentValues()
        cval.put("path_conv_done", value)
        mDb!!.update("trisquel_metadata", cval, "rowid = ?", arrayOf("1"))
    }

    // suppImgは生の絶対パスが入っているが、これはAndroid11では使えなくなるので、content://に変換する必要がある
    fun getPhotos4Conversion() : ArrayList<Photo>{
        val photoList = ArrayList<Photo>()
        var cursor: Cursor? = null
        try {
            // ["/path/to/file"] となっているものは変換する必要がある。先頭の ["\/ で判定可能。
            cursor = mDb!!.rawQuery("select _id from photo where suppimgs like ? || '%';", arrayOf("[\"\\/"),null)
            while (cursor!!.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                photoList.add(getPhoto(id)!!)
            }
        } finally {
            cursor?.close()
        }

        return photoList
    }

    fun doPhotoConversion(p: Photo){
        val selectArgs = arrayOf(Integer.toString(p.id))
        val cval = ContentValues()
        cval.put("suppimgs", p.supplementalImagesStr)
        cval.put("memo", p.memo)
        mDb!!.update("photo",
                cval,
                "_id = ?",
                selectArgs)
    }
}
