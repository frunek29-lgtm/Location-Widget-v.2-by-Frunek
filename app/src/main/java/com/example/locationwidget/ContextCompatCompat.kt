package com.example.locationwidget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object ContextCompatCompat {
    fun startForegroundServiceCompat(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
