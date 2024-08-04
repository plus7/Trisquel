package net.tnose.app.trisquel

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException

class ExportWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object{
        @Volatile
        var shouldContinue = true
        const val PARAM_ZIPFILE = "zipfile"
        const val PARAM_PERCENTAGE = "percentage"
        const val PARAM_STATUS = "status"
        const val PARAM_ERROR = "error"
        const val PARAM_MODE = "mode"
        const val notificationId = 1
        const val channelId = "trisquel_ch"
        fun createExportRequest(zipFile : String, mode : Int): WorkRequest {
            return OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(
                    workDataOf(
                        Pair(PARAM_ZIPFILE, zipFile),
                        Pair(PARAM_MODE, mode)
                    )
                )
                .addTag(Util.WORKER_TAG_EXPORT)
                .build()
        }
    }
    override suspend fun doWork(): Result {
        var success = false
        setForeground(createForegroundInfo())
        val retval = try {
            val appContext = applicationContext
            val zipFile = inputData.getString(PARAM_ZIPFILE)
            val uri = Uri.parse(zipFile!!)
            val mode = inputData.getInt(PARAM_MODE, 0)
            //makeStatusNotification("Blurring image", appContext)
            var backupSuccess = false
            appContext.contentResolver.openOutputStream(uri).use { outputStream ->
                    if(outputStream != null) {
                        backupSuccess = backupToZip(appContext, outputStream, mode)
                    }
                }
            if(backupSuccess){
                //Toast.makeText(appContext, "Backup completed.", Toast.LENGTH_LONG).show()
                success = true
                Result.success(
                    workDataOf(
                        Pair(PARAM_PERCENTAGE, 100.0),
                        Pair(PARAM_STATUS, "Backup completed.")))
            }else{
                //Toast.makeText(appContext, "Backup canceled.", Toast.LENGTH_LONG).show()
                Result.failure(
                    workDataOf(
                        Pair(PARAM_PERCENTAGE, 100.0),
                        Pair(PARAM_STATUS, "Backup failed.")))
            }
        } catch (error: CancellationException) {
            //Toast.makeText(applicationContext, "Backup canceled.", Toast.LENGTH_LONG).show()
            Result.failure(
                workDataOf(
                    Pair(PARAM_PERCENTAGE, 100.0),
                    Pair(PARAM_STATUS, "Backup canceled.")))
        } catch (error: IOException) {
            // 本当はToastで済ませるのではなく通知を出すべき
            //Toast.makeText(applicationContext, "IO error.", Toast.LENGTH_LONG).show()
            Result.failure(
                workDataOf(
                    Pair(PARAM_PERCENTAGE, 100.0),
                    Pair(PARAM_STATUS, "IO error: "+error.toString())))
        } catch (throwable: Throwable) {
            Log.e("Export", throwable.localizedMessage!!)
            Result.failure(
                workDataOf(
                    Pair(PARAM_PERCENTAGE, 100.0),
                    Pair(PARAM_STATUS, throwable.localizedMessage!!)))
        }
        notifyCompletion(success)
        return retval
    }
    // https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running?hl=ja
    private fun createForegroundInfo(): ForegroundInfo {

        val title = "Exporting"
        val contentText = "Trisquel"
        val cancel = "Cancel"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        createChannel()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.sym_def_app_icon)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(R.drawable.ic_delete, cancel, intent)
            .build()

        //ステータスバーの通知を消せないようにする
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            return ForegroundInfo(
                notificationId,
                notification
            )
        }
    }

    private fun createChannel() {
        val g = NotificationChannelGroup("trisquel_ch_grp", "trisquel_ch_grp")
        nm.createNotificationChannelGroups(arrayListOf(g))
        val ch = NotificationChannel("trisquel_ch", "trisquel_ch", NotificationManager.IMPORTANCE_DEFAULT)
        ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(ch)
    }

    private fun notifyCompletion(success: Boolean){
        val i = Intent(applicationContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(applicationContext, 0, i, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, channelId)

        val msg = if (success) "Backup completed." else "Backup failed."
        //Foreground Service
        val n = builder.setContentIntent(pi)
            .setGroup("trisquel_ch_grp")
            .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
            .setAutoCancel(true).setContentTitle(msg)
            .setContentText("Trisquel").build()

        //val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, n)
    }

    suspend fun bcastProgress(percentage: Double, status: String){
        setProgress(workDataOf(Pair(PARAM_PERCENTAGE, percentage), Pair(PARAM_STATUS, status)))
    }

    suspend fun doMetadataWrite(mode: Int, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao){
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

    suspend fun doDatabaseWrite(percentage: Double, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao): Double{
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

    suspend fun doPhotoInfoWrite(appContext: Context, mode: Int, percentage: Double, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao, entries: JSONArray): Double {
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
                    val ist1 = CompatibilityUtil.pathToInputStream(appContext.contentResolver, path, false)
                    val exifInterface = if(ist1 != null) ExifInterface(ist1) else null
                    datetaken = exifInterface?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: ""

                    val ist2 = CompatibilityUtil.pathToInputStream(appContext.contentResolver, path, true)
                    md5str = if(ist2 != null) MD5Util.digestAsStr(ist2) else ""
                } catch (e: Exception){}
                val displayname = CompatibilityUtil.pathToDisplayName(appContext.contentResolver, path)
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

    suspend fun doPhotoContentWrite(appContext: Context, percentage: Double, zos: ZipOutputStream, osw: OutputStreamWriter, dao: TrisquelDao, entries: JSONArray): Double {
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

                val ist = CompatibilityUtil.pathToInputStream(appContext.contentResolver, path, true)
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
                } /*catch (e: IOException) {
                    //Toast.makeText(appContext, e.localizedMessage, Toast.LENGTH_LONG).show()
                }*/ finally {
                    zos.closeEntry()
                }
            }
        }
        return percentage + photosPercentage
    }
    suspend fun backupToZip(appContext: Context, zipFile: OutputStream, mode: Int): Boolean {
        val zos = ZipOutputStream(zipFile)
        zos.setMethod(ZipOutputStream.DEFLATED)
        val osw = OutputStreamWriter(zos, "UTF-8")
        val dao = TrisquelDao(appContext)
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
            percentage = doPhotoInfoWrite(appContext, mode, percentage, zos, osw, dao, entries)
            if(mode == 1) {
                percentage = doPhotoContentWrite(appContext, percentage, zos, osw, dao, entries)
            }
            completed = true
        } catch (e: InterruptedException){
            Log.d("EIS", "interrupted")
        } finally {
            dao.close()
            zos.close()
        }

        if(completed) {
            //bcastProgress(100.0, "Complete.")
            return true
        }else{
            //bcastProgress(100.0, "Canceled.")
            return false
        }
    }
}