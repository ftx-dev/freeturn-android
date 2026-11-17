package com.freeturn.app.freeturnx.socksclient

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.URLEncoder

/** FreeTurnX: адрес SOCKS5 для [Socks5ClientService]; хранится отдельно от настроек родителя. */
data class Socks5ClientConfig(
    val host: String,
    val port: Int,
    val user: String = "",
    val pass: String = "",
) {
    val address: String get() = "$host:$port"

    val proxyUrl: String
        get() {
            val auth = if (user.isEmpty()) "" else "${enc(user)}:${enc(pass)}@"
            return "socks5://$auth$host:$port"
        }

    fun save(context: Context) {
        prefs(context).edit()
            .putString(KEY_HOST, host).putInt(KEY_PORT, port)
            .putString(KEY_USER, user).putString(KEY_PASS, pass)
            .apply()
    }

    companion object {
        /** Порт SOCKS5 Hotspot в FreeTurn по умолчанию. */
        const val DEFAULT_PORT = 1080

        fun load(context: Context): Socks5ClientConfig = prefs(context).let {
            Socks5ClientConfig(
                host = it.getString(KEY_HOST, null) ?: wifiGateway(context).orEmpty(),
                port = it.getInt(KEY_PORT, DEFAULT_PORT),
                user = it.getString(KEY_USER, "").orEmpty(),
                pass = it.getString(KEY_PASS, "").orEmpty(),
            )
        }

        /** Шлюз текущего Wi-Fi: на точке доступа это и есть раздающий телефон. */
        fun wifiGateway(context: Context): String? {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
            @Suppress("DEPRECATION")
            val wifi = cm.allNetworks.firstOrNull {
                cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } ?: return null
            return cm.getLinkProperties(wifi)?.routes
                ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
                ?.gateway?.hostAddress
        }

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        private fun prefs(context: Context) = context.getSharedPreferences("freeturnx_socks5_client", Context.MODE_PRIVATE)
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USER = "user"
        private const val KEY_PASS = "pass"
    }
}
