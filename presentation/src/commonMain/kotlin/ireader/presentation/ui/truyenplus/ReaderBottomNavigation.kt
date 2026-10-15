package ireader.presentation.ui.truyenplus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

/** Three destinations plus an independent search action, with a moving capsule selection. */
@Composable
fun ReaderBottomNav(selected: ReaderDestination, onSelect: (ReaderDestination)->Unit, onSearch: ()->Unit) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    val colors=MaterialTheme.colorScheme
    val compact=LocalReaderViewport.current.width.value/LocalReaderViewport.current.fontScale<340f
    val tabHeight=56.dp
    Row(Modifier.widthIn(max=600.dp).fillMaxWidth().padding(start=20.dp,end=20.dp,top=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.weight(1f).shadow(4.dp,CircleShape).clip(CircleShape)
            .readerNavigationGlass(colors.surface).border(1.dp,colors.onSurface.copy(alpha=.09f),CircleShape).padding(4.dp).selectableGroup()) {
            val width=maxWidth/ReaderAppScope.destinations.size
            val index=ReaderAppScope.destinations.indexOf(selected).coerceAtLeast(0)
            val offset by animateDpAsState(width*index, spring(dampingRatio=.86f,stiffness=500f),label="navigation selection")
            Box(Modifier.offset(x=offset).width(width).height(tabHeight).clip(CircleShape)
                .background(colors.onSurface.copy(alpha=.09f)))
            Row(Modifier.fillMaxWidth()) {
                ReaderAppScope.destinations.forEach { destination ->
                    val active=destination==selected
                    val color by animateColorAsState(colors.onSurface,label="navigation icon")
                    val icon=when(destination) {
                        ReaderDestination.Home -> Icons.Filled.Home
                        ReaderDestination.Library -> ReaderBooksSymbols.Library
                        ReaderDestination.Shelf -> Icons.Filled.Cloud
                        ReaderDestination.Settings -> if(active) Icons.Filled.Settings else Icons.Outlined.Settings
                    }
                    val label=destination.localizedLabel(strings)
                    Column(Modifier.weight(1f).height(tabHeight).clip(CircleShape)
                        .selectable(active,onClick={onSelect(destination)},role=Role.Tab)
                        .semantics { contentDescription=label;testTag="nav-${destination.name}" },
                        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        Icon(icon,null,Modifier.size(28.dp),tint=color)
                        if(!compact) Text(label,Modifier.padding(top=3.dp),style=MaterialTheme.typography.labelSmall.copy(fontSize=13.sp,lineHeight=16.sp),
                            fontWeight=if(active) FontWeight.SemiBold else FontWeight.Medium,color=color,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
        IconButton(onClick=onSearch,modifier=Modifier.size(tabHeight+8.dp).shadow(4.dp,CircleShape).clip(CircleShape)
            .readerNavigationGlass(colors.surface).border(1.dp,colors.onSurface.copy(alpha=.09f),CircleShape)
            .semantics { testTag="nav-search" }) {
            Icon(Icons.Outlined.Search,strings.localize(Res.string.search),Modifier.size(28.dp),tint=colors.onSurface)
        }
    }
}

@Composable
fun ReaderBottomNav(selected:ReaderDestination,onSelect:(ReaderDestination)->Unit) {
    ReaderBottomNav(selected,onSelect,onSearch={})
}
