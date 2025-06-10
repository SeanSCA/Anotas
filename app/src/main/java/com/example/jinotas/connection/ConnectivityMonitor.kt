package com.example.jinotas.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

class ConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    fun registerCallback(onConnected: () -> Unit) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                onConnected()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }
}