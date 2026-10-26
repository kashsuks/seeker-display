package com.kashsuks.seekerdisplay.data.bridge

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address

/** Finds the Mac bridge on the local network through Bonjour/mDNS. */
class BridgeDiscovery(private val context: Context) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)

    /** Emits the bridge address when found, and null when it disappears. Active only while collected. */
    val endpoint: Flow<BridgeEndpoint?> = callbackFlow {
        val multicastLock = wifi.createMulticastLock("seeker-bridge").apply {
            setReferenceCounted(false)
            acquire()
        }
        val executor = ContextCompat.getMainExecutor(context)
        val infoCallbacks = mutableListOf<NsdManager.ServiceInfoCallback>()

        fun publish(info: NsdServiceInfo, host: String?) {
            if (host != null && info.port > 0) trySend(BridgeEndpoint(host, info.port))
        }

        fun resolve(found: NsdServiceInfo) {
            if (Build.VERSION.SDK_INT >= 34) {
                val callback = object : NsdManager.ServiceInfoCallback {
                    override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {}
                    override fun onServiceUpdated(info: NsdServiceInfo) {
                        publish(info, info.hostAddresses.filterIsInstance<Inet4Address>().firstOrNull()?.hostAddress)
                    }
                    override fun onServiceLost() {
                        trySend(null)
                    }
                    override fun onServiceInfoCallbackUnregistered() {}
                }
                infoCallbacks += callback
                nsd.registerServiceInfoCallback(found, executor, callback)
            } else {
                @Suppress("DEPRECATION")
                nsd.resolveService(found, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        publish(info, info.host?.hostAddress)
                    }
                })
            }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceFound(info: NsdServiceInfo) = resolve(info)
            override fun onServiceLost(info: NsdServiceInfo) {
                trySend(null)
            }
        }
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)

        awaitClose {
            runCatching { nsd.stopServiceDiscovery(listener) }
            if (Build.VERSION.SDK_INT >= 34) {
                infoCallbacks.forEach { runCatching { nsd.unregisterServiceInfoCallback(it) } }
            }
            if (multicastLock.isHeld) multicastLock.release()
        }
    }

    private companion object {
        const val SERVICE_TYPE = "_seekerspotify._tcp."
    }
}
