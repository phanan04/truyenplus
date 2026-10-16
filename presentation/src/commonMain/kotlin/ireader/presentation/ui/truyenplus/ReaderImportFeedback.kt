package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassOutlinedButton as OutlinedButton
import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

@Composable fun ReaderImportFeedback(state:ReaderImportState,onRetry:()->Unit,onClear:()->Unit) {
    if(state.files.isEmpty())return
    val s=requireNotNull(LocalLocalizeHelper.current)
    var results by remember {mutableStateOf(false)}
    val label=if(state.busy)s.localize(Res.string.tp_import_in_background,state.files.size)
        else s.localize(Res.string.tp_import_complete,state.added,state.failed)
    Surface(onClick={results=true},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp).readerPanel(18),
        shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),color=Color.Transparent) {
        Row(Modifier.padding(start=14.dp,end=4.dp).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(state.busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)
            else Icon(if(state.failed>0)Icons.Default.ErrorOutline else Icons.Default.CheckCircle,null,Modifier.size(20.dp))
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
            if(!state.busy)IconButton(onClick=onClear,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Close,s.localize(Res.string.close))}
        }
    }
    if(results)ReaderGlassSheet(onDismissRequest={results=false}) {
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).navigationBarsPadding()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(s.localize(Res.string.tp_import_results),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                IconButton(onClick={results=false}){Icon(Icons.Default.Close,s.localize(Res.string.close))}
            }
            Text(label,style=MaterialTheme.typography.bodyMedium)
            LazyColumn(Modifier.heightIn(max=LocalReaderViewport.current.height*.45f),contentPadding=PaddingValues(vertical=12.dp)) {
                itemsIndexed(state.files,key={_,file->file.id}) {index,file ->
                    ListItem(headlineContent={Text(file.name ?: (s.localize(Res.string.tp_import_file_label,index+1)+" · "+file.format.name.uppercase()))},
                        supportingContent={Text(s.localize(when(file.phase) {
                            ReaderImportPhase.Added->Res.string.tp_edit_saved
                            ReaderImportPhase.Failed->Res.string.tp_import_failed
                            else->Res.string.tp_shelf_importing
                        }))},colors=ListItemDefaults.colors(containerColor=Color.Transparent))
                }
            }
            if(state.failed>0)OutlinedButton(onClick=onRetry,enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Text(s.localize(Res.string.tp_import_retry))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
