package net.tnose.app.trisquel

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.apache.commons.compress.archivers.zip.ZipFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

class VersionUnmatchException : Exception()

sealed class WrappedZipFile {
    data class JavaZipFile(val zf: java.util.zip.ZipFile): WrappedZipFile()
    data class ApacheCCZipFile(val zf: ZipFile): WrappedZipFile()
}

class CustomImportException : Exception ("Internal backup before import failed!")
class ImportWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    companion object{
        val PARAM_ZIPFILE = "zipfile"
        val PARAM_URI = "uri"
        val PARAM_MODE = "mode"
        val PARAM_PERCENTAGE = "percentage"
        val PARAM_STATUS = "status"
        fun createImportRequest(zipFile : String, mode : Int): WorkRequest {
            return OneTimeWorkRequestBuilder<ImportWorker>()
                .setInputData(
                    workDataOf(
                        Pair(PARAM_ZIPFILE, zipFile),
                        Pair(PARAM_MODE, mode)
                    )
                )
                .addTag(Util.WORKER_TAG_IMPORT)
                .build()
        }

        const val SUCCESS = 0
        const val VERSION_UNMATCH = -1
        const val CANCELED = -2
        const val UNKNOWN = -999
    }

    val cameraIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val lensIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val accessoryIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val filmrollIdOld2NewMap: HashMap<Int, Int> = HashMap()
    val photoIdOld2NewMap: HashMap<Int, Int> = HashMap()
    override suspend fun doWork(): Result {
        val uri = inputData.getString(PARAM_ZIPFILE)
        val mode = inputData.getInt(PARAM_MODE, 0)
        return try {
            backupDBBeforeImport()
            val result = importFromZip(Uri.parse(uri), (mode == 0))
            if(result != SUCCESS){
                val errstr = when(result){
                    VERSION_UNMATCH -> "Database version is too new"
                    CANCELED -> "User canceled import operation"
                    UNKNOWN -> "Unknown error %d".format(result)
                    else -> "Impossible error %d".format(result)
                }
                Result.failure(
                    workDataOf(
                        Pair(PARAM_PERCENTAGE, 100.0),
                        Pair(PARAM_STATUS, errstr)))
            }else {
                Result.success(
                    workDataOf(
                        Pair(PARAM_PERCENTAGE, 100.0),
                        Pair(PARAM_STATUS, "Import from $uri completed")))
            }
        } catch (error: CancellationException) {
            Result.failure(
                workDataOf(
                    Pair(ExportWorker.PARAM_PERCENTAGE, 100.0),
                    Pair(ExportWorker.PARAM_STATUS, "Backup canceled.")))
        } catch (e: Exception){
            Result.failure(
                workDataOf(
                    Pair(PARAM_PERCENTAGE, 100.0),
                    Pair(PARAM_STATUS, e.toString())))
        }
    }

    private suspend fun bcastProgress(percentage: Double, status: String){
        setProgress(workDataOf(Pair(PARAM_PERCENTAGE, percentage), Pair(PARAM_STATUS, status)))
    }

    fun getStringFromEntry(wzf: WrappedZipFile, entryName: String): String{
        val stream: InputStream = when(wzf) {
            is WrappedZipFile.JavaZipFile -> wzf.zf.getInputStream(wzf.zf.getEntry(entryName))
            is WrappedZipFile.ApacheCCZipFile -> wzf.zf.getInputStream(wzf.zf.getEntry(entryName))
        }

        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos)
        val data = ByteArray(8*1024)
        while (true) {
            val count = stream.read(data, 0, 8*1024)
            if (count < 0) break
            bos.write(data, 0, count)
        }
        bos.flush()
        val result = baos.toString()
        bos.close()
        stream.close()
        return result
    }

    private fun getJSONObjectFromEntry(wzf: WrappedZipFile, entryName: String): JSONObject {
        val str = getStringFromEntry(wzf, entryName)
        return JSONObject(str)
    }

    private fun getJSONArrayFromEntry(wzf: WrappedZipFile, entryName: String): JSONArray {
        val str = getStringFromEntry(wzf, entryName)
        return JSONArray(str)
    }

    fun appendSuffix(fileName: String, suffixNumber: Int):String{
        val lastDot = fileName.lastIndexOf('.')
        if(lastDot < 0){
            return fileName + "(%d)".format(suffixNumber)
        }else{
            return fileName.replaceRange(lastDot, lastDot, "(%d)".format(suffixNumber))
        }
    }

    private fun getSafeFileNameP(relPath: String, displayName: String): String {
        var currentCandidate = displayName
        var suffixNumber = 1
        while(true) {
            val filePath = Environment.getExternalStorageDirectory().absolutePath + "/" + relPath + "/" + currentCandidate
            val f = File(filePath)
            if(!f.exists()) break
            currentCandidate = appendSuffix(displayName, suffixNumber)
            suffixNumber++
        }

        return currentCandidate
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getSafeFileNameQ(relPath: String, displayName: String): String{
        var currentCandidate = displayName
        var suffixNumber = 1
        while(true) {
            /*val values = ContentValues().apply {
                put(MediaStore.Images.Media.RELATIVE_PATH, relPath)
                put(MediaStore.Images.Media.DISPLAY_NAME, currentCandidate)
            }*/
            val projection = arrayOf(
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DISPLAY_NAME)
            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(relPath, currentCandidate)

            val cursor: Cursor? = applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null)

            if(cursor == null){
                break
            }else{
                if(!cursor.moveToNext()) break
                currentCandidate = appendSuffix(displayName, suffixNumber)
                cursor.close()
            }
            suffixNumber++
        }

        return currentCandidate
    }

    private fun writeInMediaStoreP(relPath: String, displayName: String, ist: InputStream): String{
        val dirPath = Environment.getExternalStorageDirectory().absolutePath + "/" + relPath
        val dir = File(dirPath)
        if(!dir.exists()){
            dir.mkdirs()
        }
        val filePath = dirPath + "/" + displayName
        val ost = FileOutputStream(filePath)
        val bost = BufferedOutputStream(ost)
        val data = ByteArray(8*1024)
        while (true) {
            val count = ist.read(data, 0, 8*1024)
            if (count < 0) break
            bost.write(data, 0, count)
        }
        bost.flush()
        bost.close()

        val values= ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.DATA, filePath)
        }
        val item = applicationContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        return item.toString()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun writeInMediaStoreQ(relPath: String, displayName: String, ist: InputStream): String{
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, relPath)
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media
            .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = applicationContext.contentResolver.insert(collection, values)!!
        val pfd = applicationContext.contentResolver.openFileDescriptor(item, "w", null)
        if(pfd != null){
            val ost = FileOutputStream(pfd.fileDescriptor)
            val bost = BufferedOutputStream(ost)
            val data = ByteArray(8*1024)
            while (true) {
                val count = ist.read(data, 0, 8*1024)
                if (count < 0) break
                bost.write(data, 0, count)
            }
            bost.flush()
            bost.close()
            pfd.close()
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        applicationContext.contentResolver.update(item, values, null, null)
        return item.toString()
    }

    private fun newPathsFullRestore(wzf: WrappedZipFile, photo: JSONObject, dao: TrisquelDao, filmrollIdOld2NewMap: HashMap<Int, Int>): Pair<ArrayList<String>, String>{
        val result = ArrayList<String>()
        //val importErrors = ArrayList<String>()

        if(photo.getString("suppimgs").isEmpty()) return Pair(result, "")

        val filmRollOldId = photo.getInt("filmroll")
        val filmRollNewId = filmrollIdOld2NewMap[filmRollOldId]!! // TODO: 手抜きなのであとでなおす
        val folderName = dao.getFilmRoll(filmRollNewId)!!.name.replace('/', '_')

        val fileNames = photo.getJSONArray("suppimgs_file_name")
        val md5sums = photo.getJSONArray("suppimgs_md5sum")

        for(i in 0 until fileNames.length()){
            val fileName = fileNames.getString(i)
            if(fileName.isEmpty()) continue // こういうケースが本当にあってよいものか… 要確認な気がする
            val md5sum = md5sums.getString(i)
            val relPath = "Pictures/Trisquel/" + folderName
            val safeFileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                getSafeFileNameQ(relPath, fileName)
            }else{
                getSafeFileNameP(relPath, fileName)
            }

            /*val ze = zf.getEntry("imgs/" + md5sum)
            if(ze == null){
                Log.d("ZipFile", "zip entry for imgs/%s is null".format(md5sum))
            }
            val istream = zf.getInputStream(ze)
            if(istream == null){
                Log.d("ZipFile", "istream is null")
            }*/
            val entryName = "imgs/" + md5sum
            val istream: InputStream = when(wzf) {
                is WrappedZipFile.JavaZipFile -> wzf.zf.getInputStream(wzf.zf.getEntry(entryName))
                is WrappedZipFile.ApacheCCZipFile -> wzf.zf.getInputStream(wzf.zf.getEntry(entryName))
            }

            val newpath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                writeInMediaStoreQ(relPath, safeFileName, istream)
            }else{
                writeInMediaStoreP(relPath, safeFileName, istream)
            }

            result.add(newpath)
            istream.close()
        }
        return Pair(result, "")
    }

    private fun isContentUriExists(uri: Uri): Boolean{
        var result = true
        try{
            val pfd = applicationContext.contentResolver.openFileDescriptor(uri, "r")
            if(pfd==null){ result = false }
            pfd?.close()
        }catch(e: FileNotFoundException){
            result = false
        }
        return result
    }

    private fun isMd5sumMatch(uri: Uri, md5sum: String): Boolean{
        val ist = applicationContext.contentResolver.openInputStream(uri)
        if(ist != null)
            return MD5Util.digestAsStr(ist) == md5sum
        else
            return false
    }

    private fun getRealPathFromURI(contentUri: Uri): String {
        var cursor: Cursor? = null
        val result: String
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = applicationContext.contentResolver.query(contentUri, proj, null, null, null)
            cursor!!.moveToFirst()
            val column_index = cursor.getColumnIndex(proj[0])
            result = cursor.getString(column_index)
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun queryCandidateUris(fileName: String): ArrayList<String> {
        val cr = applicationContext.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val cursor: Cursor? = cr.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null)
        val paths = ArrayList<String>()
        while (cursor?.moveToNext() == true) {
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            //val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString()
            )
            paths.add(contentUri.toString())
        }
        cursor?.close()
        return paths
    }

    private fun assumedPaths(photo: JSONObject): Pair<ArrayList<String>, String> {
        val result = ArrayList<String>()
        val importErrors = ArrayList<String>()

        //Log.d("AssumePath", "suppimgs=" + photo.getString("suppimgs"))
        val suppimgs: String = photo.getString("suppimgs")
        val paths = if(suppimgs.isEmpty()) JSONArray() else JSONArray(suppimgs)
        val fileNames = if(photo.isNull("suppimgs_file_name")) JSONArray() else photo.getJSONArray("suppimgs_file_name")
        //val dateTakens = photo.getJSONArray("suppimgs_date_taken")
        val md5sums = if(photo.isNull("suppimgs_md5sum")) JSONArray() else photo.getJSONArray("suppimgs_md5sum")
        for (i in 0 until paths.length()) {
            val path = paths.getString(i)
            if(path.isEmpty()) continue
            val fileName = fileNames.getString(i)
            val md5sum = md5sums.getString(i)

            // if(pathが存在してファイル名もmd5sumも一致) -> pathを採用(同じスマートフォンからのインポートを想定)
            val contentUri = Uri.parse(path)
            if(isContentUriExists(contentUri) && isMd5sumMatch(contentUri, md5sum)){
                result.add(path)
            }else{
                val candidates = queryCandidateUris(fileName)
                when(candidates.size){
                    //fileNameが存在して1個しかない -> これを採用(普通はこれになるはず)
                    //1 -> result.add(candidates.first()) //やめた。ファイルがないのにcontent://のURIがみつかることがある。中身もチェックしないとだめ
                    //fileNameが存在しない -> インポートエラーに追加
                    0 -> importErrors.add("Not found: " + fileName)
                    //md5sumが一致するものを全部追加(取りこぼすよりはええじゃろ)
                    else -> {
                        var foundSome = false
                        for(c in candidates){
                            Log.d("ImportDebug",
                                "fileName:" + fileName
                                        + " Candidate URI:" + c
                                        + " Path:" +getRealPathFromURI(Uri.parse(c)))
                            val ist = CompatibilityUtil.pathToInputStream(applicationContext.contentResolver, c, true)
                            if(ist!=null) {
                                val md5sumToCompare = MD5Util.digestAsStr(ist)
                                if(md5sum == md5sumToCompare){
                                    result.add(c)
                                    foundSome = true
                                }
                            }
                        }
                        if(!foundSome){
                            importErrors.add("Not found: " + fileName)
                        }
                    }
                }
            }
        }
        return Pair(result, importErrors.joinToString("\n"))
    }
    // インポート前に現在のDBを念の為アプリ内ローカルの領域にコピーしておく
    @SuppressLint("SimpleDateFormat")
    private fun backupDBBeforeImport(){
        val dbpath = applicationContext.getDatabasePath("trisquel.db")
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

    private fun CloseWrappedZipFile(wzf: WrappedZipFile){
        when(wzf) {
            is WrappedZipFile.JavaZipFile -> wzf.zf.close()
            is WrappedZipFile.ApacheCCZipFile -> wzf.zf.close()
        }
    }

    private suspend fun importFromZip(uri: Uri, merge: Boolean): Int{
        var result = UNKNOWN
        var pfd: ParcelFileDescriptor? = null
        //どうもここでval pfdとするとファイルディスクリプタがGCされてしまう？？？

        pfd = applicationContext.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) throw FileNotFoundException(uri.toString())
        val fis = FileInputStream(pfd.fileDescriptor)
        val zipfile: WrappedZipFile = WrappedZipFile.ApacheCCZipFile(ZipFile(fis.channel))

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        //try {
        try {
            val metadataJSON = getJSONObjectFromEntry(zipfile, "metadata.json")
            val dbver = metadataJSON.getInt("DB_VERSION")
            val mode = metadataJSON.getInt("EXPORT_MODE")
            Log.d("importFromZip",
                "DB_VERSION=" + dbver.toString() + ", " +
                        "EXPORT_MODE=" + mode.toString())
            if (dbver > DatabaseHelper.DATABASE_VERSION) {
                throw VersionUnmatchException()
            }

            dao.beginTransaction()

            if (!merge) dao.deleteAll()

            val cameraJSON = getJSONArrayFromEntry(zipfile, "camera.json")
            for (i in 0 until cameraJSON.length()) {
                val id_pair = dao.mergeCameraJSON(cameraJSON.getJSONObject(i))
                cameraIdOld2NewMap[id_pair.first] = id_pair.second
            }

            val lensJSON = getJSONArrayFromEntry(zipfile, "lens.json")
            for (i in 0 until lensJSON.length()) {
                val id_pair = dao.mergeLensJSON(lensJSON.getJSONObject(i), cameraIdOld2NewMap)
                lensIdOld2NewMap[id_pair.first] = id_pair.second
            }

            val accessoryJSON = getJSONArrayFromEntry(zipfile, "accessory.json")
            for (i in 0 until accessoryJSON.length()) {
                val id_pair = dao.mergeAccessoryJSON(accessoryJSON.getJSONObject(i))
                accessoryIdOld2NewMap[id_pair.first] = id_pair.second
            }

            val filmrollJSON = getJSONArrayFromEntry(zipfile, "filmroll.json")
            for (i in 0 until filmrollJSON.length()) {
                val id_pair = dao.mergeFilmRollJSON(filmrollJSON.getJSONObject(i), cameraIdOld2NewMap)
                filmrollIdOld2NewMap[id_pair.first] = id_pair.second
            }

            val photoJSON = getJSONArrayFromEntry(zipfile, "photo.json")
            for (i in 0 until photoJSON.length()) {
                val pjo = photoJSON.getJSONObject(i)
                val tmp: Pair<ArrayList<String>, String> =
                    if (mode == 0) assumedPaths(pjo)
                    else newPathsFullRestore(zipfile, pjo, dao, filmrollIdOld2NewMap)
                val newpaths = tmp.first
                val importErrorStr = tmp.second
                // val (newpaths, importErrorStr) = tmp なぜかこれができない
                val id_pair = dao.mergePhotoJSON(pjo, newpaths, importErrorStr,
                    cameraIdOld2NewMap, lensIdOld2NewMap, filmrollIdOld2NewMap, accessoryIdOld2NewMap)
                photoIdOld2NewMap[id_pair.first] = id_pair.second

                val msg = if(mode == 0) "Searching photo files..." else "Copying photo files..."
                bcastProgress(i.toDouble() / photoJSON.length().toDouble() * 99.99, msg)
            }

            val tagJSON = getJSONArrayFromEntry(zipfile, "tag.json")
            val tagmapJSON = getJSONArrayFromEntry(zipfile, "tagmap.json")
            dao.mergeTagMapJSON(tagmapJSON, tagJSON, filmrollIdOld2NewMap, photoIdOld2NewMap)

            //} catch( e: Exception ) {
            //    Log.d("ZipFile", e.toString())
            dao.setTransactionSuccessful()
            result = SUCCESS
        } catch (e: VersionUnmatchException) {
            result = VERSION_UNMATCH
        } finally {
            CloseWrappedZipFile(zipfile)
            dao.endTransaction()
            dao.close()
            //zipfile.close()
            //tmpFile.delete()
        }
        //} catch( e: Exception ) {
        //Log.d("ZipFile", e.toString())
        //} finally {
        //}
        pfd.close() //念のため最後にpfdを触るコードを入れてGCさせない（いいのかこれで？） ついでにクローズもしてみる


        return result
    }
}