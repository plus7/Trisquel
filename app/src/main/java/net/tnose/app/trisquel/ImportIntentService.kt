package net.tnose.app.trisquel

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.HashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.collections.HashMap
import kotlin.collections.listOf
import kotlin.collections.set


class ImportIntentService : IntentService {

    companion object {
        @Volatile
        var shouldContinue = true

        val ACTION_START_IMPORT = "net.tnose.app.trisquel.action.START_IMPORT"
        val ACTION_IMPORT_PROGRESS = "net.tnose.app.trisquel.action.IMPORT_PROGRESS"
        val PARAM_DIR = "dir"
        val PARAM_ZIPFILE = "zipfile"
        val PARAM_PERCENTAGE = "percentage"
        val PARAM_STATUS = "status"

        internal fun startImport(context: Context, dir: String, zipfile: String){
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_START_IMPORT
            intent.putExtra(PARAM_DIR, dir)
            intent.putExtra(PARAM_ZIPFILE, zipfile)
            context.startService(intent)
        }

        const val SUCCESS = 0
        const val VERSION_UNMATCH = -1
    }
    var handler: Handler? = null
    val cameraIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val lensIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val accessoryIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val filmrollIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val photoIdOld2NewMap: HashMap<Int, Int> = HashMap()

    constructor() : super("ImportIntentService") {
        handler = Handler()
    }

    fun bcastProgress(percentage: Double, status: String){
        val intent = Intent()
        intent.putExtra(PARAM_PERCENTAGE, percentage)
        intent.putExtra(PARAM_STATUS, status)
        intent.action = ACTION_IMPORT_PROGRESS
        LocalBroadcastManager.getInstance(baseContext).sendBroadcast(intent)
    }

    fun compareZipEntryAndFile(zis: ZipInputStream, ze: ZipEntry, file: File){

    }

    fun clearAllDbEntries(dao: TrisquelDao){
        dao.deleteAll()
    }

    fun doImport(){
        val dao = TrisquelDao(this)
        dao.connection()
        dao.close()
    }

    fun backupToZip(zipFile: File) {
        val zos = ZipOutputStream(FileOutputStream(zipFile))
        zos.setMethod(ZipOutputStream.DEFLATED)
        val osw = OutputStreamWriter(zos, "UTF-8")
        val dao = TrisquelDao(this)
        dao.connection()

        bcastProgress(0.0, "Writing metadata...")

        // Metadata
        val ze = ZipEntry("metadata.json")
        zos.putNextEntry(ze)
        val metadata = JSONObject()
        metadata.put("DB_VERSION", DatabaseHelper.DATABASE_VERSION)

        osw.write(metadata.toString())
        osw.flush()
        zos.closeEntry()

        var percentage = 1.0

        bcastProgress(percentage, "Writing database entries...")
        val types = listOf("camera", "lens", "filmroll", "accessory", "tag", "tagmap")
        for(type in types) {
            val ze = ZipEntry(type + ".json")
            zos.putNextEntry(ze)
            val entries = dao.getAllEntriesJSON(type)
            osw.write(entries.toString())
            osw.flush()
            zos.closeEntry()
            percentage += 1.0
            bcastProgress(percentage, "Writing database entries...")
        }

        val zep = ZipEntry("photo.json")
        zos.putNextEntry(zep)
        val entries = dao.getAllEntriesJSON("photo")
        osw.write(entries.toString())
        osw.flush()
        zos.closeEntry()
        percentage += 1.0

        val hs = HashSet<String>()
        val photosPercentage = 100.0 - percentage
        for (i in 0..entries.length()-1){
            bcastProgress(percentage + photosPercentage * i.toDouble() / entries.length().toDouble(), "")
            val e = entries.getJSONObject(i)
            val s = e.getString("suppimgs")
            if(s.isEmpty()) continue
            val imgs = JSONArray(s)
            for (j in 0..imgs.length() - 1) {
                val path = imgs[j].toString()
                if(hs.contains(path)) continue
                hs.add(path)
                val f = File(path)

                val di = i.toDouble()
                val dj = j.toDouble()
                bcastProgress(percentage +
                        photosPercentage * (di + dj / imgs.length().toDouble()) / entries.length().toDouble()
                        , f.name)

                if(!f.exists()) continue
                val ze = ZipEntry("imgs" + path)
                zos.putNextEntry(ze)
                try {
                    val buf = ByteArray(1024*128)
                    val bis = BufferedInputStream(FileInputStream(f))
                    while (true) {
                        val len = bis.read(buf)
                        if (len < 0) break
                        zos.write(buf, 0, len)
                    }
                } catch (e: IOException) {
                    Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                } finally {
                    zos.closeEntry()
                }
            }
        }

        dao.close()
        zos.close()
        bcastProgress(100.0, "Complete.")
    }

