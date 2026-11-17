package com.freeturn.app.freeturnx

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * FreeTurnX: разрешение на локальную сеть (Android 17+). Без него система молча отбрасывает
 * обмен приложения с устройствами LAN - SOCKS5 Hotspot «висит», SOCKS5-клиент не достаёт до прокси.
 * Возвращает запрос: [then] вызывается сразу, если разрешение есть или не нужно, иначе - после ответа.
 */
@Composable
fun rememberLocalNetworkRequest(): (then: () -> Unit) -> Unit {
    val context = LocalContext.current
    // Действие после ответа живёт дольше рекомпозиций, пока открыт системный диалог.
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending[0]?.invoke()
        pending[0] = null
    }
    val current = rememberUpdatedState(launcher)
    return { then ->
        val needed = Build.VERSION.SDK_INT >= LOCAL_NETWORK_SDK &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED
        if (needed) {
            pending[0] = then
            current.value.launch(PERMISSION)
        } else {
            then()
        }
    }
}

private const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
private const val LOCAL_NETWORK_SDK = 37
