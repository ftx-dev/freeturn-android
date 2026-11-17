@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.freeturn.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freeturn.app.R
import com.freeturn.app.freeturnx.socksclient.Socks5ClientCard
import com.freeturn.app.ui.components.SettingsBackButton
import com.freeturn.app.ui.components.SettingsContentMaxWidth
import com.freeturn.app.ui.components.SettingsEntryRow
import com.freeturn.app.ui.components.SettingsGroup
import com.freeturn.app.ui.components.SettingsGroupItem
import com.freeturn.app.ui.theme.Spacing
import com.freeturn.app.viewmodel.settings.SettingsViewModel

// FreeTurnX: собственные функции форка собраны здесь, отдельно от экранов родителя,
// чтобы его обновления вливались без конфликтов.

/** Пункт «FreeTurnX: Фичи» в корне настроек. */
@Composable
fun FreeTurnXFeaturesEntry(onClick: () -> Unit) {
    SettingsGroup {
        SettingsGroupItem(0, 1) {
            SettingsEntryRow(
                iconRes = R.drawable.ic_freeturn,
                title = stringResource(R.string.freeturnx_features),
                subtitle = stringResource(R.string.freeturnx_features_desc),
                onClick = onClick
            )
        }
    }
}

/** Экран «FreeTurnX: Фичи». */
@Composable
fun FreeTurnXFeaturesScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val shizukuTetherEnabled by settingsViewModel.shizukuTetherEnabled.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.freeturnx_features)) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SettingsContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                ShizukuTetherCard(
                    enabled = shizukuTetherEnabled,
                    onEnabledChange = { settingsViewModel.setShizukuTetherEnabled(it) },
                )
                Socks5ClientCard()
            }
        }
    }
}
