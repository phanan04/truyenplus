package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Controls inherit an enclosing panel's diffusion. Only standalone controls sample the backdrop. */
@Composable internal fun Modifier.readerControlGlass(shape:Shape,enabled:Boolean=true,selected:Boolean=false):Modifier {
    if(!LocalReaderFullGlass.current)return this
    val c=MaterialTheme.colorScheme
    return clip(shape).readerCardGlass(c.surface,shape=shape)
        .background(c.onSurface.copy(alpha=if(selected && enabled).12f else .025f))
        .border(if(selected)1.dp else .5.dp,c.onSurface.copy(alpha=if(!enabled).08f else if(selected).28f else .14f),shape)
}

@Composable private fun ReaderControlContent(content:@Composable ()->Unit) {
    ReaderGlassPanelContent(content)
}

@Composable private fun glassButtonColors(colors:ButtonColors):ButtonColors =
    if(!LocalReaderFullGlass.current)colors else colors.copy(
        containerColor=Color.Transparent,disabledContainerColor=Color.Transparent,
        contentColor=if(colors.containerColor==MaterialTheme.colorScheme.error || colors.contentColor==MaterialTheme.colorScheme.error)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        disabledContentColor=MaterialTheme.colorScheme.onSurface.copy(alpha=.38f))

@Composable fun ReaderGlassButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=ButtonDefaults.shape,colors:ButtonColors=ButtonDefaults.buttonColors(),
    elevation:ButtonElevation?=ButtonDefaults.buttonElevation(),border:BorderStroke?=null,
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,
    content:@Composable RowScope.()->Unit) {
    Button(onClick,modifier.readerControlGlass(shape,enabled),enabled,shape,glassButtonColors(colors),
        if(LocalReaderFullGlass.current)null else elevation,border,contentPadding,interactionSource) {
        val row=this;ReaderControlContent {content(row)}
    }
}

@Composable fun ReaderGlassOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=ButtonDefaults.outlinedShape,colors:ButtonColors=ButtonDefaults.outlinedButtonColors(),
    elevation:ButtonElevation?=null,border:BorderStroke?=ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,
    content:@Composable RowScope.()->Unit) {
    OutlinedButton(onClick,modifier.readerControlGlass(shape,enabled),enabled,shape,glassButtonColors(colors),elevation,
        if(LocalReaderFullGlass.current)null else border,contentPadding,interactionSource) {
        val row=this;ReaderControlContent {content(row)}
    }
}

@Composable fun ReaderGlassTextButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=ButtonDefaults.textShape,colors:ButtonColors=ButtonDefaults.textButtonColors(),
    elevation:ButtonElevation?=null,border:BorderStroke?=null,contentPadding:PaddingValues=ButtonDefaults.TextButtonContentPadding,
    interactionSource:MutableInteractionSource?=null,content:@Composable RowScope.()->Unit) {
    TextButton(onClick,modifier,enabled,shape,glassButtonColors(colors),elevation,border,contentPadding,interactionSource) {
        val row=this;ReaderControlContent {content(row)}
    }
}

@Composable fun ReaderGlassIconButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    colors:IconButtonColors=IconButtonDefaults.iconButtonColors(),interactionSource:MutableInteractionSource?=null,
    content:@Composable ()->Unit) {
    IconButton(onClick,modifier,enabled,
        if(LocalReaderFullGlass.current)colors.copy(containerColor=Color.Transparent,disabledContainerColor=Color.Transparent) else colors,
        interactionSource) {ReaderControlContent(content)}
}

@Composable fun ReaderGlassFilterChip(selected:Boolean,onClick:()->Unit,label:@Composable ()->Unit,
    modifier:Modifier=Modifier,enabled:Boolean=true,leadingIcon:(@Composable ()->Unit)?=null,
    trailingIcon:(@Composable ()->Unit)?=null,shape:Shape=FilterChipDefaults.shape,
    colors:SelectableChipColors=FilterChipDefaults.filterChipColors(),elevation:SelectableChipElevation?=FilterChipDefaults.filterChipElevation(),
    border:BorderStroke?=FilterChipDefaults.filterChipBorder(enabled,selected),interactionSource:MutableInteractionSource?=null) {
    FilterChip(selected,onClick,label,modifier.readerControlGlass(shape,enabled,selected),enabled,leadingIcon,trailingIcon,shape,
        if(LocalReaderFullGlass.current)colors.copy(containerColor=Color.Transparent,selectedContainerColor=Color.Transparent,
            disabledContainerColor=Color.Transparent,disabledSelectedContainerColor=Color.Transparent) else colors,
        if(LocalReaderFullGlass.current)null else elevation,if(LocalReaderFullGlass.current)null else border,interactionSource)
}

@Composable fun ReaderGlassOutlinedTextField(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,
    enabled:Boolean=true,readOnly:Boolean=false,textStyle:TextStyle=LocalTextStyle.current,
    label:(@Composable ()->Unit)?=null,placeholder:(@Composable ()->Unit)?=null,
    leadingIcon:(@Composable ()->Unit)?=null,trailingIcon:(@Composable ()->Unit)?=null,
    prefix:(@Composable ()->Unit)?=null,suffix:(@Composable ()->Unit)?=null,supportingText:(@Composable ()->Unit)?=null,
    isError:Boolean=false,visualTransformation:VisualTransformation=VisualTransformation.None,
    keyboardOptions:KeyboardOptions=KeyboardOptions.Default,keyboardActions:KeyboardActions=KeyboardActions.Default,
    singleLine:Boolean=false,maxLines:Int=if(singleLine)1 else Int.MAX_VALUE,minLines:Int=1,
    interactionSource:MutableInteractionSource?=null,shape:Shape=OutlinedTextFieldDefaults.shape,
    colors:TextFieldColors=OutlinedTextFieldDefaults.colors()) {
    // Keep the outline and floating label: they communicate focus, validation and disabled states.
    OutlinedTextField(value,onValueChange,modifier.readerControlGlass(shape,enabled),enabled,readOnly,textStyle,
        label,placeholder,leadingIcon,trailingIcon,prefix,suffix,supportingText,isError,visualTransformation,
        keyboardOptions,keyboardActions,singleLine,maxLines,minLines,interactionSource,shape,
        if(LocalReaderFullGlass.current)colors.copy(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,
            disabledContainerColor=Color.Transparent,errorContainerColor=Color.Transparent) else colors)
}
