package com.example.jinotas.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.muddassir.connection_checker.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

object UtilsInternet {

    var isConnectedToInternet: Boolean? = null

    // Verifica si hay una red activa y funcional
    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION") networkInfo.isConnected
        }
    }

    // Comprueba si la conexión es buena basándose en el ancho de banda
    private fun isConnectionGoodEnough(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val downSpeed = capabilities.linkDownstreamBandwidthKbps
                Log.d("InternetUtils", "WiFi speed: $downSpeed Kbps")
                downSpeed > 5000 // Mínimo 5 Mbps
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val downSpeed = capabilities.linkDownstreamBandwidthKbps
                Log.d("InternetUtils", "Cellular speed: $downSpeed Kbps")
                downSpeed > 2000 // Mínimo 2 Mbps
            }

            else -> false
        }
    }

    // Realiza un ping para verificar la capacidad de respuesta
    private suspend fun isConnectionResponsive(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    val address = InetSocketAddress("8.8.8.8", 53) // DNS de Google
                    val timeTaken = measureTimeMillis {
                        socket.connect(address, 2000) // Timeout de 2 segundos
                    }
                    Log.d("InternetUtils", "Ping successful in $timeTaken ms")
                    true
                }
            } catch (e: Exception) {
                Log.e("InternetUtils", "Ping failed: ${e.message}")
                false
            }
        }
    }

    // Obtiene la intensidad de la señal móvil
    private fun getMobileSignalStrength(context: Context): Int? {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("InternetUtils", "Permission not granted for signal strength")
            return null
        }

        val allCellInfo = telephonyManager.allCellInfo
        val cellInfo = allCellInfo?.firstOrNull()
        return when (cellInfo) {
            is android.telephony.CellInfoGsm -> cellInfo.cellSignalStrength.dbm
            is android.telephony.CellInfoLte -> cellInfo.cellSignalStrength.dbm
            is android.telephony.CellInfoCdma -> cellInfo.cellSignalStrength.dbm
            is android.telephony.CellInfoWcdma -> cellInfo.cellSignalStrength.dbm
            else -> null
        }
    }

    // Verifica si la conexión es estable y rápida
    suspend fun isConnectionStableAndFast(context: Context): Boolean {
        return try {
            if (checkForInternet(context)) {
                val isGoodConnection = isConnectionGoodEnough(context)
                val isResponsive = isConnectionResponsive()
                val signalStrength = getMobileSignalStrength(context)
                val isSignalGood = signalStrength == null || signalStrength > -110

                Log.d(
                    "InternetUtils",
                    "Connection Good: $isGoodConnection, Responsive: $isResponsive, Signal: $isSignalGood"
                )
                isGoodConnection && isResponsive
            } else {
                Log.d("InternetUtils", "No active internet connection")
                false
            }
        } catch (e: Exception) {
            Log.e("InternetUtils", "Error verifying connection: ${e.message}")
            false
        }
    }

    fun checkConnectivity(state: ConnectionState, context: Context): Boolean {
        Log.e("ejecuta", "aaaaaaaaaaaaaaaaaaaaaaaa")
        return when (state) {
            ConnectionState.CONNECTED -> {
                Toast.makeText(context, "Has recuperado la conexión", Toast.LENGTH_LONG).show()
                true
            }

            ConnectionState.SLOW -> {
                Toast.makeText(context, "La conexión es lenta", Toast.LENGTH_LONG).show()
                false
            }

            else -> {
                Toast.makeText(context, "Has perdido la conexión", Toast.LENGTH_LONG).show()
                false
            }

        }
    }
}
