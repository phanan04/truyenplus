package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Shared utility screens keep their original card colors/elevation when Glass is off. */
@Composable fun ReaderOptionalGlassCard(
    modifier:Modifier=Modifier,shape:Shape=CardDefaults.shape,
    colors:CardColors=CardDefaults.cardColors(),elevation:CardElevation=CardDefaults.cardElevation(),
    border:BorderStroke?=null,content:@Composable ColumnScope.()->Unit
) {
    val full=LocalReaderFullGlass.current
    Card(modifier=if(full)modifier.clip(shape).readerCardGlass(MaterialTheme.colorScheme.surface,shape=shape) else modifier,
        shape=shape,colors=if(full)colors.copy(containerColor=Color.Transparent,disabledContainerColor=Color.Transparent) else colors,
        elevation=elevation,border=border) {
        val column=this
        ReaderGlassPanelContent {content(column)}
    }
}

/** Menus share the same captured input across popup windows; no own-content blur. */
@Composable fun ReaderGlassDropdownMenu(expanded:Boolean,onDismissRequest:()->Unit,
    modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    val full=LocalReaderFullGlass.current
    DropdownMenu(expanded,onDismissRequest,modifier=if(full)modifier.clip(RoundedCornerShape(20.dp))
        .readerNavigationGlass(MaterialTheme.colorScheme.surface,radius=20,material=ReaderGlassMaterial.Frosted) else modifier,
        containerColor=if(full)Color.Transparent else MenuDefaults.containerColor,
        shape=if(full)RoundedCornerShape(20.dp) else MenuDefaults.shape,content=content)
}

/** Surfaces already own their material. Descendant controls must reuse it instead of sampling again. */
@Composable fun ReaderGlassGroupSurface(modifier:Modifier=Modifier,shape:Shape=androidx.compose.ui.graphics.RectangleShape,
    color:Color=MaterialTheme.colorScheme.surface,contentColor:Color=contentColorFor(color),
    tonalElevation:androidx.compose.ui.unit.Dp=0.dp,shadowElevation:androidx.compose.ui.unit.Dp=0.dp,
    border:BorderStroke?=null,content:@Composable ()->Unit) {
    Surface(modifier,shape,color,contentColor,tonalElevation,shadowElevation,border) {
        ReaderGlassPanelContent(content)
    }
}

@Composable fun ReaderGlassGroupSurface(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=MaterialTheme.shapes.medium,color:Color=MaterialTheme.colorScheme.surface,contentColor:Color=contentColorFor(color),
    tonalElevation:androidx.compose.ui.unit.Dp=0.dp,shadowElevation:androidx.compose.ui.unit.Dp=0.dp,
    border:BorderStroke?=null,interactionSource:androidx.compose.foundation.interaction.MutableInteractionSource?=null,
    content:@Composable ()->Unit) {
    Surface(onClick,modifier,enabled,shape,color,contentColor,tonalElevation,shadowElevation,border,interactionSource) {
        ReaderGlassPanelContent(content)
    }
}

/** Transient feedback uses the same window material; messages and actions remain sharp. */
@Composable fun ReaderGlassSnackbarHost(hostState:SnackbarHostState,modifier:Modifier=Modifier) {
    SnackbarHost(hostState,modifier) {data ->
        val full=LocalReaderFullGlass.current
        val c=MaterialTheme.colorScheme
        Snackbar(snackbarData=data,modifier=Modifier.readerOptionalPanelGlass(16),
            shape=if(full)RoundedCornerShape(16.dp) else SnackbarDefaults.shape,
            containerColor=if(full)Color.Transparent else SnackbarDefaults.color,
            contentColor=if(full)c.onSurface else SnackbarDefaults.contentColor,
            actionColor=if(full)c.onSurface else SnackbarDefaults.actionColor,
            dismissActionContentColor=if(full)c.onSurface else SnackbarDefaults.dismissActionContentColor)
    }
}
