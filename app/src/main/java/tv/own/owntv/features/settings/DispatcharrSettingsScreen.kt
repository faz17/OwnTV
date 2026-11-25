package tv.own.owntv.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import tv.own.owntv.core.dispatcharr.DispatcharrApi
import java.util.concurrent.TimeUnit
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVIcon

/**
 * Dedicated TV-remote-friendly configuration in Settings. The API key is entered
 * on-device, never passed through a GitHub secret, playlist URL or crash report.
 */
@Composable
fun DispatcharrSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { DispatcharrConnectionStore.get(context) }
    val saved by store.connection.collectAsStateWithLifecycle()
    var seeded by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var saveFailed by remember { mutableStateOf(false) }
    var savedOk by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<Unit>?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build() }
    val rowsFocus = remember { FocusRequester() }

    LaunchedEffect(saved) {
        if (!seeded) {
            url = saved?.origin.orEmpty()
            apiKey = saved?.apiKey.orEmpty()
            seeded = true
        }
    }
    LaunchedEffect(Unit) { runCatching { rowsFocus.requestFocus() } }

    StageFullPage(
        parents = listOf(stringResource(R.string.settings_group_network)),
        title = stringResource(R.string.dispatcharr_settings_title),
        count = pluralStringResource(R.plurals.settings_setting_count, 5, 5),
        onBack = onBack,
        modifier = modifier,
        rowsFocus = rowsFocus,
    ) {
        StageFieldRow(
            icon = OwnTVIcon.NETWORK,
            label = stringResource(R.string.dispatcharr_server_url),
            value = url,
            onValueChange = { url = it; savedOk = false },
            placeholder = stringResource(R.string.dispatcharr_url_hint),
        )
        StageFieldRow(
            icon = OwnTVIcon.PENCIL,
            label = stringResource(R.string.dispatcharr_api_key),
            value = apiKey,
            onValueChange = { apiKey = it; savedOk = false },
            password = true,
        )
        StageSettingRow(
            icon = OwnTVIcon.REFRESH,
            title = stringResource(R.string.dispatcharr_save),
            desc = when {
                saveFailed -> stringResource(R.string.dispatcharr_invalid_config)
                savedOk -> stringResource(R.string.dispatcharr_config_saved)
                else -> stringResource(R.string.dispatcharr_config_hint)
            },
            value = null,
            onClick = {
                saveFailed = runCatching { store.save(url, apiKey) }.isFailure
                testResult = null
                savedOk = !saveFailed
            },
        )
        StageSettingRow(
            icon = OwnTVIcon.NETWORK,
            title = stringResource(R.string.dispatcharr_test),
            desc = when {
                testing -> stringResource(R.string.dispatcharr_testing)
                testResult?.isSuccess == true -> stringResource(R.string.dispatcharr_test_success)
                testResult?.isFailure == true -> stringResource(R.string.dispatcharr_test_failed)
                else -> stringResource(R.string.dispatcharr_test_hint)
            },
            value = null,
            onClick = {
                val connection = saved
                if (connection != null && !testing) {
                    testing = true
                    testResult = null
                    scope.launch {
                        testResult = runCatching {
                            DispatcharrApi(client, connection.origin, connection.apiKey).verifyConnection()
                        }
                        testing = false
                    }
                }
            },
        )
        StageSettingRow(
            icon = OwnTVIcon.NETWORK,
            title = stringResource(R.string.dispatcharr_remove),
            desc = if (saved == null) stringResource(R.string.dispatcharr_not_configured)
                else stringResource(R.string.dispatcharr_configured),
            value = null,
            onClick = {
                store.clear()
                apiKey = ""
                url = ""
                savedOk = false
                saveFailed = false
            },
        )
    }
}
