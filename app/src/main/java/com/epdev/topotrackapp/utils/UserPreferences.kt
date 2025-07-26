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
        val formattedName = if (name.isNotEmpty()) {
            name.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        
        prefs.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, formattedName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getUserEmail(context: Context): String {
        return getSharedPreferences(context).getString(KEY_USER_EMAIL, "") ?: ""
    }
    
    fun getUserName(context: Context): String {
        val name = getSharedPreferences(context).getString(KEY_USER_NAME, "") ?: ""
        return if (name.isNotEmpty()) {
            name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            ""
        }
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun clearUserData(context: Context) {
        getSharedPreferences(context).edit().clear().apply()
    }
}
