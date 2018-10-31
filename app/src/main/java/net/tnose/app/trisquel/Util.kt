package net.tnose.app.trisquel

/**
 * Created by user on 2018/02/09.
 */

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Util {

    inner class ZoomRange {
        private var wideEnd: Int = 0
        private var teleEnd: Int = 0

        constructor(s: String) {
            if (stringIsZoom(s)) {
                val ss = s.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                wideEnd = Integer.parseInt(ss[0])
                teleEnd = Integer.parseInt(ss[1])
            } else {
                wideEnd = Integer.parseInt(s)
                teleEnd = wideEnd
            }
        }

        constructor(w: Int, t: Int) {
            wideEnd = w
            teleEnd = t
        }
    }

    companion object {
        val TRISQUEL_VERSION = 4

        /* シャッタースピードはたかだか2桁精度なのでdoubleからきれいに変換できる */
        internal fun doubleToStringShutterSpeed(ss: Double): String {
            val inv = 1.0 / ss
            // inv = 3.0 つまり ss = 0.3333... = 1/3 で切りたいところだが0.3と1/3が前後するので余計な条件が入る
            return if (inv >= 3.0 && inv - Math.floor(inv) < 0.1) {
                "1/" + Math.floor(inv).toInt()
            } else {
                if (ss >= 4.0 && ss - Math.floor(ss) < 0.1) {
                    Integer.toString(ss.toInt())
                } else {
                    java.lang.Double.toString(ss)
                }
            }
        }

        internal fun stringToDoubleShutterSpeed(ss: String): Double {
            return if (ss.isEmpty()) {
                0.0
            } else if (ss.indexOf("1/") < 0) { // 手抜き
                java.lang.Double.parseDouble(ss)
            } else {
                1.0 / Integer.parseInt(ss.substring(2)).toDouble()
            }
        }

        internal fun safeStr2Dobule(s : String) : Double{
            try {
                return s.toDouble()
            }catch (e: NumberFormatException){
                return 0.0
            }
        }

        internal fun getFocalLengthRangeFromStr(s: String): Pair<Double, Double>{
            if (s.indexOf("-") > 0) {
                val range = s.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return Pair(Util.safeStr2Dobule(range[0]), Util.safeStr2Dobule(range[1]))
            } else {
                val focalLength = Util.safeStr2Dobule(s)
                return Pair(focalLength, focalLength)
            }
        }

        internal fun stringIsZoom(focalLength: String): Boolean {
            return focalLength.indexOf("-") >= 0
        }

        internal fun stringToDateUTC(s: String): Date {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS")
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            try {
                return sdf.parse(s)
            } catch (e: ParseException) {
                return Date(0)
            }

        }

        internal fun dateToStringUTC(d: Date): String {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS")
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(d)
        }
    }
}
