package com.example.prayer.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.prayer.model.UserLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {
    private const val PREFS_NAME = "prayer_location_prefs"
    private const val KEY_LAT = "cached_gps_lat"
    private const val KEY_LNG = "cached_gps_lng"
    private const val KEY_NAME = "cached_gps_name"
    private const val KEY_IS_GPS = "cached_is_gps"

    fun getLastSavedLocation(context: Context): UserLocation? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_LAT) && prefs.contains(KEY_LNG)) {
            val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
            val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
            val name = prefs.getString(KEY_NAME, "GPS Location") ?: "GPS Location"
            val isGps = prefs.getBoolean(KEY_IS_GPS, true)
            return UserLocation(
                latitude = lat,
                longitude = lng,
                locationName = name,
                isGps = isGps
            )
        }
        return null
    }

    fun saveLocation(context: Context, location: UserLocation) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAT, location.latitude.toString())
            .putString(KEY_LNG, location.longitude.toString())
            .putString(KEY_NAME, location.locationName)
            .putBoolean(KEY_IS_GPS, location.isGps)
            .apply()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(context: Context): UserLocation? {
        if (!hasLocationPermission(context)) return null

        // 1. Check Google Play Services Fused Location Provider
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            // Try fast last-known location first
            val lastLocation: Location? = suspendCancellableCoroutine { continuation ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> continuation.resume(loc) }
                    .addOnFailureListener { continuation.resume(null) }
            }

            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time < 1000 * 60 * 60)) {
                val name = resolveLocationName(context, lastLocation.latitude, lastLocation.longitude)
                val userLoc = UserLocation(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    locationName = name,
                    isGps = true
                )
                saveLocation(context, userLoc)
                return userLoc
            }

            // Fresh accurate current location
            val cts = CancellationTokenSource()
            val freshLocation: Location? = suspendCancellableCoroutine { continuation ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> continuation.resume(loc) }
                    .addOnFailureListener { continuation.resume(null) }
                continuation.invokeOnCancellation { cts.cancel() }
            }

            if (freshLocation != null) {
                val name = resolveLocationName(context, freshLocation.latitude, freshLocation.longitude)
                val userLoc = UserLocation(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    locationName = name,
                    isGps = true
                )
                saveLocation(context, userLoc)
                return userLoc
            } else if (lastLocation != null) {
                val name = resolveLocationName(context, lastLocation.latitude, lastLocation.longitude)
                val userLoc = UserLocation(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    locationName = name,
                    isGps = true
                )
                saveLocation(context, userLoc)
                return userLoc
            }
        } catch (e: Exception) {
            // Fallback to system LocationManager
        }

        // 2. Android standard LocationManager fallback
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            val name = resolveLocationName(context, loc.latitude, loc.longitude)
                            val userLoc = UserLocation(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                locationName = name,
                                isGps = true
                            )
                            saveLocation(context, userLoc)
                            return userLoc
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    fun resolveLocationName(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                val country = addr.countryCode ?: addr.countryName
                if (locality != null && country != null) {
                    "$locality, $country"
                } else if (locality != null) {
                    locality
                } else {
                    String.format(Locale.US, "%.4f°N, %.4f°E", lat, lng)
                }
            } else {
                String.format(Locale.US, "%.4f°N, %.4f°E", lat, lng)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.4f°N, %.4f°E", lat, lng)
        }
    }
}
