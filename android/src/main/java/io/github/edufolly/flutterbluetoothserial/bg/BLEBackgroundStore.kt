package io.github.edufolly.flutterbluetoothserial.bg

import android.content.Context
import io.github.edufolly.flutterbluetoothserial.bg.param.StartServiceParam
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BLEBackgroundStore(context: Context) {

    companion object {
        const val prefName = "ble_background"
        const val paramKey = "ble_background_param"
        const val addressKey = "ble_background_address"
    }

    private val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    fun putServiceParam(param: StartServiceParam) {
        val json = Json.encodeToString(param)
        pref.edit().putString(paramKey, json).apply()
    }

    fun getServiceParam(): StartServiceParam? {
        return try {
            pref.getString(paramKey, null)?.let { json ->
                Json.decodeFromString(json)
            }
        } catch (e: Exception) {
            removeServiceParam()
            null
        }
    }

    fun removeServiceParam() {
        pref.edit().remove(paramKey).apply()
    }

    fun putAddressSet(addresses: Set<String>) {
        pref.edit().putStringSet(addressKey, addresses).apply()
    }

    fun getAddressSet(): MutableSet<String> {
        return pref.getStringSet(addressKey, mutableSetOf()) ?: mutableSetOf()
    }
}