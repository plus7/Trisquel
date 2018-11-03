package net.tnose.app.trisquel

import android.os.Parcel
import android.os.Parcelable
import java.util.*


/**
 * Created by user on 2018/01/13.
 */

class LensSpec: Parcelable {
    var id: Int = 0
    var body: Int = 0
    var created: Date
    var lastModified: Date
    var mount: String
    var manufacturer: String
    var modelName: String
    var focalLength: String
    val focalLengthRange: Pair<Double, Double>
        get() = Util.getFocalLengthRangeFromStr(focalLength)
    var fSteps: Array<Double> = emptyArray()

    constructor(id: Int, mount: String, body: Int, manufacturer: String,
                modelName: String, focalLength: String, fSteps: String) {
        this.id = id
        this.created = Date()
        this.lastModified = Date()
        this.mount = mount
        this.body = body
        this.manufacturer = manufacturer
        this.modelName = modelName
        this.focalLength = focalLength
        if (fSteps.isNotEmpty()) {
            val fsAsArray = fSteps.split(", ?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val list = ArrayList<Double>()
            for (speed in fsAsArray) {
                list.add(java.lang.Double.parseDouble(speed))
            }
            this.fSteps = list.toTypedArray()
        }
    }

    constructor(id: Int, created: String, lastModified: String, mount: String, body: Int, manufacturer: String,
                modelName: String, focalLength: String, fSteps: String) {
        this.id = id
        this.created = Util.stringToDateUTC(created)
        this.lastModified = Util.stringToDateUTC(lastModified)
        this.mount = mount
        this.body = body
        this.manufacturer = manufacturer
        this.modelName = modelName
        this.focalLength = focalLength
        if (fSteps.isNotEmpty()) {
            val fsAsArray = fSteps.split(", ?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val list = ArrayList<Double>()
            for (speed in fsAsArray) {
                list.add(java.lang.Double.parseDouble(speed))
            }
            this.fSteps = list.toTypedArray()
        }
    }

    override fun toString(): String {
        return modelName /* なんかMaterialBetterSpinnerがtoStringを呼んでるみたいなので、一時的にモデル名にする */
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(id)
        out.writeInt(body)
        out.writeString(Util.dateToStringUTC(created))
        out.writeString(Util.dateToStringUTC(lastModified))
        out.writeString(mount)
        out.writeString(manufacturer)
        out.writeString(modelName)
        out.writeString(focalLength)
        out.writeDoubleArray(fSteps.toDoubleArray())
    }

    constructor(inp: Parcel){
        id = inp.readInt()
        body = inp.readInt()
        created = Util.stringToDateUTC(inp.readString() ?: "")
        lastModified = Util.stringToDateUTC(inp.readString() ?: "")
        mount = inp.readString() ?: ""
        manufacturer = inp.readString() ?: ""
        modelName = inp.readString() ?: ""
        focalLength = inp.readString() ?: ""
        fSteps = inp.createDoubleArray()!!.toTypedArray()
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<LensSpec> = object : Parcelable.Creator<LensSpec> {
            override fun createFromParcel(inp: Parcel): LensSpec {
                return LensSpec(inp)
            }

            override fun newArray(size: Int): Array<LensSpec?> {
                return arrayOfNulls<LensSpec?>(size)
            }
        }
    }
}
