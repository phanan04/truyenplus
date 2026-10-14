package ireader.presentation.ui.truyenplus

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderUpdateSheet(version: String, endpoint: String, supported: Boolean, state: ReaderUpdateState,
    onCheck: (String) -> Unit, onDownload: () -> Unit, onInstall: () -> Unit, onCancel: () -> Unit,
    onDismiss: () -> Unit) {
    ReaderGlassSheet(onDismissRequest=onDismiss) {
        ReaderUpdateContent(version, endpoint, supported, state, onCheck, onDownload, onInstall, onCancel)
    }
}

@Composable
fun ReaderUpdateContent(version: String, endpoint: String, supported: Boolean, state: ReaderUpdateState,
    onCheck: (String) -> Unit, onDownload: () -> Unit, onInstall: () -> Unit, onCancel: () -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var address by rememberSaveable(endpoint) { mutableStateOf(endpoint) }
    var showAddress by rememberSaveable {mutableStateOf(endpoint.isBlank())}
    var showNotes by rememberSaveable(state.update?.release?.versionCode) {mutableStateOf(false)}
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            Text(strings.localize(Res.string.tp_app_update), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(strings.localize(Res.string.tp_version, version), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!supported) {
                Text(strings.localize(Res.string.tp_update_android))
            } else {
                when (state.stage) {
                    ReaderUpdateStage.Idle -> Text(if (endpoint.isBlank()) strings.localize(Res.string.tp_update_not_set) else strings.localize(Res.string.tp_update_check_hint))
                    ReaderUpdateStage.Checking -> { Text(strings.localize(Res.string.tp_checking_update)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    ReaderUpdateStage.Current -> Text(strings.localize(Res.string.tp_update_current))
                    ReaderUpdateStage.Available, ReaderUpdateStage.Downloading, ReaderUpdateStage.Ready -> {
                        state.update?.let { update ->
                            Text(strings.localize(Res.string.tp_new_version, update.release.versionName), style = MaterialTheme.typography.titleMedium)
                            Text("${update.asset.sizeBytes / 1_048_576} MB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (state.stage == ReaderUpdateStage.Downloading) {
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            Text(strings.localize(Res.string.tp_downloading, "${(state.progress * 100).toInt()}%"), style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.stage == ReaderUpdateStage.Ready) Text(strings.localize(Res.string.tp_update_ready))
                    }
                    ReaderUpdateStage.Error -> Unit
                }

                when (state.stage) {
                    ReaderUpdateStage.Checking, ReaderUpdateStage.Downloading -> OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(strings.localize(Res.string.tp_cancel)) }
                    ReaderUpdateStage.Available -> Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(strings.localize(Res.string.tp_download_update)) }
                    ReaderUpdateStage.Ready -> Button(onClick = onInstall, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(if (state.installing) strings.localize(Res.string.tp_checking_file) else strings.localize(Res.string.tp_install_update)) }
                    else -> Button(onClick = { onCheck(address) }, enabled = address.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(strings.localize(Res.string.tp_check_update)) }
                }
                state.message?.let { Text(localizeReaderUpdateMessage(it, strings), color = if (state.stage == ReaderUpdateStage.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                state.update?.release?.notes?.takeIf {it.isNotBlank()}?.let {notes ->
                    TextButton(onClick={showNotes=!showNotes}) {Text(strings.localize(Res.string.tp_update_notes))}
                    if(showNotes)Text(notes.take(6000),style=MaterialTheme.typography.bodyMedium)
                }
                if (showAddress) {
                    Text(strings.localize(Res.string.tp_update_address), style = MaterialTheme.typography.titleSmall)
                    Text(strings.localize(Res.string.tp_update_address_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(address, { address = it }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                        label = { Text(strings.localize(Res.string.tp_https_link)) }, singleLine = true)
                } else {
                    TextButton(onClick = { showAddress = true }, enabled = !state.busy) { Text(strings.localize(Res.string.tp_update_advanced)) }
                }
                if (showAddress && state.stage in listOf(ReaderUpdateStage.Available, ReaderUpdateStage.Ready, ReaderUpdateStage.Current)) {
                    TextButton(onClick = { onCheck(address) }, enabled = !state.busy && address.isNotBlank()) { Text(strings.localize(Res.string.tp_check_address)) }
                }
                Text(strings.localize(Res.string.tp_install_confirm_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
}
