package com.freeturn.app.freeturnx.socksclient

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freeturn.app.R
import com.freeturn.app.ui.components.SettingsCard
import com.freeturn.app.ui.components.SettingsEntryRow
import com.freeturn.app.ui.theme.Spacing

/** FreeTurnX: строка «SOCKS5-клиент» с окном подключения к чужому SOCKS5 (например, SOCKS5 Hotspot). */
@Composable
fun Socks5ClientCard() {
    val state by Socks5ClientService.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // Запрос разрешения VPN - здесь, а не в окне: окно закрывается раньше, чем придёт ответ.
    val vpnPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) Socks5ClientService.start(context)
    }
    SettingsCard {
        SettingsEntryRow(
            iconRes = R.drawable.wifi_tethering_24px,
            title = stringResource(R.string.fx_socks_client),
            subtitle = when (val s = state) {
                is Socks5ClientService.State.Connected -> stringResource(R.string.fx_socks_client_connected, s.proxy)
                Socks5ClientService.State.Connecting -> stringResource(R.string.fx_socks_client_connecting)
                is Socks5ClientService.State.Failed -> stringResource(R.string.fx_socks_client_failed, s.reason)
                Socks5ClientService.State.Idle -> stringResource(R.string.fx_socks_client_desc)
            },
            onClick = { dialog = true }
        )
    }
    if (dialog) Socks5ClientDialog(
        state,
        onConnect = {
            val prepare = VpnService.prepare(context)
            if (prepare != null) vpnPermission.launch(prepare) else Socks5ClientService.start(context)
        },
        onDismiss = { dialog = false }
    )
}

@Composable
private fun Socks5ClientDialog(state: Socks5ClientService.State, onConnect: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { Socks5ClientConfig.load(context) }
    var host by rememberSaveable { mutableStateOf(saved.host) }
    var port by rememberSaveable { mutableStateOf(saved.port.toString()) }
    var user by rememberSaveable { mutableStateOf(saved.user) }
    var pass by rememberSaveable { mutableStateOf(saved.pass) }
    val active = state is Socks5ClientService.State.Connected || state is Socks5ClientService.State.Connecting
    val portNumber = port.toIntOrNull()?.takeIf { it in 1..65535 }

    fun connect() {
        Socks5ClientConfig(host.trim(), portNumber ?: return, user.trim(), pass).save(context)
        onConnect()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fx_socks_client)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(R.string.fx_socks_client_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = host, onValueChange = { host = it }, enabled = !active, singleLine = true,
                    label = { Text(stringResource(R.string.fx_socks_client_host)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, enabled = !active,
                    singleLine = true, isError = portNumber == null,
                    label = { Text(stringResource(R.string.fx_socks_client_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = user, onValueChange = { user = it }, enabled = !active, singleLine = true,
                    label = { Text(stringResource(R.string.fx_socks_client_user)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it }, enabled = !active, singleLine = true,
                    label = { Text(stringResource(R.string.fx_socks_client_pass)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (active) {
                TextButton(onClick = { Socks5ClientService.stop(context); onDismiss() }) {
                    Text(stringResource(R.string.fx_socks_client_disconnect))
                }
            } else {
                TextButton(onClick = ::connect, enabled = host.isNotBlank() && portNumber != null) {
                    Text(stringResource(R.string.fx_socks_client_connect))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
