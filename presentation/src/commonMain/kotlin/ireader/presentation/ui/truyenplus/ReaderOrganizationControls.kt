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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ireader.domain.models.library.LibrarySort
import ireader.i18n.asString
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

/** Scope is an app-specific extension of the reference search glyph, not another page toolbar. */
@Composable fun ReaderSearchScopePicker(shelf:Boolean,onScope:(Boolean)->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var open by remember {mutableStateOf(false)}
    Box {
        IconButton(onClick={open=true},modifier=Modifier.size(48.dp).testTag("search-scope-picker")) {
            Box(Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Search,s.localize(if(shelf)Res.string.tp_shelf else Res.string.tp_library),Modifier.size(24.dp).align(Alignment.TopStart))
                Icon(Icons.Default.ArrowDropDown,null,Modifier.size(12.dp).align(Alignment.BottomEnd))
            }
        }
        ReaderGlassDropdownMenu(open,{open=false}) {
            for(isShelf in listOf(false,true))DropdownMenuItem(
                text={Text(s.localize(if(isShelf)Res.string.tp_shelf else Res.string.tp_library))},
                modifier=Modifier.heightIn(min=56.dp).testTag(if(isShelf)"search-scope-shelf" else "search-scope-library").semantics {selected=shelf==isShelf;role=Role.RadioButton},
                leadingIcon={if(shelf==isShelf)Icon(Icons.Default.Check,null) else Spacer(Modifier.size(24.dp))},
                onClick={open=false;onScope(isShelf)})
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
@Composable fun ReaderCollectionPicker(names:List<String>,selected:Int,onSelect:(Int)->Unit,onManage:()->Unit,manageLabel:String?=null,iconOnly:Boolean=false) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var open by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    Box {
        TextButton(onClick={open=true},modifier=(if(iconOnly)Modifier.size(48.dp).shadow(4.dp,CircleShape).readerOptionalGlass(24).background(readerOptionalGlassColor(MaterialTheme.colorScheme.surface),CircleShape) else Modifier.heightIn(min=48.dp)).testTag("library-collections")) {
            if(iconOnly)Icon(Icons.Default.Sort,s.localize(Res.string.tp_collections),Modifier.size(24.dp))
            else {
                Text(names.getOrNull(selected).orEmpty(),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp));Icon(androidx.compose.material.icons.Icons.Default.ExpandMore,null,Modifier.size(18.dp))
            }
        }
        if(open)ReaderGlassSheet(onDismissRequest={open=false}) {
            Column(Modifier.fillMaxWidth().testTag("books-collection-sheet")) {
                ReaderBooksSheetHeader(manageLabel ?: s.localize(Res.string.tp_collections),{open=false},actions={
                    TextButton(onClick={open=false;onManage()},modifier=Modifier.heightIn(min=48.dp)) {Text(s.localize(Res.string.edit))}
                })
                LazyColumn(Modifier.fillMaxWidth().heightIn(max=LocalReaderViewport.current.height*.65f),
                    contentPadding=PaddingValues(horizontal=20.dp,vertical=12.dp)) {
                    itemsIndexed(names) {i,name->
                        Surface(modifier=Modifier.readerOptionalGlass(12),color=readerOptionalGlassColor(MaterialTheme.colorScheme.surface),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(i==selected,onClick={open=false;onSelect(i)},role=Role.RadioButton)
                                .testTag("collection-choice-$i").padding(horizontal=16.dp,vertical=12.dp),
                                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                                Icon(androidx.compose.material.icons.Icons.Default.Folder,null,Modifier.size(22.dp))
                                Text(name,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge,maxLines=2)
                                if(i==selected)Icon(androidx.compose.material.icons.Icons.Default.Check,null,Modifier.size(22.dp))
                            }
                        }
                        if(i<names.lastIndex)HorizontalDivider(Modifier.padding(start=56.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.08f))
                    }
                    item {
                        TextButton(onClick={open=false;onManage()},modifier=Modifier.fillMaxWidth().padding(top=20.dp).heightIn(min=56.dp)) {
                            Text(manageLabel ?: s.localize(Res.string.tp_manage_collections))
                        }
                    }
                }
            }
        }
    }
}
