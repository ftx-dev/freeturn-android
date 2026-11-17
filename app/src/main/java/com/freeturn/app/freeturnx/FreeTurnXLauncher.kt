package com.freeturn.app.freeturnx

import android.content.Context
import com.freeturn.app.domain.proxy.ProxyServiceLauncher
import com.freeturn.app.freeturnx.socksclient.Socks5ClientService
import kotlinx.coroutines.Job

/**
 * FreeTurnX: декоратор над запуском VPN родителя. SOCKS5-клиент показывается на главном экране
 * как обычное подключение, поэтому:
 * - «Стоп» (экран, тайл, виджет, шторка) выключает SOCKS5-клиент, если работает он;
 * - запустить VPN, пока работает SOCKS5-клиент, нельзя - и наоборот (см. [Socks5ClientService]).
 */
class FreeTurnXLauncher(
    private val context: Context,
    private val parent: ProxyServiceLauncher,
) : ProxyServiceLauncher by parent {

    override fun start(): Job =
        if (Socks5ClientService.isActive) Job().apply { complete() } else parent.start()

    override fun stop(): Job {
        if (Socks5ClientService.isActive) {
            Socks5ClientService.stop(context)
            return Job().apply { complete() }
        }
        return parent.stop()
    }

    override fun restartIfRunning(): Job =
        if (Socks5ClientService.isActive) Job().apply { complete() } else parent.restartIfRunning()
}
