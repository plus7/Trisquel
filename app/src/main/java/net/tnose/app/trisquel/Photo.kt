package net.tnose.app.trisquel

import org.json.JSONArray
import org.json.JSONException
import java.util.*

/**
 * Created by user on 2018/01/13.
 */

class Photo(var id: Int, var filmrollid: Int, var frameIndex: Int, var date: String, var cameraid: Int, var lensid: Int,
            var focalLength: Double, var aperture: Double, var shutterSpeed: Double, var expCompensation: Double,
            var ttlLightMeter: Double, var location: String, var latitude: Double, var longitude: Double, var memo: String,
            accessories: String, supplementalImages: String, favorite: Boolean) {
    var accessories: ArrayList<Int>
    var supplementalImages: ArrayList<String>
    var favorite: Boolean

    val isValidLatLng: Boolean
        get() = latitude <= 90 && latitude >= -90 && longitude <= 180 && longitude >= -180

    val accessoriesStr: String // 横着してLike演算子で検索したいのでこうなっている
        get() = accessories.map{a -> a.toString()}.joinToString("/","/", "/")

    val supplementalImagesStr: String // アクセサリと違って検索する要件がないので楽なやり方で行く
        get() = JSONArray(supplementalImages).toString()

    companion object {
        val splitter = "/".toRegex()
    }

    init {
        this.accessories = ArrayList()
        for (idStr in accessories.split(splitter).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!idStr.isEmpty()) this.accessories.add(Integer.parseInt(idStr))
        }

        this.supplementalImages = ArrayList()
        try {
            val array = JSONArray(supplementalImages)
            for (i in 0 until array.length()) {
                this.supplementalImages.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        this.favorite = favorite
    }//内部的にはゼロオリジンで管理する
}
