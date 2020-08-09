package net.tnose.app.trisquel

import android.R
import android.app.IntentService
import android.app.Notification
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

class DbConvIntentService : IntentService {
    companion object {
        @Volatile
        var shouldContinue = true

        val ACTION_START_DBCONV = "net.tnose.app.trisquel.action.START_DBCONV"
        val ACTION_DBCONV_PROGRESS = "net.tnose.app.trisquel.action.DBCONV_PROGRESS"
        val PARAM_PERCENTAGE = "percentage"
        val PARAM_STATUS = "status"

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

    fun bcastProgress(percentage: Double, status: String){
        val intent = Intent()
        intent.putExtra(PARAM_PERCENTAGE, percentage)
        intent.putExtra(PARAM_STATUS, status)
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

    fun doConvert(): Boolean {
        val dao = TrisquelDao(this)
        try {
            dao.connection()
            val photoList = dao.getPhotos4Conversion()
            var processedCount = 0.0
            for(p in photoList){
                for(i in 0..p.supplementalImages.size-1) {
                    val before = p.supplementalImages[i]
                    p.supplementalImages[i] = pathConversion(p.supplementalImages[i])
                    Log.d("CONV", "before:" + before +" after:" + p.supplementalImages[i])
                }
                dao.doPhotoConversion(p)
                processedCount += 1.0
                bcastProgress( processedCount / (photoList.size + 1) * 100, "updating...")
            }
        }finally {
            dao.setConversionState(1)
            dao.close()
        }
        return true
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        when(intent.action){
            DbConvIntentService.ACTION_START_DBCONV -> {
                bcastProgress(0.0, "Starting DB conversion...")
                // Notificationのインスタンス化
                var notification = Notification()
                val i = Intent(applicationContext, MainActivity::class.java)
                val pi = PendingIntent.getActivity(this, 0, i,
                        PendingIntent.FLAG_CANCEL_CURRENT)

                val builder = NotificationCompat.Builder(this)

                //Foreground Service
                notification = builder.setContentIntent(pi)
                        .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                        .setAutoCancel(true).setContentTitle("Converting database")
                        .setContentText("Trisquel").build()

                //ステータスバーの通知を消せないようにする
                notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
                startForeground(1, notification) //Foreground Service開始

                val convertSuccess = doConvert()

                if(!convertSuccess) {
                    handler?.post(Runnable {
                        Toast.makeText(this, "Conversion failed!", Toast.LENGTH_LONG).show()
                    })
                } else {
                    handler?.post(Runnable {
                        val i2 = Intent(applicationContext, MainActivity::class.java)
                        val pi = PendingIntent.getActivity(this, 0, i2, 0)

                        val builder = NotificationCompat.Builder(this)

                        //Foreground Service
                        val n = builder.setContentIntent(pi)
                                .setSmallIcon(R.mipmap.sym_def_app_icon).setTicker("")
                                .setAutoCancel(true).setContentTitle("Conversion finished")
                                .setContentText("Trisquel").build()

                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(1, n)
                    })
                }
                bcastProgress(100.0, "DB conversion completed.")
            }
        }
    }
}