package com.epdev.topotrackapp.utils

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREF_NAME = "user_preferences"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveUserData(context: Context, email: String, name: String = "") {
        val prefs = getSharedPreferences(context)
        prefs.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name.ifEmpty { email.substringBefore("@") })
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getUserEmail(context: Context): String {
        return getSharedPreferences(context).getString(KEY_USER_EMAIL, "") ?: ""
    }
    
    fun getUserName(context: Context): String {
        return getSharedPreferences(context).getString(KEY_USER_NAME, "") ?: ""
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun clearUserData(context: Context) {
        getSharedPreferences(context).edit().clear().apply()
    }
}
