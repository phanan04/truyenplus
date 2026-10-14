package ireader.presentation.ui.home.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ireader.presentation.ui.truyenplus.readerAppGlassCard
import ireader.i18n.localize
import ireader.i18n.resources.*

/** Presentation only: each action opens IReader's existing platform file picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruyenPlusImportSheet(
    onImportEpub: () -> Unit,
    onImportPdf: () -> Unit,
    onDismiss: () -> Unit,
) {
    ireader.presentation.ui.truyenplus.ReaderGlassSheet(onDismissRequest=onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(localize(Res.string.truyenplus_import_title), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.readerAppGlassCard(20),
                color = androidx.compose.ui.graphics.Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(18.dp).size(42.dp))
                    }
                    Text(localize(Res.string.truyenplus_select_device), fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    Text(localize(Res.string.truyenplus_supported_formats),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onDismiss(); onImportEpub() }, shape = RoundedCornerShape(14.dp)) {
                        Text(localize(Res.string.import_epub), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onDismiss(); onImportEpub() }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp)) {
                    Icon(Icons.Outlined.MenuBook, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("EPUB")
                }
                OutlinedButton(onClick = { onDismiss(); onImportPdf() }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp)) {
                    Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("PDF")
                }
            }
            Text(localize(Res.string.truyenplus_import_description), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}
