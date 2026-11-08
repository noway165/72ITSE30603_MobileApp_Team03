package vn.edu.vlu.moneymanager.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tells the screens whether the device has a network, so they can show the offline banner as soon as
 * Airplane Mode is turned on (not only after a request fails) and refresh when the network comes back.
 * Stays true until [start] is called, so JVM unit tests behave as online.
 */
object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val manager = context.getSystemService(ConnectivityManager::class.java)
        _isOnline.value = manager.getNetworkCapabilities(manager.activeNetwork).hasInternet()
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isOnline.value = capabilities.hasInternet()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    private fun NetworkCapabilities?.hasInternet(): Boolean =
        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
