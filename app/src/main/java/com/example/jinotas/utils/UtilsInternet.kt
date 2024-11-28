package com.example.jinotas.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.measureTimeMillis

object UtilsInternet {
    fun isConnectionGoodEnough(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val downSpeed = networkCapabilities.linkDownstreamBandwidthKbps
                Log.d("InternetUtils", "WiFi speed: $downSpeed Kbps")
                downSpeed > 5000 // Por ejemplo, 5 Mbps
            }

            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val downSpeed = networkCapabilities.linkDownstreamBandwidthKbps
                Log.d("InternetUtils", "Cellular speed: $downSpeed Kbps")
                downSpeed > 2000 // Por ejemplo, 2 Mbps
            }

            else -> false
        }
    }

    fun getMobileSignalStrength(context: Context): Int? {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("InternetUtils", "Permission not granted for signal strength")
            return null
        }

        val allCellInfo = telephonyManager.allCellInfo
        val cellInfo = allCellInfo.firstOrNull()
        return when (cellInfo) {
            is CellSignalStrength -> cellInfo.dbm
            else -> null
        }
    }


    suspend fun isConnectionResponsive(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    val socketAddress = InetSocketAddress("8.8.8.8", 53) // DNS de Google, puerto 53
                    socket.connect(socketAddress, 1000) // Timeout en milisegundos
                    true
                }
            } catch (e: Exception) {
                Log.e("InternetUtils", "Ping failed: ${e.message}")
                false
            }
        }
    }


    suspend fun isConnectionStableAndFast(context: Context): Boolean {
        // Verificar conexión básica
        val isGoodConnection = isConnectionGoodEnough(context)

        // Verificar latencia (ping)
        val isResponsive = isConnectionResponsive()

        // Opcional: Verificar intensidad de señal si está en red celular
        val signalStrength = getMobileSignalStrength(context)
        val isSignalGood = signalStrength == null || signalStrength > -110 // Solo aplica si no es null

        Log.d("InternetUtils", "Connection Good: $isGoodConnection, Responsive: $isResponsive, Signal: $signalStrength")
        return isGoodConnection && isResponsive && isSignalGood
    }


}