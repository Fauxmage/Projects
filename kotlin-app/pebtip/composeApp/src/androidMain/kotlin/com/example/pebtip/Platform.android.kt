package com.example.pebtip

import android.os.Build
import java.net.NetworkInterface

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}
actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getLocalIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.firstOrNull { addr ->
                !addr.isLoopbackAddress && addr.hostAddress?.contains('.') == true
            }
            ?.hostAddress ?: "Unavailable"
    } catch (_: Exception) {
        "Unavailable"
    }
}