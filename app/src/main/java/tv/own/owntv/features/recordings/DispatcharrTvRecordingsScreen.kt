package tv.own.owntv.features.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.dispatcharr.DispatcharrApi
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.stage.StagePill
import tv.own.owntv.ui.stage.StagePopup
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageText
import java.text.DateFormat
import java.util.Date

/**
 * Server DVR via Dispatcharr, never SFTP and never the device's own storage.
 * The server itself streams completed recordings from its rclone-mounted folder.
 */
@Composable
fun DispatcharrTvRecordingsScreen(
    api: DispatcharrApi?,
    player: OwnTVPlayer,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var refresh by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<DispatcharrApi.ServerRecording>>(emptyList()) }
    var fetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var actionFor by remember { mutableStateOf<DispatcharrApi.ServerRecording?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(api, refresh) {
        error = false
        fetching = api != null
        records = emptyList()
        if (api != null) {
            runCatching { api.serverRecordings() }
                .onSuccess { records = it }
                .onFailure { error = true }
        }
        fetching = false
    }

    fun watch(record: DispatcharrApi.ServerRecording) {
        val client = api ?: return
        player.play(
            client.serverRecordingPlaybackUrl(record.id),
            title = record.title,
            isLive = false,
            httpHeaders = client.serverRecordingHeaders(),
        )
        onFullscreen()
    }

    val now = System.currentTimeMillis()
    val visible = if (completed) records.filter { it.isCompleted() }.sortedByDescending { it.startMs }
        else records.filter { it.isScheduled(now) || it.isActive(now) }.sortedBy { it.startMs }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.mpx),
        verticalArrangement = Arrangement.spacedBy(12.mpx),
    ) {
        Text(stringResource(R.string.recording_settings_group), style = stageText(30f, 700), color = StageColors.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(12.mpx)) {
            StagePill(
                text = stringResource(R.string.recording_group_scheduled),
                onClick = { completed = false },
                icon = OwnTVIcon.CALENDAR, highlighted = !completed,
            )
            StagePill(
                text = stringResource(R.string.recording_group_completed),
                onClick = { completed = true },
                icon = OwnTVIcon.PLAY, highlighted = completed,
            )
            StagePill(
                text = stringResource(R.string.dispatcharr_remote_refresh),
                onClick = { refresh++ },
                icon = OwnTVIcon.REFRESH,
            )
        }
        when {
            api == null -> Text(stringResource(R.string.dispatcharr_remote_unconfigured),
                style = stageText(19f, 500), color = StageColors.Muted)
            fetching -> Text(stringResource(R.string.dispatcharr_loading),
                style = stageText(19f, 500), color = StageColors.Muted)
            error -> Text(stringResource(R.string.dispatcharr_remote_failed),
                style = stageText(19f, 500), color = StageColors.Danger)
            visible.isEmpty() -> Text(stringResource(R.string.recording_empty),
                style = stageText(19f, 500), color = StageColors.Muted)
            else -> visible.forEach { record ->
                val live = record.isActive(System.currentTimeMillis())
                StagePill(
                    text = record.title,
                    small = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(record.startMs)),
                    onClick = {
                        if (completed && record.hasFile) watch(record)
                        else actionFor = record
                    },
                    icon = if (live) OwnTVIcon.REC else if (completed) OwnTVIcon.PLAY else OwnTVIcon.CALENDAR,
                )
            }
        }
    }

    actionFor?.let { rec ->
        StagePopup(
            onDismiss = { actionFor = null },
            title = rec.title,
        ) {
            if (rec.isCompleted()) {
                StagePill(
                    text = stringResource(R.string.content_downloads_play),
                    icon = OwnTVIcon.PLAY,
                    onClick = { actionFor = null; watch(rec) },
                )
            } else if (rec.isActive(System.currentTimeMillis()) || rec.isScheduled(System.currentTimeMillis())) {
                StagePill(
                    text = stringResource(if (rec.isActive(System.currentTimeMillis()))
                        R.string.recording_stop else R.string.common_cancel),
                    icon = OwnTVIcon.REC,
                    onClick = {
                        actionFor = null
                        scope.launch {
                            runCatching {
                                if (rec.isActive(System.currentTimeMillis())) api?.stopServerRecording(rec.id)
                                else api?.cancelServerRecording(rec.id)
                            }.onSuccess { refresh++ }.onFailure { error = true }
                        }
                    },
                )
            }
        }
    }
}
