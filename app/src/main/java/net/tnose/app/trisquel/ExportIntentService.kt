package net.tnose.app.trisquel

import android.R
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL
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
        val PARAM_MODE = "mode"

        internal fun startExport(context: Context, dir: String, zipfile: String, mode: Int){
            val intent = Intent(context, ExportIntentService::class.java)
            intent.action = ACTION_START_EXPORT
            intent.putExtra(PARAM_DIR, dir)
            intent.putExtra(PARAM_ZIPFILE, zipfile)
            intent.putExtra(PARAM_MODE, mode)
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
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(baseContext).sendBroadcast(intent)
    }

    fun doMetadataWrite(mode: Int, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao){
        // Metadata
        val ze = ZipEntry("metadata.json")
        zos.putNextEntry(ze)
        val metadata = JSONObject()
        metadata.put("DB_VERSION", DatabaseHelper.DATABASE_VERSION)
        metadata.put("EXPORT_MODE", mode) //0: DB Only, 1: DB+IMAGES

        osw.write(metadata.toString())
        osw.flush()
        zos.closeEntry()
    }

    fun doDatabaseWrite(percentage: Double, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao): Double{
        val types = listOf("camera", "lens", "filmroll", "accessory", "tag", "tagmap")
        var mypercentage = percentage
        for (type in types) {
            val ze = ZipEntry(type + ".json")
            zos.putNextEntry(ze)
            val entries = dao.getAllEntriesJSON(type)
            osw.write(entries.toString())
            osw.flush()
            zos.closeEntry()
            mypercentage += 1.0
            bcastProgress(mypercentage, "Writing database entries...")
            if (!shouldContinue){ throw InterruptedException() }
        }
        return mypercentage
    }

    fun doPhotoInfoWrite(mode: Int, percentage: Double, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao, entries: JSONArray): Double {
        val zep = ZipEntry("photo.json")
        zos.putNextEntry(zep)
        val checkSumPercentage = if(mode == 1) (99.99 - percentage)/4 else 99.99 - percentage
        for (i in 0..entries.length() - 1) {
            val e = entries.getJSONObject(i)
            val s = e.getString("suppimgs")
            if (s.isEmpty()) continue
            val imgs = JSONArray(s)
            val suppimgs_date_taken = JSONArray()
            val suppimgs_file_name = JSONArray()
            val suppimgs_md5sum = JSONArray()
            for(j in 0 until imgs.length()){
                val di = i.toDouble()
                val dj = j.toDouble()
                bcastProgress(percentage +
                        checkSumPercentage * (di + dj / imgs.length().toDouble()) / entries.length().toDouble()
                        , "Writing checksum for supplemental images...")
                val path = imgs.getString(j)
                if(path.isEmpty()){
                    suppimgs_date_taken.put(j, "")
                    suppimgs_file_name.put(j, "")
                    suppimgs_md5sum.put(j, "")
                    continue
                }
                var datetaken = ""
                var md5str = ""
                try {
                    val ist1 = CompatibilityUtil.pathToInputStream(contentResolver, path, false)
                    val exifInterface = if(ist1 != null) ExifInterface(ist1) else null
                    datetaken = exifInterface?.getAttribute(TAG_DATETIME_ORIGINAL) ?: ""

                    val ist2 = CompatibilityUtil.pathToInputStream(contentResolver, path, true)
                    md5str = if(ist2 != null) MD5Util.digestAsStr(ist2) else ""
                } catch (e: Exception){}
                val displayname = CompatibilityUtil.pathToDisplayName(contentResolver, path)
                suppimgs_date_taken.put(j, datetaken)
                suppimgs_file_name.put(j, displayname)
                suppimgs_md5sum.put(j, md5str)
            }
            e.put("suppimgs_date_taken", suppimgs_date_taken)
            e.put("suppimgs_file_name", suppimgs_file_name)
            e.put("suppimgs_md5sum", suppimgs_md5sum)
            //bcastProgress(percentage, "Writing checksum for supplemental images...")
            if (!shouldContinue){ throw InterruptedException() }
        }

        osw.write(entries.toString())
        osw.flush()
        zos.closeEntry()
        if (!shouldContinue){ throw InterruptedException() }
        return percentage + checkSumPercentage
    }

    fun doPhotoContentWrite(percentage: Double, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao, entries: JSONArray): Double {
        val hs = HashSet<String>()
        val photosPercentage = 99.99 - percentage
        for (i in 0..entries.length() - 1) {
            bcastProgress(percentage + photosPercentage * i.toDouble() / entries.length().toDouble(), "")
            val e = entries.getJSONObject(i)
            val si = e.getString("suppimgs")
            if (si.isEmpty()) continue
            val imgs = JSONArray(si)
            val sf = e.getString("suppimgs_file_name")
            val fileNames = JSONArray(sf)
            val sc = e.getString("suppimgs_md5sum")
            val checkSums = JSONArray(sc)
            for (j in 0 until imgs.length()) {
                val path = imgs[j].toString()
                val fileName = fileNames[j].toString()
                val checkSum = checkSums[j].toString()
                if (hs.contains(checkSum)) continue
                hs.add(checkSum)

                val ist = CompatibilityUtil.pathToInputStream(contentResolver, path, true)
                //val f = File(path)

                val di = i.toDouble()
                val dj = j.toDouble()
                bcastProgress(percentage +
                        photosPercentage * (di + dj / imgs.length().toDouble()) / entries.length().toDouble()
                        , fileName)

                if (ist == null) continue
                if (!shouldContinue){ throw InterruptedException() }

                val ze = ZipEntry("imgs/" + checkSum)
                zos.putNextEntry(ze)
                try {
                    val buf = ByteArray(1024 * 128)
                    val bis = BufferedInputStream(ist)
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
        return percentage + photosPercentage
    }

    fun backupToZip(zipFile: File, mode: Int): Boolean {
        val zos = ZipOutputStream(FileOutputStream(zipFile))
        zos.setMethod(ZipOutputStream.DEFLATED)
        val osw = OutputStreamWriter(zos, "UTF-8")
        val dao = TrisquelDao(this)
        dao.connection()

        var completed = false

        try {
            bcastProgress(0.0, "Writing metadata...")

            doMetadataWrite(mode, zos, osw, dao)
            if (!shouldContinue){ throw InterruptedException() }

            var percentage = 1.0
            bcastProgress(percentage, "Writing database entries...")

            percentage = doDatabaseWrite(percentage, zos, osw, dao)

            val entries = dao.getAllEntriesJSON("photo")
            percentage = doPhotoInfoWrite(mode, percentage, zos, osw, dao, entries)
            if(mode == 1) {
                percentage = doPhotoContentWrite(percentage, zos, osw, dao, entries)
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
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // グループ生成
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val g = NotificationChannelGroup("trisquel_ch_grp", "trisquel_ch_grp")
                    nm.createNotificationChannelGroups(arrayListOf(g))
                    val ch = NotificationChannel("trisquel_ch", "trisquel_ch", NotificationManager.IMPORTANCE_DEFAULT)
                    ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    nm.createNotificationChannel(ch)
                }
                val channelId =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            "trisquel_ch"
                        } else {
                            ""
                        }
                // Notificationのインスタンス化
                var notification = Notification()
                val i = Intent(applicationContext, MainActivity::class.java)
                val pi = PendingIntent.getActivity(this, 0, i,
                        PendingIntent.FLAG_CANCEL_CURRENT)

                val builder = NotificationCompat.Builder(this, channelId)

                //Foreground Service
                notification = builder.setContentIntent(pi)
                        .setGroup("trisquel_ch_grp")
                        .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                        .setAutoCancel(true).setContentTitle("Exporting")
                        .setContentText("Trisquel").build()

                //ステータスバーの通知を消せないようにする
                notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
                startForeground(1, notification) //Foreground Service開始

                val dir = intent.getStringExtra(PARAM_DIR)
                val zipfile = intent.getStringExtra(PARAM_ZIPFILE)
                val mode = intent.getIntExtra(PARAM_MODE, 0)
                val sd = File(dir!!)
                val backupZip = File(sd, zipfile!!)
                val backupSuccess = backupToZip(backupZip, mode)
                notifyMediaScanner(backupZip)

                if(!backupSuccess) {
                    handler?.post(Runnable {
                        Toast.makeText(this, "Backup canceled.", Toast.LENGTH_LONG).show()
                    })
                } else {
                    handler?.post(Runnable {
                        val i2 = Intent(applicationContext, MainActivity::class.java)
                        val pi = PendingIntent.getActivity(this, 0, i2, 0)

                        val builder = NotificationCompat.Builder(this, channelId)

                        //Foreground Service
                        val n = builder.setContentIntent(pi)
                                .setGroup("trisquel_ch_grp")
                                .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                                .setAutoCancel(true).setContentTitle("Wrote to " + backupZip.absolutePath)
                                .setContentText("Trisquel").build()

                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(1, n)
                        Toast.makeText(this, "Wrote to " + backupZip.absolutePath, Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
    }

}