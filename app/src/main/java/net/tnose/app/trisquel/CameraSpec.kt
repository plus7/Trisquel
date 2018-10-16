package net.tnose.app.trisquel

import java.util.*

/**
 * Created by user on 2018/01/13.
 */

class CameraSpec {
    var id: Int = 0
    var type: Int = 0
    var created: Date
    var lastModified: Date
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
        this.created = Util.stringToDateUTC(created)
        this.lastModified = Util.stringToDateUTC(lastModified)
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
}
