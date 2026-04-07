package net.tnose.app.trisquel

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

/**
 * Created by user on 2018/01/13.
 */

class FilmRoll {
    var id: Int = 0
    var name: String
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
    //public int format;
    var manufacturer: String
    var brand: String
    var iso: Int = 0
    var cameraid: Int = 0
    var camera: CameraSpec
    var photos: List<Photo>

    val dateRange: String
        get() {
            if (photos.isEmpty()) return ""

            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC)
            var minDate: LocalDate? = null
            var maxDate: LocalDate? = null

            for (p in photos) {
                try {
                    val d = LocalDate.parse(p.date, formatter)
                    if (minDate == null || d.isBefore(minDate)) minDate = d
                    if (maxDate == null || d.isAfter(maxDate)) maxDate = d
                } catch (e: DateTimeParseException) {
                }
            }

            if (minDate == null || maxDate == null) return ""

            return if (minDate == maxDate) {
                minDate.format(formatter)
            } else {
                "${minDate.format(formatter)}-${maxDate.format(formatter)}"
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
        this.createdStr = created
        this.lastModifiedStr = lastModified
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
        this.createdStr = created
        this.lastModifiedStr = lastModified
        this.camera = camera
        //this.format = camera.format;
        this.manufacturer = manufacturer
        this.brand = brand
        this.iso = iso
        this.photos = photos
    }

    fun toEntity(): FilmRollEntity {
        return FilmRollEntity(id,
            Util.dateToStringUTC(created),
            name,
            Util.dateToStringUTC(lastModified),
            camera.id, camera.format.toString(), manufacturer, brand, iso.toString())
    }

    companion object {
        internal fun fromEntity(filmRollAndRels: FilmRollAndRels) : FilmRoll {
            val fEntity = filmRollAndRels.filmRoll
            val cEntity = filmRollAndRels.camera
            //val pEntities = ArrayList(filmRollAndRels.photos.map { Photo.fromEntity(it) })

            //constructor(id: Int, name: String, created: String,
            // lastModified: String, camera: CameraSpec,
            // manufacturer: String, brand: String,
            // iso: Int, exposures: Int) {
            //
            return FilmRoll(fEntity.id, fEntity.name, fEntity.created,
                fEntity.lastModified, CameraSpec.fromEntity(cEntity!!),
                fEntity.manufacturer, fEntity.brand,
                fEntity.iso.toInt(), 0, ArrayList())// pEntities)
        }
    }
}
