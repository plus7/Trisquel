package net.tnose.app.trisquel

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by user on 2018/01/13.
 */

class FilmRoll {
    var id: Int = 0
    var name: String
    var created: Date
    var lastModified: Date
    //public int format;
    var manufacturer: String
    var brand: String
    var iso: Int = 0
    var cameraid: Int = 0
    var camera: CameraSpec
    var photos: List<Photo>

    val dateRange: String
        get() {
            if (photos.size == 0) return ""

            var minDate = Date(java.lang.Long.MAX_VALUE)
            var maxDate = Date(0)
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            for (p in photos) {
                var d = Date(0)
                try {
                    d = sdf.parse(p.date)
                } catch (e: ParseException) {

                }

                if (minDate.after(d)) {
                    minDate = d
                }
                if (maxDate.before(d)) {
                    maxDate = d
                }
            }

            return if (minDate == maxDate) {
                sdf.format(minDate)
            } else {
                sdf.format(minDate) + "-" + sdf.format(maxDate)
            }
        }
    val exposures: Int
        get() = photos.size

    constructor(id: Int, name: String, camera: CameraSpec, manufacturer: String, brand: String, iso: Int, exposures: Int) {
        this.id = id
        this.name = name
        this.created = Date()
        this.lastModified = Date()
        this.camera = camera
        //this.format = camera.format;
        this.manufacturer = manufacturer
        this.brand = brand
        this.iso = iso
        this.photos = ArrayList()
    }

    constructor(id: Int, name: String, created: String, lastModified: String, camera: CameraSpec, manufacturer: String, brand: String, iso: Int, exposures: Int) {
        this.id = id
        this.name = name
        this.created = Util.stringToDateUTC(created)
        this.lastModified = Util.stringToDateUTC(lastModified)
        this.camera = camera
        //this.format = camera.format;
        this.manufacturer = manufacturer
        this.brand = brand
        this.iso = iso
        this.photos = ArrayList()
    }

    constructor(id: Int, name: String, created: String, lastModified: String, camera: CameraSpec,
                manufacturer: String, brand: String, iso: Int, exposures: Int, photos: ArrayList<Photo>) {
        this.id = id
        this.name = name
        this.created = Util.stringToDateUTC(created)
        this.lastModified = Util.stringToDateUTC(lastModified)
        this.camera = camera
        //this.format = camera.format;
        this.manufacturer = manufacturer
        this.brand = brand
        this.iso = iso
        this.photos = photos
    }
}
