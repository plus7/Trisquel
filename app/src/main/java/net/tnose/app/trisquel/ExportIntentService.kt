package net.tnose.app.trisquel

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportIntentService : IntentService{

    companion object {
        val ACTION_START_EXPORT = "net.tnose.app.trisquel.action.START_EXPORT"
        val ACTION_EXPORT_PROGRESS = "net.tnose.app.trisquel.action.EXPORT_PROGRESS"
        val PARAM_DIR = "dir"
        val PARAM_ZIPFILE = "zipfile"
        val PARAM_PERCENTAGE = "percentage"
        val PARAM_STATUS = "status"

        internal fun startExport(context: Context, dir: String, zipfile: String){
            val intent = Intent(context, ExportIntentService::class.java)
            intent.action = ACTION_START_EXPORT
            intent.putExtra(PARAM_DIR, dir)
            intent.putExtra(PARAM_ZIPFILE, zipfile)
            context.startService(intent)
        }
    }
    var handler: Handler? = null

    constructor() : super("ExportIntentService") {
        handler = Handler()
    }

    fun bcastProgress(percentage: Double, status: String){
        val intent = Intent()
        intent.putExtra(PARAM_PERCENTAGE, percentage)
        intent.putExtra(PARAM_STATUS, status)
        intent.action = ACTION_EXPORT_PROGRESS
        LocalBroadcastManager.getInstance(baseContext).sendBroadcast(intent)
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

    fun notifyMediaScanner(backupZip: File){
        // MediaScannerに教えないとすぐにはPCから見えない
        val contentUri = Uri.fromFile(backupZip)
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
        this.sendBroadcast(mediaScanIntent)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        when(intent.action){
            ACTION_START_EXPORT -> {
                val dir = intent.getStringExtra(PARAM_DIR)
                val zipfile = intent.getStringExtra(PARAM_ZIPFILE)
                val sd = File(dir!!)
                val backupZip = File(sd, zipfile!!)
                backupToZip(backupZip)
                notifyMediaScanner(backupZip)
                handler?.post(Runnable {
                    Toast.makeText(this, "Wrote to " + backupZip.absolutePath, Toast.LENGTH_LONG).show()
                })
            }
        }
    }

}