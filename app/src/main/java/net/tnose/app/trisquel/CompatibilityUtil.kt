package net.tnose.app.trisquel

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class CompatibilityUtil {
    companion object {
        internal fun pathToInputStream(contentResolver: ContentResolver, path: String, reqOrig: Boolean): InputStream? {
            if(path.startsWith("/")){
                return File(path).inputStream()
            } else {
                val photoUri =
                        if (reqOrig && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStore.setRequireOriginal(Uri.parse(path))
                        }else {
                            Uri.parse(path)
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
                        Uri.parse(path), arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                        null,null, null)
                cursor?.moveToFirst()
                val displayName = cursor?.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)) ?: ""
                cursor?.close()
                return displayName
            }
        }
    }
}