package net.wshmkr.launcher.datastore

import android.util.Log
import org.json.JSONArray
import org.json.JSONException

internal fun encodeStringList(values: Iterable<String>): String {
    val jsonArray = JSONArray()
    for (value in values) {
        jsonArray.put(value)
    }
    return jsonArray.toString()
}

internal fun decodeStringList(json: String?): List<String> {
    if (json == null) return emptyList()
    return try {
        val jsonArray = JSONArray(json)
        List(jsonArray.length()) { jsonArray.getString(it) }
    } catch (e: JSONException) {
        Log.w("PreferencesSerialization", "Discarding unparseable preference value", e)
        emptyList()
    }
}
