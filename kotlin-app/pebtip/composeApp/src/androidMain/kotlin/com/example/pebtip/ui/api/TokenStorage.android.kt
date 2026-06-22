package com.example.pebtip.ui.api

import android.content.Context
import android.content.SharedPreferences

private lateinit var appContext: Context

//Andorid implementation using sharedpreferences for persistent key-value storage
actual class TokenStorage actual constructor() {
    //Lazy initialized so appcontext is set before first access
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("pebtip_prefs", Context.MODE_PRIVATE)
    }

    //Save token to sharedpreferences to survive app restarts and backgrounding
    actual fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    //returns null if no token has been saved yet
    actual fun getToken(): String? = prefs.getString("auth_token", null)

    //remove token if invalid or user logs out
    actual fun clearToken(){
        prefs.edit().remove("auth_token").apply()
    }

    //Save dark mode preference to sharedPreferences
    actual fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    //Returns saved dark mode, defaults to system setting if not set
    actual fun getDarkMode(): Boolean? =
        if (prefs.contains("dark_mode"))
            prefs.getBoolean("dark_mode", false)
        else null

    //Save auto upload preference to sharedPreferences
    actual fun saveAutoUpload(enabled: Boolean) {
        prefs.edit().putBoolean("auto_upload", enabled).apply()
    }

    //Returns saved auto upload preference, defaults to disabled if not set
    actual fun getAutoUploadEnabled(): Boolean? =
        if (prefs.contains("auto_upload"))
            prefs.getBoolean("auto_upload", false)
        else null

    companion object{
        //Must be called once from MainActivity
        fun init(context: Context){
            appContext = context.applicationContext
        }
    }
}