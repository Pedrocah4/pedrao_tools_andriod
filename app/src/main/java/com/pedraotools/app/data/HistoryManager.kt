package com.pedraotools.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SavedClient(
    val placeId: String,
    val name: String,
    val address: String,
    val reviewUrl: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)

class HistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getSavedClients(): List<SavedClient> {
        val rawJson = prefs.getString(KEY_SAVED_CLIENTS, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(rawJson)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val placeId = item.optString("placeId")
                    val name = item.optString("name")
                    val address = item.optString("address")
                    val reviewUrl = item.optString("reviewUrl")
                    val savedAt = item.optLong("savedAtMillis", 0L)
                    if (placeId.isNotBlank() && reviewUrl.isNotBlank()) {
                        add(
                            SavedClient(
                                placeId = placeId,
                                name = name,
                                address = address,
                                reviewUrl = reviewUrl,
                                savedAtMillis = savedAt
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveClient(client: SavedClient): List<SavedClient> {
        val current = getSavedClients().toMutableList()
        current.removeAll { it.placeId == client.placeId }
        current.add(0, client)
        persistList(current)
        return current
    }

    fun deleteClient(placeId: String): List<SavedClient> {
        val updated = getSavedClients().filterNot { it.placeId == placeId }
        persistList(updated)
        return updated
    }

    private fun persistList(clients: List<SavedClient>) {
        val jsonArray = JSONArray()
        for (client in clients) {
            val obj = JSONObject()
                .put("placeId", client.placeId)
                .put("name", client.name)
                .put("address", client.address)
                .put("reviewUrl", client.reviewUrl)
                .put("savedAtMillis", client.savedAtMillis)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_CLIENTS, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "pedrao_tools_saved_clients"
        private const val KEY_SAVED_CLIENTS = "saved_clients_json"
    }
}

