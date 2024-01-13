package net.tnose.app.trisquel

import android.R
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar

class DbConvIntentService : IntentService {
    companion object {
        @Volatile
        var shouldContinue = true

        val ACTION_START_DBCONV = "net.tnose.app.trisquel.action.START_DBCONV"
        val ACTION_DBCONV_PROGRESS = "net.tnose.app.trisquel.action.DBCONV_PROGRESS"
        val PARAM_PERCENTAGE = "percentage"
        val PARAM_STATUS = "status"
        val PARAM_ERROR = "error"

        internal fun startExport(context: Context){
            val intent = Intent(context, DbConvIntentService::class.java)
            intent.action = ACTION_START_DBCONV
            context.startService(intent)
        }
    }
    var handler: Handler? = null

    constructor() : super("DbConvIntentService") {
        handler = Handler()
    }

    fun bcastProgress(percentage: Double, status: String, isError: Boolean){
        val intent = Intent()
        intent.putExtra(PARAM_PERCENTAGE, percentage)
        intent.putExtra(PARAM_STATUS, status)
        intent.putExtra(PARAM_ERROR, isError)
        intent.action = ACTION_DBCONV_PROGRESS
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(baseContext).sendBroadcast(intent)
    }

    fun pathConversion (oldPath: String): String {
        if(oldPath.startsWith("content://")) return oldPath //起きないとは思うけど、一応
        val contentResolver = contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media.DATA} = ?"
        val selectionArgs = arrayOf(oldPath)

        val cursor: Cursor? = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null)

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                )
                return contentUri.toString()
            }
        }
        return ""
    }

    // DB変換前に現在のDBを念の為アプリ内ローカルの領域にコピーしておく
    fun backupDBBeforeConvert(){
        val dbpath = this.getDatabasePath("trisquel.db")
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss")
        val backupPath = dbpath.absolutePath + "." + sdf.format(calendar.time) + ".bak"

        val fos = FileOutputStream(backupPath)
        val fis = FileInputStream(dbpath)

        val src = fis.channel
        val dst = fos.channel
        dst.transferFrom(src, 0, src.size())
        src.close()
        dst.close()
    }

    fun doConvert(): Boolean {
        val dao = TrisquelDao(this)
        var status = false
        try {
            dao.connection()
            val photoList = dao.getPhotos4Conversion()
            var processedCount = 0.0
            for(p in photoList){
                for(i in 0..p.supplementalImages.size-1) {
                    val before = p.supplementalImages[i]
                    val newpath = pathConversion(p.supplementalImages[i])
                    p.supplementalImages[i] = newpath
                    if(newpath.isEmpty()){
                        val f = File(before)
                        val prefix = if(p.memo.isEmpty()) "" else "\n"
                        if(f.exists()){
                            p.memo += prefix + "Path conversion failed: %s".format(f.name)
                        }else{
                            p.memo += prefix + "Not found: %s".format(f.name)
                        }
                    }
                    Log.d("CONV", "before:" + before +" after:" + p.supplementalImages[i])
                }
                dao.doPhotoConversion(p)
                processedCount += 1.0
                bcastProgress( processedCount / (photoList.size + 1) * 100, getString(net.tnose.app.trisquel.R.string.description_update_dialog), false)
            }
            status = true
        }finally {
            if (status == true){
                dao.setConversionState(1)
            }
            dao.close()
        }
        return status
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        when(intent.action){
            ACTION_START_DBCONV -> {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // グループ生成
                val g = NotificationChannelGroup("trisquel_ch_grp", "trisquel_ch_grp")
                nm.createNotificationChannelGroups(arrayListOf(g))
                val ch = NotificationChannel("trisquel_ch", "trisquel_ch", NotificationManager.IMPORTANCE_DEFAULT)
                ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                nm.createNotificationChannel(ch)
                val channelId = "trisquel_ch"

                bcastProgress(0.0, "Starting DB conversion...", false)
                // Notificationのインスタンス化
                var notification = Notification()
                val i = Intent(applicationContext, MainActivity::class.java)
                val pi = PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val builder = NotificationCompat.Builder(this, channelId)

                //Foreground Service
                notification = builder.setContentIntent(pi)
                        .setGroup("trisquel_ch_grp")
                        .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                        .setAutoCancel(true).setContentTitle("Converting database")
                        .setContentText("Trisquel").build()

                //ステータスバーの通知を消せないようにする
                notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
                startForeground(1, notification) //Foreground Service開始

                try {
                    backupDBBeforeConvert()
                } catch (e: Exception){
                    handler?.post(Runnable {
                        Toast.makeText(this, "Internal backup before DB conversion failed!", Toast.LENGTH_LONG).show()
                    })
                    bcastProgress(100.0, "DB conversion interrupted.", true)
                    return
                }

                val convertSuccess = doConvert()

                if(!convertSuccess) {
                    handler?.post(Runnable {
                        Toast.makeText(this, "Conversion failed!", Toast.LENGTH_LONG).show()
                    })
                    bcastProgress(100.0, "DB conversion failed!", true)
                } else {
                    handler?.post(Runnable {
                        val i2 = Intent(applicationContext, MainActivity::class.java)
                        val pi = PendingIntent.getActivity(this, 0, i2, PendingIntent.FLAG_IMMUTABLE)

                        val builder = NotificationCompat.Builder(this, channelId)

                        //Foreground Service
                        val n = builder.setContentIntent(pi)
                                .setGroup("trisquel_ch_grp")
                                .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                                .setAutoCancel(true).setContentTitle("Conversion finished")
                                .setContentText("Trisquel").build()

                        nm.notify(1, n)
                    })
                    bcastProgress(100.0, "DB conversion completed.", false)
                }
            }
        }
    }
}