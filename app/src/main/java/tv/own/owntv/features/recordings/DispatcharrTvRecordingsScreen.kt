package tv.own.owntv.features.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.dispatcharr.DispatcharrApi
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.stage.StagePill
import tv.own.owntv.ui.stage.StagePoster
import tv.own.owntv.ui.stage.StagePopup
import tv.own.owntv.ui.stage.StageSurface
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageText
import java.text.DateFormat
import java.util.Date

/**
 * A single Dispatcharr DVR library shared across OwnTV profiles. Only presentation
 * (list/grid) is stored locally; the recordings remain owned by the server.
 * Both layouts use D-pad focus controls, rather than touch gestures.
 */
@Composable
fun DispatcharrTvRecordingsScreen(
    api: DispatcharrApi?,
    player: OwnTVPlayer,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) {
        context.getSharedPreferences("owntv_dispatcharr_recordings_ui", android.content.Context.MODE_PRIVATE)
    }
    var grid by remember(preferences) { mutableStateOf(preferences.getBoolean("grid", false)) }
    fun selectGrid(value: Boolean) {
        grid = value
        preferences.edit().putBoolean("grid", value).apply()
    }

    var refresh by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<DispatcharrApi.ServerRecording>>(emptyList()) }
    var fetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
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
        if (!record.hasFile) return
        player.play(
            client.serverRecordingPlaybackUrl(record.id),
            title = record.title,
            isLive = false,
            httpHeaders = client.serverRecordingHeaders(),
        )
        onFullscreen()
    }

    val now = System.currentTimeMillis()
    val completed = records.filter { it.isCompleted() }.sortedByDescending { it.startMs }
    val scheduled = records.filter { it.isScheduled(now) || it.isActive(now) }.sortedBy { it.startMs }
    val completedLabel = stringResource(R.string.recording_group_completed)
    val scheduledLabel = stringResource(R.string.recording_group_scheduled)

    Column(modifier.fillMaxSize().padding(horizontal = 28.mpx, vertical = 18.mpx)) {
        Text(
            stringResource(R.string.recording_settings_group),
            style = stageText(30f, 700),
            color = StageColors.Text,
            modifier = Modifier.padding(bottom = 14.mpx),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.mpx),
            modifier = Modifier.padding(bottom = 16.mpx),
        ) {
            StagePill(
                text = stringResource(R.string.dispatcharr_view_list),
                icon = OwnTVIcon.LIST,
                highlighted = !grid,
                onClick = { selectGrid(false) },
            )
            StagePill(
                text = stringResource(R.string.dispatcharr_view_grid),
                icon = OwnTVIcon.GRID,
                highlighted = grid,
                onClick = { selectGrid(true) },
            )
            StagePill(
                text = stringResource(R.string.dispatcharr_remote_refresh),
                icon = OwnTVIcon.REFRESH,
                onClick = { refresh++ },
            )
        }

        when {
            api == null -> TvDvrMessage(stringResource(R.string.dispatcharr_remote_unconfigured))
            fetching -> TvDvrMessage(stringResource(R.string.dispatcharr_loading))
            error -> TvDvrMessage(stringResource(R.string.dispatcharr_remote_failed), danger = true)
            grid -> LazyVerticalGrid(
                columns = GridCells.Adaptive(215.mpx),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 6.mpx, end = 8.mpx, top = 12.mpx, bottom = 24.mpx),
                horizontalArrangement = Arrangement.spacedBy(20.mpx),
                verticalArrangement = Arrangement.spacedBy(26.mpx),
            ) {
                item(key = -1L, span = { GridItemSpan(maxLineSpan) }) {
                    TvDvrHeading(completedLabel)
                }
                if (completed.isEmpty()) item(key = -2L, span = { GridItemSpan(maxLineSpan) }) {
                    TvDvrMessage(stringResource(R.string.dispatcharr_remote_empty_completed))
                }
                gridItems(completed, key = { it.id }) { record ->
                    TvDvrPoster(record, onClick = {
                        if (record.hasFile) watch(record) else actionFor = record
                    }, onMenu = { actionFor = record })
                }
                item(key = -3L, span = { GridItemSpan(maxLineSpan) }) {
                    TvDvrHeading(scheduledLabel)
                }
                if (scheduled.isEmpty()) item(key = -4L, span = { GridItemSpan(maxLineSpan) }) {
                    TvDvrMessage(stringResource(R.string.dispatcharr_remote_empty_scheduled))
                }
                gridItems(scheduled, key = { it.id }) { record ->
                    TvDvrPoster(record, onClick = { actionFor = record }, onMenu = { actionFor = record })
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 6.mpx, end = 14.mpx, top = 12.mpx, bottom = 24.mpx),
                verticalArrangement = Arrangement.spacedBy(12.mpx),
            ) {
                item(key = -1L) { TvDvrHeading(completedLabel) }
                if (completed.isEmpty()) item(key = -2L) {
                    TvDvrMessage(stringResource(R.string.dispatcharr_remote_empty_completed))
                }
                items(completed, key = { it.id }) { record ->
                    TvDvrListRow(record, onClick = {
                        if (record.hasFile) watch(record) else actionFor = record
                    }, onMenu = { actionFor = record })
                }
                item(key = -3L) { TvDvrHeading(scheduledLabel) }
                if (scheduled.isEmpty()) item(key = -4L) {
                    TvDvrMessage(stringResource(R.string.dispatcharr_remote_empty_scheduled))
                }
                items(scheduled, key = { it.id }) { record ->
                    TvDvrListRow(record, onClick = { actionFor = record }, onMenu = { actionFor = record })
                }
            }
        }
    }

    actionFor?.let { rec ->
        StagePopup(onDismiss = { actionFor = null }, title = rec.title) {
            if (rec.isCompleted()) {
                StagePill(
                    text = stringResource(R.string.content_downloads_play),
                    icon = OwnTVIcon.PLAY,
                    onClick = { actionFor = null; watch(rec) },
                )
            } else if (rec.isActive(System.currentTimeMillis()) || rec.isScheduled(System.currentTimeMillis())) {
                StagePill(
                    text = stringResource(
                        if (rec.isActive(System.currentTimeMillis()))
                            R.string.recording_stop else R.string.common_cancel,
                    ),
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

@Composable
private fun TvDvrHeading(title: String) {
    Text(title, style = stageText(23f, 700), color = StageColors.Text,
        modifier = Modifier.padding(top = 13.mpx, bottom = 5.mpx))
}

@Composable
private fun TvDvrMessage(message: String, danger: Boolean = false) {
    Text(message, style = stageText(19f, 500),
        color = if (danger) StageColors.Danger else StageColors.Muted,
        modifier = Modifier.padding(vertical = 10.mpx))
}

@Composable
private fun TvDvrArtwork(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(12.mpx)).background(Color(0xFF25313A)),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            OwnTVIcon(OwnTVIcon.REC, StageColors.Muted, Modifier.size(46.mpx))
        }
    }
}

@Composable
private fun TvDvrPoster(
    record: DispatcharrApi.ServerRecording,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    StagePoster(
        title = record.title,
        line = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(record.startMs)),
        width = 184.mpx,
        height = 252.mpx,
        compact = true,
        onClick = onClick,
        onLongClick = onMenu,
    ) {
        TvDvrArtwork(record.posterUrl, Modifier.fillMaxSize())
    }
}

@Composable
private fun TvDvrListRow(
    record: DispatcharrApi.ServerRecording,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    StageSurface(
        onClick = onClick,
        onLongClick = onMenu,
        radius = 16.mpx,
        modifier = Modifier.fillMaxWidth().height(112.mpx),
        idle = Modifier.background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.mpx)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.mpx),
            modifier = Modifier.fillMaxSize().padding(horizontal = 13.mpx, vertical = 10.mpx),
        ) {
            TvDvrArtwork(record.posterUrl, Modifier.width(68.mpx).height(90.mpx))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.mpx)) {
                Text(record.title, style = stageText(21f, 700), color = StageColors.Text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(record.startMs)),
                    style = stageText(17f, 500), color = StageColors.Muted,
                )
            }
            OwnTVIcon(if (record.hasFile) OwnTVIcon.PLAY else OwnTVIcon.CALENDAR,
                StageColors.Muted, Modifier.size(25.mpx))
        }
    }
}
