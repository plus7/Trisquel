package net.tnose.app.trisquel

import java.util.*

/**
 * Created by user on 2018/01/13.
 */

class LensSpec {
    var id: Int = 0
    var body: Int = 0
    var created: Date
    var lastModified: Date
    var mount: String
    var manufacturer: String
    var modelName: String
    var focalLength: String
    val minFocalLength: Double
        get() = 0.0
    val maxFocalLength: Double
        get() = 0.0
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
        if (fSteps.length > 0) {
            val fsAsArray = fSteps.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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
        if (fSteps.length > 0) {
            val fsAsArray = fSteps.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val list = ArrayList<Double>()
            for (speed in fsAsArray) {
                list.add(java.lang.Double.parseDouble(speed.replace("[", "").replace("]", "")))
            }
            this.fSteps = list.toTypedArray()
        }
    }

    override fun toString(): String {
        return modelName /* なんかMaterialBetterSpinnerがtoStringを呼んでるみたいなので、一時的にモデル名にする */
    }
}
