package net.tnose.app.trisquel

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class UserPreferencesRepository(private val context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getPinnedFilters(): ArrayList<Pair<Int, ArrayList<String>>> {
        val prefstr = sharedPreferences.getString("pinned_filters", "[]") ?: "[]"
        val array = JSONArray(prefstr)
        val arrayOfFilter = ArrayList<Pair<Int, ArrayList<String>>>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val filtertype = obj.getInt("type")
            val jsonfiltervalues = obj.getJSONArray("values")
            val filtervalues = ArrayList<String>()
            for (j in 0 until jsonfiltervalues.length()) {
                filtervalues.add(jsonfiltervalues.getString(j))
            }
            arrayOfFilter.add(Pair(filtertype, filtervalues))
        }
        return arrayOfFilter
    }

    fun addPinnedFilter(newfilter: Pair<Int, ArrayList<String>>) {
        val pinnedFilters = getPinnedFilters()
        val array = JSONArray()
        pinnedFilters.forEach { f ->
            val jsonfilter = JSONObject()
            jsonfilter.put("type", f.first)
            jsonfilter.put("values", JSONArray(f.second))
            array.put(jsonfilter)
        }
        val jsonfilter = JSONObject()
        jsonfilter.put("type", newfilter.first)
        jsonfilter.put("values", JSONArray(newfilter.second))
        array.put(jsonfilter)
        sharedPreferences.edit { putString("pinned_filters", array.toString()) }
    }

    fun removePinnedFilter(filter: Pair<Int, ArrayList<String>>) {
        val prefstr = sharedPreferences.getString("pinned_filters", "[]") ?: "[]"
        val array = JSONArray(prefstr)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val filtertype = obj.getInt("type")
            val jsonfiltervalues = obj.getJSONArray("values")
            val filtervalues = ArrayList<String>()
            for (j in 0 until jsonfiltervalues.length()) {
                filtervalues.add(jsonfiltervalues.getString(j))
            }
            if (filtertype == filter.first && filtervalues.containsAll(filter.second)) {
                array.remove(i)
                break
            }
        }
        sharedPreferences.edit { putString("pinned_filters", array.toString()) }
    }

    fun getSortKey(route: String): Int {
        val key = when (route) {
            MainActivity.ROUTE_FILMROLLS -> "filmroll_sortkey"
            MainActivity.ROUTE_CAMERAS -> "camera_sortkey"
            MainActivity.ROUTE_LENSES -> "lens_sortkey"
            MainActivity.ROUTE_ACCESSORIES -> "accessory_sortkey"
            else -> ""
        }
        return if (key.isNotEmpty()) sharedPreferences.getInt(key, 0) else 0
    }

    fun setSortKey(route: String, which: Int) {
        val key = when (route) {
            MainActivity.ROUTE_FILMROLLS -> "filmroll_sortkey"
            MainActivity.ROUTE_CAMERAS -> "camera_sortkey"
            MainActivity.ROUTE_LENSES -> "lens_sortkey"
            MainActivity.ROUTE_ACCESSORIES -> "accessory_sortkey"
            else -> ""
        }
        if (key.isNotEmpty()) sharedPreferences.edit { putInt(key, which) }
    }

    fun getLastVersion(): Int {
        return sharedPreferences.getInt("last_version", 0)
    }

    fun setLastVersion(version: Int) {
        sharedPreferences.edit { putInt("last_version", version) }
    }

    fun isAutocompleteFromPreviousShotEnabled(): Boolean {
        return sharedPreferences.getBoolean("autocomplete_from_previous_shot", false)
    }

    fun setAutocompleteFromPreviousShotEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("autocomplete_from_previous_shot", enabled) }
    }

    fun resetAutocompleteHistory() {
        sharedPreferences.edit {
            putString("lens_manufacturer", "[]")
            putString("camera_manufacturer", "[]")
            putString("camera_mounts", "[]")
            putString("film_manufacturer", "[]")
            putString("film_brand", "{}")
        }
    }

    fun getSuggestList(prefKey: String, defRscId: Int): List<String> {
        val prefstr = sharedPreferences.getString(prefKey, "[]") ?: "[]"
        val strArray = mutableListOf<String>()
        val defRsc = context.resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }
        strArray.addAll(defRsc)
        return strArray.distinct()
    }

    fun saveSuggestList(prefKey: String, defRscId: Int, newValue: String) {
        if (newValue.isEmpty()) return
        val prefstr = sharedPreferences.getString(prefKey, "[]") ?: "[]"
        val strArray = mutableListOf<String>()
        val defRsc = context.resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        if (strArray.contains(newValue)) {
            strArray.remove(newValue)
        }
        strArray.add(0, newValue)
        strArray.addAll(defRsc)

        val result = JSONArray(strArray.distinct())
        sharedPreferences.edit { putString(prefKey, result.toString()) }
    }

    fun saveSuggestList(prefKey: String, defRscId: Int, newValues: Array<String>) {
        val prefstr = sharedPreferences.getString(prefKey, "[]") ?: "[]"
        val strArray = mutableListOf<String>()
        val defRsc = context.resources.getStringArray(defRscId)
        try {
            val array = JSONArray(prefstr)
            for (i in 0 until array.length()) {
                strArray.add(array.getString(i))
            }
        } catch (e: JSONException) {
        }

        for (item in newValues) {
            if (item.isEmpty()) continue
            if (strArray.contains(item)) {
                strArray.remove(item)
            }
            strArray.add(0, item)
        }
        strArray.addAll(defRsc)

        val result = JSONArray(strArray.distinct())
        sharedPreferences.edit { putString(prefKey, result.toString()) }
    }

    fun getSuggestListSub(parentKey: String, subKey: String): List<String> {
        val prefstr = sharedPreferences.getString(parentKey, "{}") ?: "{}"
        val strArray = mutableListOf<String>()
        try {
            val obj = JSONObject(prefstr)
            if (!obj.isNull(subKey)) {
                val array = obj.getJSONArray(subKey)
                for (i in 0 until array.length()) {
                    strArray.add(array.getString(i))
                }
            }
        } catch (e: JSONException) {
        }
        return strArray
    }

    fun saveSuggestListSub(parentKey: String, subKey: String, newValue: String) {
        if (newValue.isEmpty() || subKey.isEmpty()) return
        val prefstr = sharedPreferences.getString(parentKey, "{}") ?: "{}"
        val strArray = mutableListOf<String>()
        var obj: JSONObject
        try {
            obj = JSONObject(prefstr)
            if (!obj.isNull(subKey)) {
                val array = obj.getJSONArray(subKey)
                for (i in 0 until array.length()) {
                    strArray.add(array.getString(i))
                }
            }
        } catch (e: JSONException) {
            obj = JSONObject()
        }

        if (strArray.contains(newValue)) {
            strArray.remove(newValue)
        }
        strArray.add(0, newValue)

        try {
            obj.put(subKey, JSONArray(strArray.distinct()))
            sharedPreferences.edit { putString(parentKey, obj.toString()) }
        } catch (e: JSONException) {
        }
    }

    fun saveSuggestListSub(parentKey: String, subKey: String, values: List<String>) {
        val prefstr = sharedPreferences.getString(parentKey, "{}") ?: "{}"
        var obj: JSONObject
        try {
            obj = JSONObject(prefstr)
        } catch (e: JSONException) {
            obj = JSONObject()
        }

        try {
            obj.put(subKey, JSONArray(values))
            sharedPreferences.edit { putString(parentKey, obj.toString()) }
        } catch (e: JSONException) {
        }
    }
}
