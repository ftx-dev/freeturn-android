package com.freeturn.app.freeturnx

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import java.net.Inet4Address

/**
 * FreeTurnX: при раздаче (SOCKS5 Hotspot, Shizuku) приложение остаётся в своём VPN, и ответы
 * устройствам локальной сети - вплоть до SYN-ACK на подключение к SOCKS5 Hotspot - уходили
 * в туннель: из сети до раздачи было не достучаться. Подсети, к которым телефон подключён
 * напрямую (домашний Wi-Fi и т. п.), исключаются из маршрутов VPN.
 * excludeRoute есть с Android 13; раньше - без изменений.
 */
/** Что исключили при последнем открытии туннеля - для журнала. */
@Volatile var lastExcludedSubnets: List<String> = emptyList()
    private set

fun VpnService.Builder.excludeLocalSubnets(context: Context): VpnService.Builder = apply {
    lastExcludedSubnets = emptyList()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@apply
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return@apply
    @Suppress("DEPRECATION")
    cm.allNetworks
        .filter { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == false }
        .flatMap { cm.getLinkProperties(it)?.routes.orEmpty() }
        // Прямая подсеть интерфейса: без шлюза, не маршрут по умолчанию, IPv4.
        .filter { !it.hasGateway() && !it.isDefaultRoute && it.destination.address is Inet4Address }
        .map { it.destination }
        .filter { it.prefixLength in 8..30 && it.address.isSiteLocalAddress }
        .distinct()
        .filter { runCatching { excludeRoute(it) }.isSuccess }
        .also { lastExcludedSubnets = it.map(Any::toString) }
}

/**
 * Слушающий сокет SOCKS5 Hotspot - мимо VPN: иначе ответ на подключение (SYN-ACK) клиенту из
 * локальной сети уходит в туннель, и порт «висит». Принятые соединения наследуют метку сокета.
 * Дескриптор ServerSocket в публичном API не отдаётся - достаём через SocketImpl.
 */
fun VpnService.protectServerSocket(server: java.net.ServerSocket): String = runCatching {
    val impl = java.net.ServerSocket::class.java.getDeclaredMethod("getImpl").apply { isAccessible = true }.invoke(server)
    val fd = java.net.SocketImpl::class.java.getDeclaredField("fd").apply { isAccessible = true }.get(impl) as java.io.FileDescriptor
    android.os.ParcelFileDescriptor.dup(fd).use { dup ->
        if (protect(dup.fd)) "слушающий сокет вне VPN" else "protect слушающего сокета отклонён"
    }
}.getOrElse { "слушающий сокет не защищён: ${it.javaClass.simpleName} ${it.message}" }
