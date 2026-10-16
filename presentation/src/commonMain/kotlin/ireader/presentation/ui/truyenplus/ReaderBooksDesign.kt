package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import ireader.i18n.localize
import ireader.i18n.resources.*

/** Geometry from the supplied 393pt Apple Books screens; accessibility is never scaled down. */
internal object ReaderBooksTokens {
    val bottomDockGap=12.dp
    val title:TextStyle @Composable get()=TextStyle(fontFamily=booksEditorialFamily(),fontSize=34.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.7).sp)
    val section:TextStyle @Composable get()=TextStyle(fontFamily=booksEditorialFamily(),fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.3).sp)
    val gutter:Dp @Composable get()=if(LocalReaderViewport.current.width<360.dp)20.dp else 32.dp
    val rowShape=RoundedCornerShape(18.dp)
}

@Composable private fun booksEditorialFamily():FontFamily {
    val face=Font(Res.font.books_editorial_bold,FontWeight.Bold)
    return remember(face) {FontFamily(face)}
}

@Composable internal fun ReaderBooksHeader(title:String,onSettings:(()->Unit)?=null,onBack:(()->Unit)?=null,
    horizontalPadding:Dp=ReaderBooksTokens.gutter,actions:@Composable RowScope.()->Unit={}) {
    Column(Modifier.fillMaxWidth().padding(horizontal=horizontalPadding).padding(top=34.dp,bottom=12.dp)) {
        if(LocalReaderViewport.current.compactControls) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                if(onBack!=null)ReaderBooksBack(onBack)
                Spacer(Modifier.weight(1f));actions();if(onSettings!=null)ReaderBooksAccount(onSettings)
            }
            Text(title,style=ReaderBooksTokens.title,modifier=Modifier.padding(top=8.dp).testTag("books-page-title").semantics {heading()})
        } else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            if(onBack!=null)ReaderBooksBack(onBack)
            Text(title,style=ReaderBooksTokens.title,modifier=Modifier.weight(1f).testTag("books-page-title").semantics {heading()})
            actions();if(onSettings!=null)ReaderBooksAccount(onSettings)
        }
    }
}
@Composable private fun ReaderBooksAccount(onClick:()->Unit) {
    IconButton(onClick,modifier=Modifier.size(48.dp).testTag("books-account")) {
        ReaderBooksAccountSymbol(localize(Res.string.tp_settings))
    }
}
@Composable private fun ReaderBooksBack(onClick:()->Unit) {
    IconButton(onClick,modifier=Modifier.size(48.dp).testTag("books-back")) {Icon(Icons.Outlined.ChevronLeft,localize(Res.string.tp_back))}
}

@Composable internal fun ReaderBooksSection(title:String,onMore:(()->Unit)?=null,subtitle:String="",topPadding:Dp=22.dp,bottomPadding:Dp=20.dp) {
    Column(Modifier.fillMaxWidth().padding(horizontal=ReaderBooksTokens.gutter).padding(top=topPadding,bottom=bottomPadding)) {
        val action=if(onMore!=null)Modifier.heightIn(min=48.dp).clickable(onClick=onMore) else Modifier
        Row(Modifier.fillMaxWidth().then(action),verticalAlignment=Alignment.CenterVertically) {
            Text(title,style=ReaderBooksTokens.section,modifier=Modifier.weight(1f,fill=false).semantics {heading()})
            if(onMore!=null)Icon(Icons.Outlined.ChevronRight,localize(Res.string.tp_see_all),Modifier.padding(start=4.dp).size(22.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f))
        }
        if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
    }
}

