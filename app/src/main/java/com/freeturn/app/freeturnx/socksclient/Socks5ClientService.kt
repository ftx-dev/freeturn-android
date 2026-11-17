package com.freeturn.app.freeturnx.socksclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import com.freeturn.app.R
import com.freeturn.app.domain.proxy.ProxyPhase
import com.freeturn.app.domain.proxy.ProxyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.concurrent.thread

/**
 * FreeTurnX: SOCKS5-клиент. Весь трафик телефона идёт в чужой SOCKS5 - например, в SOCKS5 Hotspot
 * другого телефона с FreeTurn, к чьему Wi-Fi мы подключены. Ядро FreeTurn здесь не участвует:
 * TUN этого VPN отдаётся helper'у tun2socks (libtether.so, тот же, что у раздачи через Shizuku).
 * Сам helper - наш процесс, а приложение исключено из VPN, поэтому его соединения идут мимо туннеля.
 */
class Socks5ClientService : VpnService(), KoinComponent {

    // Состояние дублируется в общий ProxyStore: главный экран, тайл и виджет показывают
    // SOCKS5-клиент как обычное подключение.
    private val store: ProxyStore by inject()

    sealed interface State {
        data object Idle : State
        data object Connecting : State
        data class Connected(val proxy: String) : State
        data class Failed(val reason: String) : State
    }

    private var tun: ParcelFileDescriptor? = null
    private var helper: Process? = null
    private var helperSocket: LocalSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll(State.Idle)
            return START_NOT_STICKY
        }
        val config = Socks5ClientConfig.load(this)
        startForegroundCompat(config.address)
        // VPN FreeTurn уже работает - SOCKS5-клиент не поднимаем (и наоборот, см. FreeTurnXLauncher).
        if (_state.value !is State.Connected && store.status.value.busy) {
            stopAll(State.Failed(getString(R.string.fx_socks_client_vpn_busy)), publish = false)
            return START_NOT_STICKY
        }
        _state.value = State.Connecting
        store.starting()
        thread(name = "socks5-client-start") {
            val error = runCatching { start(config) }.getOrElse { it.message ?: it.javaClass.simpleName }
            if (error != null) {
                stopAll(State.Failed(error))
            } else {
                _state.value = State.Connected(config.address)
                store.setPhase(SESSION, ProxyPhase.Connected, active = 1, total = 1)
                store.setTunnelUp(true)
            }
        }
        return START_NOT_STICKY
    }

    @Synchronized
    private fun start(config: Socks5ClientConfig): String? {
        stopHelper()
        tun?.close()
        val established = Builder()
            .setSession("FreeTurnX SOCKS5")
            .setMtu(MTU)
            .addAddress(TUN_ADDRESS, 30)
            .addRoute("0.0.0.0", 0)
            // IPv6 - тоже в туннель, иначе он уходил бы мимо прокси.
            .addAddress(TUN_ADDRESS6, 126)
            .addRoute("::", 0)
            .addDnsServer(DNS)
            .addDisallowedApplication(packageName)
            .establish() ?: return "нет разрешения на VPN"
        tun = established

        val path = File(applicationInfo.nativeLibraryDir, HELPER_NAME)
        if (!path.canExecute()) return "нет $HELPER_NAME"
        val socketName = "freeturnx-socks-${android.os.Process.myPid()}-${SystemClock.uptimeMillis()}"
        val server = LocalServerSocket(socketName)
        val proc = ProcessBuilder(path.path, "-socket", socketName, "-proxy", config.proxyUrl, "-mtu", MTU.toString())
            .redirectErrorStream(true)
            .start()
        helper = proc
        thread(isDaemon = true, name = "socks5-client-log") {
            proc.inputStream.bufferedReader().forEachLine { Log.i(TAG, it) }
        }
        var accepted: LocalSocket? = null
        thread(name = "socks5-client-accept") { accepted = runCatching { server.accept() }.getOrNull() }
            .join(HELPER_TIMEOUT_MS)
        runCatching { server.close() }
        val socket = accepted ?: return "helper не запустился"
        socket.setFileDescriptorsForSend(arrayOf(established.fileDescriptor))
        socket.outputStream.write(1)
        socket.outputStream.flush()
        // Сокет держим открытым: его закрытие - сигнал helper'у завершиться.
        helperSocket = socket
        return null
    }

    @Synchronized
    private fun stopHelper() {
        runCatching { helperSocket?.close() }
        helperSocket = null
        helper?.let { p ->
            if (!runCatching { p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) }.getOrDefault(false)) p.destroy()
        }
        helper = null
    }

    private fun stopAll(final: State, publish: Boolean = true) {
        stopHelper()
        runCatching { tun?.close() }
        tun = null
        val wasOurs = _state.value is State.Connecting || _state.value is State.Connected
        _state.value = final
        if (publish && wasOurs) {
            if (final is State.Failed) store.fail(final.reason) else store.idle()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Другой VPN (в том числе основной FreeTurn) занял место - мы уже не работаем.
    override fun onRevoke() = stopAll(State.Idle)

    override fun onDestroy() {
        if (_state.value is State.Connecting || _state.value is State.Connected) stopAll(State.Idle)
        super.onDestroy()
    }

    private fun startForegroundCompat(address: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.fx_socks_client), NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 0, Intent(this, Socks5ClientService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_freeturn)
            .setContentTitle(getString(R.string.fx_socks_client))
            .setContentText(address)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.fx_socks_client_disconnect), stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state.asStateFlow()

        val isActive: Boolean
            get() = _state.value.let { it is State.Connecting || it is State.Connected }

        /** Id сессии в ProxyStore: ядро FreeTurn нумерует свои с 1. */
        private const val SESSION = -7301L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, Socks5ClientService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, Socks5ClientService::class.java).setAction(ACTION_STOP))
        }

        private const val ACTION_STOP = "com.freeturn.app.freeturnx.SOCKS5_CLIENT_STOP"
        private const val CHANNEL = "FreeTurnXSocks5Client"
        private const val NOTIFICATION_ID = 7301
        private const val TAG = "FreeTurnXSocks"
        private const val HELPER_NAME = "libtether.so"
        private const val HELPER_TIMEOUT_MS = 5_000L
        private const val MTU = 1500
        private const val TUN_ADDRESS = "172.19.0.1"
        private const val TUN_ADDRESS6 = "fdfe:dcba:9876::1"
        private const val DNS = "1.1.1.1"
    }
}