    fun getJSONFromEntry(zf: ZipFile, entryName: String): JSONObject{
        val ze = zf.getEntry(entryName)
        val stream = zf.getInputStream(ze)
        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos)
        val data = ByteArray(4*1024)
        while (true) {
            val count = stream.read(data, 0, 4*1024)
            if (count < 0) break
            bos.write(data, 0, count)
        }
        bos.flush()
        bos.close()
        stream.close()
        return JSONObject(baos.toString())
    }

    fun importFromZip(file: File, merge: Boolean): Int{
        val zipfile = ZipFile(file)

        val metadataJSON = getJSONFromEntry(zipfile, "metadata.json")
        val dbver = metadataJSON.getInt("DB_VERSION")
        Log.d("importFromZip", "DB_VERSION=" + dbver.toString())
        if(dbver > DatabaseHelper.DATABASE_VERSION) return VERSION_UNMATCH

        val dao = TrisquelDao(this)
        dao.connection()

        if(merge) dao.deleteAll()

        val cameraJSON = JSONArray(getJSONFromEntry(zipfile, "camera.json"))
        for(i in 0 until cameraJSON.length()) {
            val id_pair = dao.mergeCameraJSON(cameraJSON.getJSONObject(i))
            cameraIdOld2NewMap[id_pair.first] = id_pair.second
        }

        val lensJSON = JSONArray(getJSONFromEntry(zipfile, "lens.json"))
        for(i in 0 until lensJSON.length()) {
            val id_pair = dao.mergeLensJSON(lensJSON.getJSONObject(i), cameraIdOld2NewMap)
            lensIdOld2NewMap[id_pair.first] = id_pair.second
        }

        val accessoryJSON = JSONArray(getJSONFromEntry(zipfile, "accessory.json"))
        for(i in 0 until accessoryJSON.length()) {
            val id_pair = dao.mergeAccessoryJSON(accessoryJSON.getJSONObject(i))
            accessoryIdOld2NewMap[id_pair.first] = id_pair.second
        }

        val filmrollJSON = JSONArray(getJSONFromEntry(zipfile, "filmroll.json"))
        for(i in 0 until filmrollJSON.length()) {
            val id_pair = dao.mergeFilmRollJSON(filmrollJSON.getJSONObject(i), cameraIdOld2NewMap)
            filmrollIdOld2NewMap[id_pair.first] = id_pair.second
        }

        val photoJSON = JSONArray(getJSONFromEntry(zipfile, "photo.json"))
        for(i in 0 until photoJSON.length()) {
            val id_pair = dao.mergePhotoJSON(photoJSON.getJSONObject(i),
                    cameraIdOld2NewMap, lensIdOld2NewMap, filmrollIdOld2NewMap, accessoryIdOld2NewMap)
            photoIdOld2NewMap[id_pair.first] = id_pair.second
        }

        val tagJSON = JSONArray(getJSONFromEntry(zipfile, "tag.json"))
        val tagmapJSON = JSONArray(getJSONFromEntry(zipfile,"tagmap.json"))
        dao.mergeTagMapJSON(tagmapJSON, tagJSON, filmrollIdOld2NewMap, photoIdOld2NewMap)

        dao.close()
        return SUCCESS
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        when(intent.action){
            ACTION_START_IMPORT -> {
                val dir = intent.getStringExtra(PARAM_DIR)
                val zipfile = intent.getStringExtra(PARAM_ZIPFILE)
                val sd = File(dir!!)
                val backupZip = File(sd, zipfile!!)
                importFromZip(backupZip, true)
                handler?.post(Runnable {
                    Toast.makeText(this, "Import from " + backupZip.absolutePath + " completed", Toast.LENGTH_LONG).show()
                })
            }
        }
    }

}