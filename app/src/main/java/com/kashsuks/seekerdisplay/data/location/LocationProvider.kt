package com.kashsuks.seekerdisplay.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/** Gets a coarse location from the platform [LocationManager], without needing Google Play services. */
class LocationProvider(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** A fresh fix if one arrives within [timeoutMs], else the newest cached fix, else null. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(timeoutMs: Long = 15_000L): Location? {
        if (!hasPermission()) return null
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        for (provider in providers) {
            val fresh = withTimeoutOrNull(timeoutMs) { requestFix(provider) }
            if (fresh != null) return fresh
        }
        return lastKnown()
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFix(provider: String): Location? = suspendCancellableCoroutine { cont ->
        val signal = android.os.CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        runCatching {
            manager.getCurrentLocation(provider, signal, mainExecutor) { location ->
                if (cont.isActive) cont.resume(location)
            }
        }.onFailure { if (cont.isActive) cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(): Location? =
        manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
}
