package com.example.mainactivity.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("facesend_prefs", Context.MODE_PRIVATE)

    var nickname: String
        get() = prefs.getString("nickname", "") ?: ""
        set(value) = prefs.edit().putString("nickname", value).apply()
}
