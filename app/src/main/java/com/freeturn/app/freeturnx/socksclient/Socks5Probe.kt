package com.freeturn.app.freeturnx.socksclient

import java.net.InetSocketAddress
import java.net.Socket

/** FreeTurnX: проверка, что по адресу отвечает SOCKS5 (приветствие RFC 1928). */
object Socks5Probe {
    private const val TIMEOUT_MS = 4_000

    /** null - прокси отвечает; иначе текст ошибки. */
    fun check(config: Socks5ClientConfig): String? = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS
            // Версия 5, методы: без пароля и (если задан) логин/пароль.
            val methods = if (config.user.isEmpty()) byteArrayOf(0) else byteArrayOf(0, 2)
            socket.getOutputStream().apply { write(byteArrayOf(5, methods.size.toByte()) + methods); flush() }
            val reply = ByteArray(2)
            var read = 0
            while (read < 2) {
                val n = socket.getInputStream().read(reply, read, 2 - read)
                if (n < 0) break
                read += n
            }
            when {
                read < 2 || reply[0] != 5.toByte() -> "по адресу ${config.address} не SOCKS5"
                reply[1] == 0xFF.toByte() -> "SOCKS5 ${config.address} не принял логин/пароль"
                else -> null
            }
        }
    }.getOrElse { "SOCKS5 недоступен по адресу ${config.address}" }
}
