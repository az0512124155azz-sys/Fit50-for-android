package com.fit50.app

import android.content.Context

class Fit50Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("fit50", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()

    var guest: Boolean
        get() = prefs.getBoolean("guest", false)
        set(value) = prefs.edit().putBoolean("guest", value).apply()

    var completed: Int
        get() = prefs.getInt("completed", 0)
        set(value) = prefs.edit().putInt("completed", value).apply()

    var minutes: Int
        get() = prefs.getInt("minutes", 0)
        set(value) = prefs.edit().putInt("minutes", value).apply()

    var streak: Int
        get() = prefs.getInt("streak", 0)
        set(value) = prefs.edit().putInt("streak", value).apply()
}
