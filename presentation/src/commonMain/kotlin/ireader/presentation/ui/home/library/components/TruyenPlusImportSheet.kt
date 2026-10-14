package ireader.presentation.ui.home.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ireader.i18n.localize
import ireader.i18n.resources.*
import ireader.presentation.ui.truyenplus.ReaderGlassSheet
import ireader.presentation.ui.truyenplus.readerAppGlassCard

@Composable fun TruyenPlusImportSheet(onImportEpub:()->Unit,onImportPdf:()->Unit,onDismiss:()->Unit) {
    ReaderGlassSheet(onDismissRequest=onDismiss) {
        TruyenPlusImportContent({onDismiss();onImportEpub()},{onDismiss();onImportPdf()},onDismiss)
    }
}
@Composable fun TruyenPlusImportContent(onImportEpub:()->Unit,onImportPdf:()->Unit,onDismiss:()->Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(localize(Res.string.truyenplus_import_title),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton(onDismiss,Modifier.size(48.dp)){Icon(Icons.Default.Close,localize(Res.string.close))}
        }
        Text(localize(Res.string.tp_import_choose_format),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        for(epub in listOf(true,false))Surface(onClick=if(epub)onImportEpub else onImportPdf,
            color=androidx.compose.ui.graphics.Color.Transparent,shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            modifier=Modifier.fillMaxWidth().heightIn(min=80.dp).readerAppGlassCard(20).testTag(if(epub)"import-epub" else "import-pdf")) {
            Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Icon(if(epub)Icons.Outlined.MenuBook else Icons.Outlined.PictureAsPdf,null,Modifier.size(28.dp))
                Text(if(epub)"EPUB" else "PDF",style=MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
