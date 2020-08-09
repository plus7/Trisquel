package net.tnose.app.trisquel

import java.io.InputStream
import java.security.MessageDigest

class MD5Util {
    companion object {
        internal fun digestAsStr(ist : InputStream) : String{
            val md5digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(1024 * 128)
            var n = 0
            do{
                n = ist.read(buffer)
                if(n > 0) md5digest.update(buffer, 0, n)
            }while (n > 0)
            val md5bytes = md5digest.digest()
            return md5bytes.map { it ->
                if(it in 1..15) "0" + Integer.toHexString(it.toInt().and(0xFF))
                else Integer.toHexString(it.toInt().and(0xFF))
            }.joinToString("")
        }
    }
}