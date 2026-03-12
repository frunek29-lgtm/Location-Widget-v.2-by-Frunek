package com.example.locationwidget

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class LocationUpdateService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val hasStartedUpdates = AtomicBoolean(false)

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            persistLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Uruchamianie lokalizacji..."))
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMaxUpdateDelayMillis(10_000L)
            .setWaitForAccurateLocation(false)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startSafeUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startSafeUpdates() {
        if (!PermissionUtils.hasForegroundLocationPermission(this)) {
            WidgetUpdater.saveState(this, "Brak pozwolenia", "—", "Nadaj lokalizację w aplikacji")
            WidgetUpdater.updateAllWidgets(this)
            stopSelf()
            return
        }

        if (!hasStartedUpdates.compareAndSet(false, true)) return

        try {
            WidgetUpdater.saveState(this, "Szukanie pozycji...", "—", "GPS / sieć w trakcie")
            WidgetUpdater.updateAllWidgets(this)

            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) persistLocation(location)
                }
                .addOnFailureListener {
                    WidgetUpdater.saveState(this, "Brak informacji", "—", "Nie udało się pobrać ostatniej pozycji")
                    WidgetUpdater.updateAllWidgets(this)
                }

            fusedClient.requestLocationUpdates(locationRequest, callback, mainLooper)
        } catch (se: SecurityException) {
            WidgetUpdater.saveState(this, "Brak pozwolenia", "—", "Brak dostępu do lokalizacji")
            WidgetUpdater.updateAllWidgets(this)
            stopSelf()
        } catch (t: Throwable) {
            WidgetUpdater.saveState(this, "Brak informacji", "—", "Błąd usługi lokalizacji")
            WidgetUpdater.updateAllWidgets(this)
            stopSelf()
        }
    }

    private fun persistLocation(location: Location) {
        resolveAddress(location) { city, postal, status ->
            WidgetUpdater.saveState(this, city, postal, status)
            WidgetUpdater.updateAllWidgets(this)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification("$city, $postal"))
        }
    }

    private fun resolveAddress(location: Location, done: (String, String, String) -> Unit) {
        val fallbackCity = LocationFormatter.fallbackCity(location)

        if (!Geocoder.isPresent()) {
            done(fallbackCity, "Brak kodu", "Pokazuję współrzędne GPS")
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val address = addresses.firstOrNull()
                            val city = LocationFormatter.cityFromAddress(address).let {
                                if (it == "Brak informacji") fallbackCity else it
                            }
                            val postal = LocationFormatter.postalFromAddress(address)
                            val status = if (address == null) "Pokazuję współrzędne GPS" else "Lokalizacja OK"
                            done(city, postal, status)
                        }

                        override fun onError(errorMessage: String?) {
                            done(fallbackCity, "Brak kodu", "Błąd geokodera, pokazuję GPS")
                        }
                    })
            } else {
                @Suppress("DEPRECATION")
                val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                val city = LocationFormatter.cityFromAddress(address).let {
                    if (it == "Brak informacji") fallbackCity else it
                }
                val postal = LocationFormatter.postalFromAddress(address)
                val status = if (address == null) "Pokazuję współrzędne GPS" else "Lokalizacja OK"
                done(city, postal, status)
            }
        } catch (_: IOException) {
            done(fallbackCity, "Brak kodu", "Brak sieci do geokodera")
        } catch (_: IllegalArgumentException) {
            done(fallbackCity, "Brak kodu", "Nieprawidłowa pozycja GPS")
        } catch (_: Throwable) {
            done(fallbackCity, "Brak kodu", "Nie udało się odczytać adresu")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            fusedClient.removeLocationUpdates(callback)
        } catch (_: Throwable) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Location Widget")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Widget Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "location_widget_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false

        fun start(context: Context) {
            val intent = Intent(context, LocationUpdateService::class.java)
            ContextCompatCompat.startForegroundServiceCompat(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationUpdateService::class.java)
            context.stopService(intent)
            isRunning = false
        }
    }
}
