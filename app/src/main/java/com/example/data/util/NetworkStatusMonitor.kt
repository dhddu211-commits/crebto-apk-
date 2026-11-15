package com.example.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkStatusMonitor {
  fun observeIsOnline(context: Context): Flow<Boolean> =
    callbackFlow {
        val cm =
          context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager
        if (cm == null) {
          trySend(false)
          awaitClose {}
          return@callbackFlow
        }

        fun checkCurrentOnline(): Boolean {
          val active = cm.activeNetwork ?: return false
          val caps = cm.getNetworkCapabilities(active) ?: return false
          return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        trySend(checkCurrentOnline())

        val callback =
          object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
              trySend(checkCurrentOnline())
            }

            override fun onCapabilitiesChanged(
              network: Network,
              networkCapabilities: NetworkCapabilities,
            ) {
              val validated =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
              trySend(validated)
            }

            override fun onLost(network: Network) {
              trySend(checkCurrentOnline())
            }

            override fun onUnavailable() {
              trySend(false)
            }
          }

        val request =
          NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        awaitClose {
          runCatching { cm.unregisterNetworkCallback(callback) }
        }
      }
      .distinctUntilChanged()
}
