package com.example.pebtip.ui.api

import platform.Foundation.NSUserDefaults

//using NSUserDefaults for persistent key-value storage
actual class TokenStorage actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    //save token, syncronize makes immediate write to disk
    actual fun saveToken(token: String) {
        defaults.setObject(token, "auth_token")
        defaults.synchronize()
    }

    //Returns null if no token has been saved yet
    actual fun getToken(): String? = defaults.stringForKey("auth_token")

    //Remove token when invalid or user log out
    actual fun clearToken() {
        defaults.removeObjectForKey("auth_token")
        defaults.synchronize()
    }

    //save dark mode to NSUserDefaults
    actual fun saveDarkMode(enabled: Boolean){
        defaults.setBool(enabled, "dark_mode")
    }

    //returns saved dark mode preference, defaults to system settings if not set
    actual fun getDarkMode(): Boolean? =
        if (defaults.objectForKey("dark_mode") != null)
            defaults.boolForKey("dark_mode")
        else null

    actual fun saveAutoUpload(enabled: Boolean) {
        defaults.setBool(enabled, "auto_upload")
        defaults.synchronize()
    }

    actual fun getAutoUploadEnabled(): Boolean? =
        if (defaults.objectForKey("auto_upload") != null)
            defaults.boolForKey("auto_upload")
        else null
}