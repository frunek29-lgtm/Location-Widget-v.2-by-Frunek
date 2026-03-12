package com.example.locationwidget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updateStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val permissionButton: Button = findViewById(R.id.permissionButton)
        val backgroundButton: Button = findViewById(R.id.backgroundButton)
        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        permissionButton.setOnClickListener { requestForegroundPermissions() }
        backgroundButton.setOnClickListener { openAppSettings() }
        startButton.setOnClickListener {
            if (!PermissionUtils.hasForegroundLocationPermission(this)) {
                requestForegroundPermissions()
            } else {
                LocationUpdateService.start(this)
                updateStatus()
            }
        }
        stopButton.setOnClickListener {
            LocationUpdateService.stop(this)
            WidgetUpdater.saveState(this, "Zatrzymano", "—", "Usługa wyłączona")
            updateWidgetNow(this)
            updateStatus()
        }

        if (getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CITY, null) == null) {
            WidgetUpdater.saveState(this, "Brak danych", "—", "Nadaj lokalizację i kliknij Start")
        }
        updateWidgetNow(this)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun requestForegroundPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun updateStatus() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_CITY, "Brak danych") ?: "Brak danych"
        val postal = prefs.getString(KEY_POSTAL, "—") ?: "—"
        val running = LocationUpdateService.isRunning
        val fg = if (PermissionUtils.hasForegroundLocationPermission(this)) "OK" else "BRAK"
        val bg = if (PermissionUtils.hasBackgroundLocationPermission(this)) "OK" else "BRAK"

        statusText.text = buildString {
            append("Lokalizacja w użyciu: $fg\n")
            append("Lokalizacja w tle: $bg\n")
            append("Usługa: ${if (running) "WŁĄCZONA" else "WYŁĄCZONA"}\n")
            append("Ostatni odczyt: $city, $postal\n\n")
            append("Dla pracy po wygaszeniu ekranu ustaw w uprawnieniach: Lokalizacja -> Zezwalaj zawsze.")
        }
    }
}
