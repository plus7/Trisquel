package net.tnose.app.trisquel

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.json.JSONArray
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
}
