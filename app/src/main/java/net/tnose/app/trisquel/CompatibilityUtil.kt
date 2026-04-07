package net.tnose.app.trisquel

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import androidx.core.net.toUri

class CompatibilityUtil {
    companion object {
        internal fun pathToInputStream(contentResolver: ContentResolver, path: String, reqOrig: Boolean): InputStream? {
            if(path.startsWith("/")){
                return File(path).inputStream()
            } else {
                val photoUri =
                        if (reqOrig) {
                            MediaStore.setRequireOriginal(path.toUri())
                        }else {
                            path.toUri()
                        }
                val ist: InputStream?
                try{
                    ist = contentResolver.openInputStream(photoUri)
                    return ist
                }catch (e: FileNotFoundException){
                    Log.e("Could Not Resolve URI", "path="+path+" uri="+photoUri.toString())
                }
                return null
            }
        }

        internal fun pathToDisplayName(contentResolver: ContentResolver, path: String): String{
            if(path.startsWith("/")){
                return File(path).name
            }else{
                val cursor = contentResolver.query(
                    path.toUri(), arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                        null,null, null)
                cursor?.moveToFirst()
                val displayName = cursor?.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)) ?: ""
                cursor?.close()
                return displayName
            }
        }
    }
}