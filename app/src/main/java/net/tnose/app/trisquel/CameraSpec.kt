package net.tnose.app.trisquel

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Created by user on 2018/01/13.
 */

class CameraSpec: Parcelable {
    var id: Int = 0
    var type: Int = 0
    var created: Date
        get() = Util.stringToDateUTC(createdStr)
        set(value) {
            createdStr = Util.dateToStringUTC(value)
        }
    private var createdStr: String = ""
    var lastModified: Date
        get() = Util.stringToDateUTC(lastModifiedStr)
        set(value) {
            lastModifiedStr = Util.dateToStringUTC(value)
        }
    private var lastModifiedStr: String = ""
    var mount: String
    var manufacturer: String
    var modelName: String
    var format: Int = 0
    var shutterSpeedGrainSize: Int = 0 //0: custom, 1: 一段, 2: 半段, 3: 1/3段
    var fastestShutterSpeed: Double? = null
    var slowestShutterSpeed: Double? = null
    var bulbAvailable: Boolean = false
    var shutterSpeedSteps: Array<Double> = emptyArray()
    var evGrainSize: Int = 0 //1: 一段, 2: 半段, 3: 1/3段
    var evWidth: Int = 0  // 1～3まで

    constructor(id: Int, type: Int, mount: String, manufacturer: String, modelName: String, format: Int,
                shutterSpeedGrainSize: Int, fastestShutterSpeed: Double?, slowestShutterSpeed: Double?,
                bulbAvailable: Boolean, shutterSpeedSteps: String, evGrainSize: Int, evWidth: Int) {
        this.id = id
        this.type = type
        this.created = Date()
        this.lastModified = Date()
        this.mount = mount
        this.manufacturer = manufacturer
        this.modelName = modelName
        this.format = format
        this.shutterSpeedGrainSize = shutterSpeedGrainSize
        this.fastestShutterSpeed = fastestShutterSpeed
        this.slowestShutterSpeed = slowestShutterSpeed
        this.bulbAvailable = bulbAvailable
        if (shutterSpeedSteps.isNotEmpty()) {
            // ","のあとにスペースがあるのは旧仕様。他も同様
            val sssAsArray = shutterSpeedSteps.split(", ?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val list = ArrayList<Double>()
            for (speed in sssAsArray) {
                list.add(java.lang.Double.parseDouble(speed))
            }
            this.shutterSpeedSteps = list.toTypedArray()
        }
        this.evGrainSize = if (evGrainSize > 3) 3 else if (evGrainSize < 1) 1 else evGrainSize
        this.evWidth = if (evWidth > 3) 3 else if (evWidth < 1) 1 else evWidth
    }

    constructor(id: Int, type: Int, created: String, lastModified: String, mount: String, manufacturer: String, modelName: String, format: Int,
                shutterSpeedGrainSize: Int, fastestShutterSpeed: Double?, slowestShutterSpeed: Double?,
                bulbAvailable: Boolean, shutterSpeedSteps: String, evGrainSize: Int, evWidth: Int) {
        this.id = id
        this.type = type
        this.createdStr = created
        this.lastModifiedStr = lastModified
        this.mount = mount
        this.manufacturer = manufacturer
        this.modelName = modelName
        this.format = format
        this.shutterSpeedGrainSize = shutterSpeedGrainSize
        this.fastestShutterSpeed = fastestShutterSpeed
        this.slowestShutterSpeed = slowestShutterSpeed
        this.bulbAvailable = bulbAvailable
        if (shutterSpeedSteps.length > 0) {
            val sssAsArray = shutterSpeedSteps.split(", ?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val list = ArrayList<Double>()
            for (speed in sssAsArray) {
                try {
                    list.add(java.lang.Double.parseDouble(speed))
                } catch (e: NumberFormatException) {
                }

            }
            this.shutterSpeedSteps = list.toTypedArray()
        }
        this.evGrainSize = if (evGrainSize > 3) 3 else if (evGrainSize < 1) 1 else evGrainSize
        this.evWidth = if (evWidth > 3) 3 else if (evWidth < 1) 1 else evWidth
    }

    override fun toString(): String {
        return "$manufacturer $modelName"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(id)
        out.writeInt(type)
        out.writeString(Util.dateToStringUTC(created))
        out.writeString(Util.dateToStringUTC(lastModified))
        out.writeString(mount)
        out.writeString(manufacturer)
        out.writeString(modelName)
        out.writeInt(format)
        out.writeInt(shutterSpeedGrainSize)
        out.writeDouble(fastestShutterSpeed ?: 0.0)
        out.writeDouble(slowestShutterSpeed ?: 0.0)
        out.writeInt(if(bulbAvailable) 1 else 0)
        out.writeDoubleArray(shutterSpeedSteps.toDoubleArray())
        out.writeInt(evGrainSize)
        out.writeInt(evWidth)
    }

    constructor(inp: Parcel){
        id = inp.readInt()
        type = inp.readInt()
        created = Util.stringToDateUTC(inp.readString() ?: "")
        lastModified = Util.stringToDateUTC(inp.readString() ?: "")
        mount = inp.readString() ?: ""
        manufacturer = inp.readString() ?: ""
        modelName = inp.readString() ?: ""
        format = inp.readInt()
        shutterSpeedGrainSize = inp.readInt()
        fastestShutterSpeed = inp.readDouble()
        slowestShutterSpeed = inp.readDouble()
        bulbAvailable = inp.readInt() > 0
        shutterSpeedSteps = inp.createDoubleArray()!!.toTypedArray()
        evGrainSize = inp.readInt()
        evWidth = inp.readInt()
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<CameraSpec> = object : Parcelable.Creator<CameraSpec> {
            override fun createFromParcel(inp: Parcel): CameraSpec {
                return CameraSpec(inp)
            }

            override fun newArray(size: Int): Array<CameraSpec?> {
                return arrayOfNulls<CameraSpec?>(size)
            }
        }
    }
}
