package com.example.pebtip

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

// Stored in SharedPreferences file {packageName}_pkjs_{uuid}
private const val LATEST_ACCEL_KEY = "latest_accel_raw"

class AndroidPebbleReceiver(
    private val context: Context,
    private val appUuid: UUID
) : PebbleReceiver {

    private val _accelData = MutableSharedFlow<AccelSample>()
    override val accelData = _accelData

    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun startListening(scope: CoroutineScope) {
        stopListening()

        val prefsName = "${context.packageName}_pkjs_$appUuid"
        val sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs = sharedPrefs

        val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key != LATEST_ACCEL_KEY) return@OnSharedPreferenceChangeListener
            val base64 = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
            val bytes = try {
                Base64.decode(base64, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                return@OnSharedPreferenceChangeListener
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            while (buffer.remaining() >= 6) {
                val sample = AccelSample(buffer.short, buffer.short, buffer.short)
                scope.launch { _accelData.emit(sample) }
            }
        }
        listener = changeListener
        sharedPrefs.registerOnSharedPreferenceChangeListener(changeListener)
    }

    fun stopListening() {
        listener?.let { prefs?.unregisterOnSharedPreferenceChangeListener(it) }
        listener = null
        prefs = null
    }
}