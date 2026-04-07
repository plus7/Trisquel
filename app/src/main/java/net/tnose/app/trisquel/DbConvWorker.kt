package net.tnose.app.trisquel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DbConvWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val repo = TrisquelRepo(ctx as android.app.Application)

    companion object {
        val PARAM_PERCENTAGE = "percentage"
        val PARAM_STATUS = "status"
        fun createDbConvRequest(): WorkRequest {
            return OneTimeWorkRequestBuilder<DbConvWorker>()
                .addTag(Util.WORKER_TAG_DBCONV)
                .build()
        }
    }

    suspend fun bcastProgress(percentage: Double, status: String){
        setProgress(workDataOf(Pair(PARAM_PERCENTAGE, percentage), Pair(PARAM_STATUS, status)))
    }


    fun pathConversion (oldPath: String): String {
        if(oldPath.startsWith("content://")) return oldPath //起きないとは思うけど、一応
        val contentResolver = applicationContext.contentResolver
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
        val dbpath = applicationContext.getDatabasePath("trisquel.db")
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val backupPath = dbpath.absolutePath + "." + LocalDateTime.now().format(formatter) + ".bak"

        val fos = FileOutputStream(backupPath)
        val fis = FileInputStream(dbpath)

        val src = fis.channel
        val dst = fos.channel
        dst.transferFrom(src, 0, src.size())
        src.close()
        dst.close()
    }

    suspend fun doConvert(): Boolean {
        var status = false
        try {
            val photoEntities = repo.getPhotos4Conversion()
            var processedCount = 0.0
            for(entity in photoEntities){
                val photo = Photo.fromEntity(entity)
                for(i in 0..photo.supplementalImages.size-1) {
                    val before = photo.supplementalImages[i]
                    val newpath = pathConversion(photo.supplementalImages[i])
                    photo.supplementalImages[i] = newpath
                    if(newpath.isEmpty()){
                        val f = File(before)
                        val prefix = if(photo.memo.isEmpty()) "" else "\n"
                        if(f.exists()){
                            photo.memo += prefix + "Path conversion failed: %s".format(f.name)
                        }else{
                            photo.memo += prefix + "Not found: %s".format(f.name)
                        }
                    }
                    Log.d("CONV", "before:" + before +" after:" + photo.supplementalImages[i])
                }
                repo.upsertPhoto(photo.toEntity())
                processedCount += 1.0
                bcastProgress( processedCount / (photoEntities.size + 1) * 100, applicationContext.getString(R.string.description_update_dialog))
            }
            status = true
        }finally {
            if (status == true){
                repo.setConversionState(1)
            }
        }
        return status
    }

    override suspend fun doWork(): Result {
        return try {
            bcastProgress(0.0, "Starting DB conversion...")
            //try {
                backupDBBeforeConvert()
            /*} catch (e: Exception) {
                Result.failure(
                    workDataOf(
                        Pair(DbConvWorker.PARAM_PERCENTAGE, 100.0),
                        Pair(DbConvWorker.PARAM_STATUS, "DB conversion interrupted.")))
            }*/

            val convertSuccess = doConvert()

            if (!convertSuccess) {
                Result.failure(
                    workDataOf(
                        Pair(PARAM_PERCENTAGE, 100.0),
                        Pair(PARAM_STATUS, "DB conversion failed!")))
            } else {
                Result.success(
                    workDataOf(
                        Pair(PARAM_PERCENTAGE, 100.0),
                        Pair(PARAM_STATUS, "DB conversion completed.")))
            }
        } catch (e: Exception) {
            Result.failure(
                workDataOf(
                    Pair(PARAM_PERCENTAGE, 100.0),
                    Pair(PARAM_STATUS, "DB conversion failed! $e")))
        }
    }
}
