package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ireader.domain.services.tts_service.v2.EdgeVoice
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

@Composable
fun ReaderVoiceChoiceDialog(edgeSupported: Boolean, selected: EdgeVoice?, onEdge: (EdgeVoice) -> Unit,
    onSystem: () -> Unit, onDismiss: () -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(strings.localize(Res.string.tp_choose_voice)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (edgeSupported) {
                    for (voice in EdgeVoice.entries) {
                        VoiceChoice(strings.localize(if (voice == EdgeVoice.HoaiMy) Res.string.tp_edge_hoaimy else Res.string.tp_edge_namminh),
                            "Edge TTS · " + strings.localize(Res.string.tp_edge_online), selected == voice) { onEdge(voice) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
                VoiceChoice(strings.localize(Res.string.tp_system_voice), "", selected == null, onSystem)
            }
        }, confirmButton = {})
}

@Composable private fun VoiceChoice(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(selected, role = Role.RadioButton, onClick = onClick)
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
