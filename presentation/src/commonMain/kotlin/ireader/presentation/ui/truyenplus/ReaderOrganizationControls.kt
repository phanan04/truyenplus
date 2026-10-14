package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ireader.domain.models.library.LibrarySort
import ireader.i18n.asString
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

@Composable fun ReaderSearchScopePicker(shelf:Boolean,onScope:(Boolean)->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        for(isShelf in listOf(false,true))Surface(onClick={onScope(isShelf)},
            color=if(shelf==isShelf)MaterialTheme.colorScheme.onSurface.copy(alpha=.1f) else androidx.compose.ui.graphics.Color.Transparent,
            shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag(if(isShelf)"search-scope-shelf" else "search-scope-library")
                .semantics {selected=shelf==isShelf;role=Role.Tab}) {
            Box(Modifier.padding(8.dp),contentAlignment=Alignment.Center) {
                Text(s.localize(if(isShelf)Res.string.tp_shelf else Res.string.tp_library),style=MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable fun ReaderSortContent(sort:LibrarySort,onType:(LibrarySort.Type)->Unit,onDirection:(Boolean)->Unit,onDismiss:()->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).navigationBarsPadding()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(s.localize(Res.string.tp_sort_books),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton(onClick=onDismiss,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Close,s.localize(Res.string.close))}
        }
        LazyColumn(Modifier.heightIn(max=LocalReaderViewport.current.height*.65f),contentPadding=PaddingValues(bottom=12.dp)) {
            items(listOf(LibrarySort.Type.Title,LibrarySort.Type.LastRead,LibrarySort.Type.DateAdded,LibrarySort.Type.TotalChapters,LibrarySort.Type.Unread)) {type ->
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(sort.type==type,onClick={onType(type)},role=Role.RadioButton)
                    .testTag("sort-${type.name}"),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(sort.type==type,onClick=null)
                    Text(LibrarySort.Type.name(type).asString(s),Modifier.padding(start=8.dp),style=MaterialTheme.typography.bodyLarge)
                }
            }
            item {HorizontalDivider();Text(s.localize(Res.string.tp_sort_order),Modifier.padding(top=16.dp),style=MaterialTheme.typography.labelLarge)}
            items(listOf(true,false)) {ascending ->
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(sort.isAscending==ascending,onClick={onDirection(ascending)},role=Role.RadioButton)
                    .testTag(if(ascending)"sort-ascending" else "sort-descending"),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(sort.isAscending==ascending,onClick=null)
                    Text(s.localize(if(ascending)Res.string.ascending else Res.string.descending),Modifier.padding(start=8.dp))
                }
            }
        }
    }
}

/** Apple Books collection flow: the current scope is visible; choosing it is explicit. */
@Composable fun ReaderCollectionPicker(names:List<String>,selected:Int,onSelect:(Int)->Unit,onManage:()->Unit,manageLabel:String?=null) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var open by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    Box {
        TextButton(onClick={open=true},modifier=Modifier.heightIn(min=48.dp).testTag("library-collections")) {
            Text(names.getOrNull(selected).orEmpty(),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp));Icon(androidx.compose.material.icons.Icons.Default.ExpandMore,null,Modifier.size(18.dp))
        }
        DropdownMenu(open,onDismissRequest={open=false}) {
            names.forEachIndexed {i,name ->DropdownMenuItem(text={Text(name,maxLines=2)},
                leadingIcon={Icon(if(i==selected)androidx.compose.material.icons.Icons.Default.Check else androidx.compose.material.icons.Icons.Default.Folder,null)},
                modifier=Modifier.heightIn(min=56.dp).testTag("collection-choice-$i").semantics {this.selected=i==selected},
                onClick={open=false;onSelect(i)})}
            HorizontalDivider()
            DropdownMenuItem(text={Text(manageLabel ?: s.localize(Res.string.tp_manage_collections))},onClick={open=false;onManage()},modifier=Modifier.heightIn(min=56.dp))
        }
    }
}
