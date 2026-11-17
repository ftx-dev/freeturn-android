package com.freeturn.app.freeturnx.socksclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.freeturn.app.R
import com.freeturn.app.data.config.Socks5Config
import com.freeturn.app.ui.components.QrCode
import com.freeturn.app.ui.components.SettingsCard
import com.freeturn.app.ui.components.SettingsEntryRow
import com.freeturn.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address

/**
 * FreeTurnX: QR для SOCKS5 Hotspot - второй телефон сканирует его в SOCKS5-клиенте.
 * [loadConfig] - настройки SOCKS5 Hotspot родителя (порт, логин, пароль).
 */
@Composable
fun HotspotQrCard(loadConfig: suspend () -> Socks5Config) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsCard {
        SettingsEntryRow(
            iconRes = R.drawable.wifi_tethering_24px,
            title = stringResource(R.string.fx_hotspot_qr),
            subtitle = stringResource(R.string.fx_hotspot_qr_desc),
            onClick = { open = true }
        )
    }
    if (!open) return
    val url by produceState<String?>(null) {
        val cfg = loadConfig()
        value = withContext(Dispatchers.IO) { hotspotAddress(context) }
            ?.let { Socks5ClientConfig(it, cfg.port, cfg.user, cfg.pass).proxyUrl }.orEmpty()
    }
    // Без кнопок: закрывается касанием вне окна или «Назад».
    Dialog(onDismissRequest = { open = false }) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                Modifier.padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(stringResource(R.string.fx_hotspot_qr), style = MaterialTheme.typography.titleLarge)
                when {
                    url == null -> Unit
                    url!!.isEmpty() -> Text(stringResource(R.string.fx_hotspot_qr_no_ap))
                    else -> {
                        QrCode(url!!, contentDescription = url, modifier = Modifier.fillMaxWidth())
                        Text(
                            url!!.substringAfterLast('@').removePrefix("socks5://"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Адрес телефона в Wi-Fi сети, к которой он подключён: SOCKS5 Hotspot обслуживает устройства
 * этой же сети. Клиенты точки доступа самого телефона до него не достают (ответы уходят в VPN) -
 * для них есть раздача через Shizuku.
 */
internal fun hotspotAddress(context: android.content.Context): String? {
    val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return null
    @Suppress("DEPRECATION")
    return cm.allNetworks
        .filter { cm.getNetworkCapabilities(it)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true }
        .flatMap { cm.getLinkProperties(it)?.linkAddresses.orEmpty() }
        .map { it.address }
        .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
        ?.hostAddress
}
