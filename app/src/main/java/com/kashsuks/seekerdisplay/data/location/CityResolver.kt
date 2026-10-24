package com.kashsuks.seekerdisplay.data.location

import android.content.Context
import android.location.Geocoder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.coroutines.resume

@Serializable
private data class ReverseGeocodeDto(val city: String = "", val locality: String = "")

/**
 * Turns coordinates into a city name. Uses the platform [Geocoder] when it has a backend,
 * and falls back to BigDataCloud's keyless reverse-geocoding endpoint when it doesn't.
 */
class CityResolver(private val context: Context, private val client: HttpClient) {

    suspend fun cityName(latitude: Double, longitude: Double): String? =
        platformGeocode(latitude, longitude) ?: networkGeocode(latitude, longitude)

    private suspend fun platformGeocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return suspendCancellableCoroutine { cont ->
            runCatching {
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        val address = addresses.firstOrNull()
                        if (cont.isActive) cont.resume(address?.locality ?: address?.subAdminArea)
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }

    private suspend fun networkGeocode(latitude: Double, longitude: Double): String? = runCatching {
        val dto: ReverseGeocodeDto = client.get("https://api.bigdatacloud.net/data/reverse-geocode-client") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("localityLanguage", "en")
        }.body()
        dto.city.ifBlank { dto.locality }.ifBlank { null }
    }.getOrNull()
}
