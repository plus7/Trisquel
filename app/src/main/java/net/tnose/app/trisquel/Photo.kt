package net.tnose.app.trisquel

import android.util.Log
import java.util.*

/**
 * Created by user on 2018/01/13.
 */

class Photo(var id: Int, var filmrollid: Int, var index: Int, var date: String, var cameraid: Int, var lensid: Int,
            var focalLength: Double, var aperture: Double, var shutterSpeed: Double, var expCompensation: Double,
            var ttlLightMeter: Double, var location: String, var latitude: Double, var longitude: Double, var memo: String, accessories: String) {
    var accessories: ArrayList<Int>

    val isValidLatLng: Boolean
        get() = latitude <= 90 && latitude >= -90 && longitude <= 180 && longitude >= -180

    val accessoriesStr: String
        get() = accessories.map{a -> a.toString()}.joinToString("/","/", "/")

    init {
        Log.d("new Photo",
                "id:" + Integer.toString(id) + ", " +
                        "filmroll:" + Integer.toString(filmrollid) + ", " +
                        "index:" + Integer.toString(index) + ", " +
                        "date:" + date + ", " +
                        "lensid:" + Integer.toString(lensid))
        this.accessories = ArrayList()
        for (idStr in accessories.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!idStr.isEmpty()) this.accessories.add(Integer.parseInt(idStr))
        }
    }//内部的にはゼロオリジンで管理する
}