@Composable internal fun ReaderBooksSearchField(query:String,onQuery:(String)->Unit,placeholder:String,
    focus:FocusRequester=remember {FocusRequester()},onSubmit:(String)->Unit={},onFocus:()->Unit={},dock:Boolean=false,shelf:Boolean?=null,onScope:((Boolean)->Unit)?=null) {
    val keyboard=LocalSoftwareKeyboardController.current
    TextField(query,onQuery,singleLine=true,shape=CircleShape,
        modifier=Modifier.fillMaxWidth().heightIn(min=if(dock)48.dp else 52.dp).then(if(dock)Modifier else Modifier.readerOptionalGlass(26)).focusRequester(focus).onFocusChanged {if(it.isFocused)onFocus()}.testTag("books-search-field"),
        placeholder={Text(placeholder,maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={if(shelf!=null && onScope!=null)ReaderSearchScopePicker(shelf,onScope) else Icon(Icons.Outlined.Search,null)},
        trailingIcon={if(query.isNotEmpty())IconButton({onQuery("")}) {Icon(Icons.Outlined.Cancel,localize(Res.string.clear))}},
        keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions=androidx.compose.foundation.text.KeyboardActions(onSearch={onSubmit(query);keyboard?.hide()}),
        colors=TextFieldDefaults.colors(focusedContainerColor=if(dock)Color.Transparent else readerOptionalGlassColor(MaterialTheme.colorScheme.onSurface.copy(alpha=.07f)),unfocusedContainerColor=if(dock)Color.Transparent else readerOptionalGlassColor(MaterialTheme.colorScheme.onSurface.copy(alpha=.06f)),
            focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
}

@Composable internal fun ReaderBooksActionRow(title:String,subtitle:String="",icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    Row(modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick).heightIn(min=60.dp).padding(horizontal=16.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Icon(icon,null,Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.bodyLarge)
            if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))
        }
        Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Search is its own bottom-reachable workspace, matching screenshots 74–83. */
@Composable internal fun ReaderSearchDock(query:String,onQuery:(String)->Unit,onReturn:()->Unit,
    focus:FocusRequester=remember {FocusRequester()},onSubmit:(String)->Unit={},onFocus:()->Unit={},onCloseBook:(()->Unit)?=null,origin:ReaderDestination=ReaderDestination.Home,shelf:Boolean?=null,onScope:((Boolean)->Unit)?=null) {
    Row(Modifier.widthIn(max=760.dp).fillMaxWidth().padding(start=28.dp,end=28.dp,top=10.dp,bottom=ReaderBooksTokens.bottomDockGap).imePadding(),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(shape=CircleShape,color=Color.Transparent,modifier=Modifier.size(48.dp).readerPanel(24)) {
            IconButton(onReturn,Modifier.fillMaxSize().testTag("search-return")) {Icon(when(origin){ReaderDestination.Library->Icons.Outlined.LibraryBooks;ReaderDestination.Shelf->Icons.Outlined.CloudQueue;else->Icons.Outlined.Home},localize(Res.string.tp_back))}
        }
        Surface(shape=CircleShape,color=Color.Transparent,modifier=Modifier.weight(1f).readerPanel(28)) {
            ReaderBooksSearchField(query,onQuery,localize(if(shelf==true)Res.string.tp_shelf else Res.string.tp_library),focus,onSubmit,onFocus,dock=true,shelf=shelf,onScope=onScope)
        }
        if(onCloseBook!=null)Surface(shape=CircleShape,color=Color.Transparent,modifier=Modifier.size(48.dp).readerPanel(24)) {
            IconButton(onCloseBook,Modifier.fillMaxSize().testTag("book-context-close")) {Icon(Icons.Outlined.Close,localize(Res.string.close))}
        }
    }
}

@Composable internal fun ReaderBooksSheetHeader(title:String,onClose:()->Unit,onBack:(()->Unit)?=null,
    closeTag:String="books-sheet-close",closeModifier:Modifier=Modifier,closeEnabled:Boolean=true,actions:@Composable RowScope.()->Unit={}) {
    Row(Modifier.fillMaxWidth().heightIn(min=60.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null)IconButton(onBack,Modifier.size(48.dp)) {Icon(Icons.Outlined.ChevronLeft,localize(Res.string.tp_back))}
        else Spacer(Modifier.width(48.dp))
        Text(title,style=MaterialTheme.typography.titleMedium.copy(fontSize=17.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold,letterSpacing=0.sp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,
            modifier=Modifier.weight(1f).semantics {heading()},maxLines=2,overflow=TextOverflow.Ellipsis)
        actions()
        IconButton(onClose,Modifier.size(48.dp).then(closeModifier).testTag(closeTag),enabled=closeEnabled) {Icon(Icons.Outlined.Close,localize(Res.string.close))}
    }
}
