package com.freeturn.app.freeturnx.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.freeturn.app.R
import com.freeturn.app.data.AppPreferences
import com.freeturn.app.domain.proxy.ProxyServiceLauncher
import com.freeturn.app.domain.proxy.ProxyStore
import com.freeturn.app.freeturnx.socksclient.Socks5ClientConfig
import com.freeturn.app.freeturnx.socksclient.Socks5ClientService
import com.freeturn.app.tether.ShizukuTether
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** FreeTurnX: общая основа плиток - состояние из потока, пока шторка открыта. */
abstract class FreeTurnXTile : TileService(), KoinComponent {
    protected var scope: CoroutineScope? = null

    protected abstract val label: Int
    protected abstract fun states(): Flow<Int>

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob()).also { s ->
            states().onEach(::render).launchIn(s)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope?.cancel()
        scope = null
    }

    private fun render(state: Int) {
        val tile = qsTile ?: return
        tile.state = state
        tile.label = getString(label)
        tile.contentDescription = tile.label
        tile.icon = Icon.createWithResource(this, R.drawable.wifi_tethering_24px)
        tile.updateTile()
    }

    /** Открыть приложение (нужно согласие или настройка), свернув шторку. */
    protected fun openApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

/** Плитка «Hotspot под VPN»: включает раздачу через Shizuku; работающий VPN переподключается с ней. */
class HotspotTileService : FreeTurnXTile() {
    private val prefs: AppPreferences by inject()
    private val launcher: ProxyServiceLauncher by inject()

    override val label = R.string.fx_tile_hotspot

    override fun states(): Flow<Int> =
        prefs.shizukuTetherEnabledFlow.map { on -> if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE }

    override fun onClick() {
        super.onClick()
        val s = scope ?: return
        s.launch {
            val on = !prefs.shizukuTetherEnabledFlow.first()
            // Включить без Shizuku или разрешения нельзя - это настраивается в приложении.
            if (on && ShizukuTether.unavailable() != null) { openApp(); return@launch }
            prefs.setShizukuTetherEnabled(on)
            // Раздаче нужен туннель, поднятый с ней: работающий VPN переподключается.
            launcher.restartIfRunning()
        }
    }
}

/** Плитка «SOCKS5-клиент»: подключение к последнему адресу или отключение. */
class Socks5TileService : FreeTurnXTile() {
    private val store: ProxyStore by inject()

    override val label = R.string.fx_socks_client

    override fun states(): Flow<Int> = kotlinx.coroutines.flow.combine(Socks5ClientService.state, store.status) { _, vpn ->
        when {
            Socks5ClientService.isActive -> Tile.STATE_ACTIVE
            vpn.busy -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
    }

    override fun onClick() {
        super.onClick()
        when {
            Socks5ClientService.isActive -> Socks5ClientService.stop(this)
            store.status.value.busy -> Unit
            // Нет согласия на VPN или адреса - настраивается в приложении.
            VpnService.prepare(this) != null || Socks5ClientConfig.load(this).host.isBlank() -> openApp()
            // Фоновый старт сервиса система может запретить - тогда через приложение.
            else -> runCatching { Socks5ClientService.start(this) }.onFailure { openApp() }
        }
    }
}
