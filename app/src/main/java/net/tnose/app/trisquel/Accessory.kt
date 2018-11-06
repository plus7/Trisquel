package net.tnose.app.trisquel

import java.util.*

/**
 * Created by user on 2018/07/12.
 */

class Accessory {
    var id: Int = -1
    var created: Date
    var last_modified: Date
    var type: Int = 0
    var name: String
    var mount: String = ""
    var focal_length_factor: Double = 0.0

    //conversion lens
    constructor(id: Int, created: String, last_modified: String, type: Int, name: String,
                mount: String?, focal_length_factor: Double) {
        this.id = id
        this.created = Util.stringToDateUTC(created)
        this.last_modified = Util.stringToDateUTC(last_modified)
        this.type = type
        this.name = name
        if(mount != null)
            this.mount = mount
        this.focal_length_factor = focal_length_factor
    }

    //filter, unknown
    constructor(id: Int, created: String, last_modified: String, type: Int, name: String) {
        this.id = id
        this.created = Util.stringToDateUTC(created)
        this.last_modified = Util.stringToDateUTC(last_modified)
        this.type = type
        this.name = name
        this.mount = ""
        this.focal_length_factor = 0.0
    }

    //extension tube
    constructor(id: Int, created: String, last_modified: String, type: Int, name: String, mount: String?) {
        this.id = id
        this.created = Util.stringToDateUTC(created)
        this.last_modified = Util.stringToDateUTC(last_modified)
        this.type = type
        this.name = name
        if(mount != null)
            this.mount = mount
        this.focal_length_factor = 0.0
    }

    constructor(id: Int, type: Int, name: String, mount: String?, focal_length_factor: Double) {
        this.id = id
        this.created = Date()
        this.last_modified = Date()
        this.type = type
        this.name = name
        if(mount != null)
            this.mount = mount
        this.focal_length_factor = focal_length_factor
    }

    companion object {
        const val ACCESSORY_UNKNOWN = 0
        const val ACCESSORY_FILTER = 1
        const val ACCESSORY_TELE_CONVERTER = 2
        const val ACCESSORY_WIDE_CONVERTER = 3
        const val ACCESSORY_EXT_TUBE = 4
    }
}
