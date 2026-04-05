package com.androidide

import android.app.Application
import android.util.Log

/**
 * AndroidIDEApplication.kt
 *
 * Application class — initialises global state.
 * Toolchain setup is deferred to MainViewModel to keep startup fast.
 */
class AndroidIDEApplication : Application() {

    companion object {
        private const val TAG = "AndroidIDEApp"
        lateinit var instance: AndroidIDEApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "Android IDE Native started — version ${BuildConfig.VERSION_NAME}")
    }
}
