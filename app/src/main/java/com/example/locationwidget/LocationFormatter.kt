package com.example.locationwidget

import android.location.Address
import android.location.Location
import java.util.Locale

object LocationFormatter {
    fun fallbackCity(location: Location): String {
        return String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)
    }

    fun cityFromAddress(address: Address?): String {
        return address?.locality
            ?: address?.subLocality
            ?: address?.subAdminArea
            ?: address?.adminArea
            ?: "Brak informacji"
    }

    fun postalFromAddress(address: Address?): String {
        return address?.postalCode ?: "Brak kodu"
    }
}
