package net.tnose.app.trisquel

import android.R
import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class ExportIntentService : IntentService{

    companion object {
        @Volatile
        var shouldContinue = true

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

    fun backupToZip(zipFile: File): Boolean {
        val zos = ZipOutputStream(FileOutputStream(zipFile))
        zos.setMethod(ZipOutputStream.DEFLATED)
        val osw = OutputStreamWriter(zos, "UTF-8")
        val dao = TrisquelDao(this)
        dao.connection()

        var completed = false

        try {
            bcastProgress(0.0, "Writing metadata...")

            // Metadata
            val ze = ZipEntry("metadata.json")
            zos.putNextEntry(ze)
            val metadata = JSONObject()
            metadata.put("DB_VERSION", DatabaseHelper.DATABASE_VERSION)
            metadata.put("EXPORT_MODE", 1) //0: DB Only, 1: DB+IMAGES
            metadata.put("EXTERNAL_STORAGE_PATH", Environment.getExternalStorageDirectory().absolutePath)

            osw.write(metadata.toString())
            osw.flush()
            zos.closeEntry()

            if (!shouldContinue){ throw InterruptedException() }

            var percentage = 1.0

            bcastProgress(percentage, "Writing database entries...")
            val types = listOf("camera", "lens", "filmroll", "accessory", "tag", "tagmap")
            for (type in types) {
                val ze = ZipEntry(type + ".json")
                zos.putNextEntry(ze)
                val entries = dao.getAllEntriesJSON(type)
                osw.write(entries.toString())
                osw.flush()
                zos.closeEntry()
                percentage += 1.0
                bcastProgress(percentage, "Writing database entries...")
                if (!shouldContinue){ throw InterruptedException() }
            }

            val zep = ZipEntry("photo.json")
            zos.putNextEntry(zep)
            val entries = dao.getAllEntriesJSON("photo")
            osw.write(entries.toString())
            osw.flush()
            zos.closeEntry()
            percentage += 1.0
            if (!shouldContinue){ throw InterruptedException() }

            val hs = HashSet<String>()
            val photosPercentage = 100.0 - percentage
            for (i in 0..entries.length() - 1) {
                bcastProgress(percentage + photosPercentage * i.toDouble() / entries.length().toDouble(), "")
                val e = entries.getJSONObject(i)
                val s = e.getString("suppimgs")
                if (s.isEmpty()) continue
                val imgs = JSONArray(s)
                for (j in 0..imgs.length() - 1) {
                    val path = imgs[j].toString()
                    if (hs.contains(path)) continue
                    hs.add(path)
                    val f = File(path)

                    val di = i.toDouble()
                    val dj = j.toDouble()
                    bcastProgress(percentage +
                            photosPercentage * (di + dj / imgs.length().toDouble()) / entries.length().toDouble()
                            , f.name)

                    if (!f.exists()) continue
                    if (!shouldContinue){ throw InterruptedException() }

                    val ze = ZipEntry("imgs" + path)
                    zos.putNextEntry(ze)
                    try {
                        val buf = ByteArray(1024 * 128)
                        val bis = BufferedInputStream(FileInputStream(f))
                        while (true) {
                            val len = bis.read(buf)
                            if (len < 0) break
                            zos.write(buf, 0, len)

                            if (!shouldContinue){ throw InterruptedException() }
                        }
                    } catch (e: IOException) {
                        Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                    } finally {
                        zos.closeEntry()
                    }
                }
            }
            completed = true
        } catch (e: InterruptedException){
            Log.d("EIS", "interrupted")
        } finally {
            dao.close()
            zos.close()
        }

        if(completed) {
            bcastProgress(100.0, "Complete.")
            return true
        }else{
            bcastProgress(100.0, "Canceled.")
            return false
        }
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
                // Notificationのインスタンス化
                var notification = Notification()
                val i = Intent(applicationContext, MainActivity::class.java)
                val pi = PendingIntent.getActivity(this, 0, i,
                        PendingIntent.FLAG_CANCEL_CURRENT)

                val builder = NotificationCompat.Builder(this)

                //Foreground Service
                notification = builder.setContentIntent(pi)
                        .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                        .setAutoCancel(true).setContentTitle("Exporting")
                        .setContentText("Trisquel").build()

                //ステータスバーの通知を消せないようにする
                notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
                startForeground(1, notification) //Foreground Service開始

                val dir = intent.getStringExtra(PARAM_DIR)
                val zipfile = intent.getStringExtra(PARAM_ZIPFILE)
                val sd = File(dir!!)
                val backupZip = File(sd, zipfile!!)
                val backupSuccess = backupToZip(backupZip)
                notifyMediaScanner(backupZip)

                if(!backupSuccess) {
                    handler?.post(Runnable {
                        Toast.makeText(this, "Backup canceled.", Toast.LENGTH_LONG).show()
                    })
                } else {
                    handler?.post(Runnable {
                        Toast.makeText(this, "Wrote to " + backupZip.absolutePath, Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
    }

}