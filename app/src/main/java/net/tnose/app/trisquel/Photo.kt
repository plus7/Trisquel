package net.tnose.app.trisquel

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONException

/**
 * Created by user on 2018/01/13.
 */

class Photo(var id: Int, var filmrollid: Int, var frameIndex: Int, var date: String, var cameraid: Int, var lensid: Int,
            var focalLength: Double, var aperture: Double, var shutterSpeed: Double, var expCompensation: Double,
            var ttlLightMeter: Double, var location: String, var latitude: Double, var longitude: Double, var memo: String,
            accessories: String, supplementalImages: String, favorite: Boolean): Parcelable {
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

        @JvmField val CREATOR: Parcelable.Creator<Photo> = object : Parcelable.Creator<Photo> {
            override fun createFromParcel(inp: Parcel): Photo {
                return Photo(inp)
            }

            override fun newArray(size: Int): Array<Photo?> {
                return arrayOfNulls<Photo?>(size)
            }
        }
        internal fun fromEntity(entity: PhotoEntity) : Photo {
            return Photo(entity.id, entity.filmroll!!, entity._index!!,
                entity.date, entity.camera!!, entity.lens!!,
                entity.focalLength!!, entity.aperture!!, entity.shutterSpeed!!,
                entity.expCompensation!!, entity.ttlLightMeter!!,
                entity.location, entity.latitude!!, entity.longitude!!,
                entity.memo, entity.accessories, entity.suppimgs, entity.favorite!! == 1)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        //accessories: String, supplementalImages: String, favorite: Boolean
        out.writeInt(id)
        out.writeInt(filmrollid)
        out.writeInt(frameIndex)
        out.writeString(date)
        out.writeInt(cameraid)
        out.writeInt(lensid)
        out.writeDouble(focalLength)
        out.writeDouble(aperture)
        out.writeDouble(shutterSpeed)
        out.writeDouble(expCompensation)
        out.writeDouble(ttlLightMeter)
        out.writeString(location)
        out.writeDouble(latitude)
        out.writeDouble(longitude)
        out.writeString(memo)
        out.writeIntArray(accessories.toIntArray())
        out.writeStringList(supplementalImages)
        out.writeInt(if(favorite) 1 else 0)
    }

    // これでいいのか？
    constructor(inp: Parcel) : this(0, 0, 0, "", 0, 0,
            0.0, 0.0, 0.0, 0.0,
            0.0, "", 0.0, 0.0, "", "", "", false){
        id = inp.readInt()
        filmrollid = inp.readInt()
        frameIndex = inp.readInt()
        date = inp.readString() ?: ""
        cameraid = inp.readInt()
        lensid = inp.readInt()
        focalLength = inp.readDouble()
        aperture = inp.readDouble()
        shutterSpeed = inp.readDouble()
        expCompensation = inp.readDouble()
        ttlLightMeter = inp.readDouble()
        location = inp.readString() ?: ""
        latitude = inp.readDouble()
        longitude = inp.readDouble()
        memo = inp.readString() ?: ""
        accessories = inp.createIntArray()!!.toCollection(ArrayList())
        supplementalImages = inp.createStringArrayList()!!
        favorite = inp.readInt() > 0
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
