package net.tnose.app.trisquel

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

// 既存のJSON構造との互換性を保つためのデータクラス
data class PinnedFilterItem(
    @SerializedName("type") val type: Int,
    @SerializedName("values") val values: ArrayList<String>
)

class UserPreferencesRepository(private val context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = Gson()

    fun getPinnedFilters(): ArrayList<Pair<Int, ArrayList<String>>> {
        val prefStr = sharedPreferences.getString("pinned_filters", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<PinnedFilterItem>>() {}.type
            val list: List<PinnedFilterItem>? = gson.fromJson(prefStr, type)
            ArrayList(list?.map { Pair(it.type, it.values) } ?: emptyList())
        } catch (e: Exception) {
            ArrayList()
        }
    }

    fun addPinnedFilter(newFilter: Pair<Int, ArrayList<String>>) {
        val currentFilters = getPinnedFilters()
        currentFilters.add(newFilter)
        
        val itemsToSave = currentFilters.map { PinnedFilterItem(it.first, it.second) }
        sharedPreferences.edit { putString("pinned_filters", gson.toJson(itemsToSave)) }
    }

    fun removePinnedFilter(filter: Pair<Int, ArrayList<String>>) {
        val currentFilters = getPinnedFilters()
        val iterator = currentFilters.iterator()
        while (iterator.hasNext()) {
            val f = iterator.next()
            if (f.first == filter.first && f.second.containsAll(filter.second)) {
                iterator.remove()
                break
            }
        }
        
        val itemsToSave = currentFilters.map { PinnedFilterItem(it.first, it.second) }
        sharedPreferences.edit { putString("pinned_filters", gson.toJson(itemsToSave)) }
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

    private fun getStoredList(prefKey: String): MutableList<String> {
        val prefStr = sharedPreferences.getString(prefKey, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<MutableList<String>>() {}.type
            gson.fromJson(prefStr, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun getSuggestList(prefKey: String, defRscId: Int): List<String> {
        val storedList = getStoredList(prefKey)
        val defRsc = context.resources.getStringArray(defRscId).toList()
        return (storedList + defRsc).distinct()
    }

    fun saveSuggestList(prefKey: String, defRscId: Int, newValue: String) {
        if (newValue.isEmpty()) return
        val storedList = getStoredList(prefKey)
        
        storedList.remove(newValue)
        storedList.add(0, newValue)
        
        val defRsc = context.resources.getStringArray(defRscId).toList()
        val result = (storedList + defRsc).distinct()
        
        sharedPreferences.edit { putString(prefKey, gson.toJson(result)) }
    }

    fun saveSuggestList(prefKey: String, defRscId: Int, newValues: Array<String>) {
        val storedList = getStoredList(prefKey)

        for (item in newValues) {
            if (item.isEmpty()) continue
            storedList.remove(item)
            storedList.add(0, item)
        }
        
        val defRsc = context.resources.getStringArray(defRscId).toList()
        val result = (storedList + defRsc).distinct()
        
        sharedPreferences.edit { putString(prefKey, gson.toJson(result)) }
    }

    private fun getStoredMap(parentKey: String): MutableMap<String, List<String>> {
        val prefStr = sharedPreferences.getString(parentKey, "{}") ?: "{}"
        return try {
            val type = object : TypeToken<MutableMap<String, List<String>>>() {}.type
            gson.fromJson(prefStr, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun getSuggestListSub(parentKey: String, subKey: String): List<String> {
        return getStoredMap(parentKey)[subKey] ?: emptyList()
    }

    fun saveSuggestListSub(parentKey: String, subKey: String, newValue: String) {
        if (newValue.isEmpty() || subKey.isEmpty()) return
        
        val map = getStoredMap(parentKey)
        val list = map[subKey]?.toMutableList() ?: mutableListOf()
        
        list.remove(newValue)
        list.add(0, newValue)
        
        map[subKey] = list.distinct()
        sharedPreferences.edit { putString(parentKey, gson.toJson(map)) }
    }

    fun saveSuggestListSub(parentKey: String, subKey: String, values: List<String>) {
        val map = getStoredMap(parentKey)
        map[subKey] = values
        sharedPreferences.edit { putString(parentKey, gson.toJson(map)) }
    }
}
