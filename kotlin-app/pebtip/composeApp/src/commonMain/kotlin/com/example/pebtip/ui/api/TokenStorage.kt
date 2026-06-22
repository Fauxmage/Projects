package com.example.pebtip.ui.api

//expect/actual pattern, this defines the interface in commonMain
expect class TokenStorage() {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
    fun saveDarkMode(enabled: Boolean)
    fun getDarkMode(): Boolean?
    fun saveAutoUpload(enabled: Boolean)
    fun getAutoUploadEnabled(): Boolean?
}