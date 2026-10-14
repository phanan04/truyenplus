package ireader.presentation.ui.truyenplus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import ireader.presentation.core.toComposeTextAlign
import ireader.presentation.ui.reader.ReaderTextPreferencesState
import ireader.domain.models.prefs.mapTextAlign

/** Draw only the actual spoken sentence's line boxes; timing is supplied by the engine. */
@Composable
internal fun ReaderSpokenParagraph(text:String,page:Int,context:ReaderListeningContext,
    preferences:ReaderTextPreferencesState,modifier:Modifier) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val sentence=context.sentenceRange(page,text)
    val visibleRange=sentence.takeIf {context.highlight}
    var previous by remember { mutableStateOf<IntRange?>(null) }
    var last by remember { mutableStateOf<IntRange?>(null) }
    val fade=remember { Animatable(1f) }
    LaunchedEffect(visibleRange) {
        previous=last;last=visibleRange
        fade.snapTo(0f);fade.animateTo(1f,tween(180))
    }
    LaunchedEffect(layout,sentence) {
        val result=layout
        if(result!=null && sentence!=null && sentence.last<text.length) {
            val first=result.getLineForOffset(sentence.first)
            val end=result.getLineForOffset(sentence.last)
            context.onSentenceBounds(page,Rect(0f,result.getLineTop(first),result.size.width.toFloat(),result.getLineBottom(end)))
        }
    }
    val decorated=modifier.drawWithContent {
        val result=layout
        fun shade(range:IntRange?,alpha:Float) {
            if(result==null || range==null || range.isEmpty() || range.last>=text.length || alpha<=0)return
            val first=result.getLineForOffset(range.first);val end=result.getLineForOffset(range.last)
            for(line in first..end) {
                val start=maxOf(range.first,result.getLineStart(line))
                val until=minOf(range.last+1,result.getLineEnd(line,visibleEnd=true))
                if(until<=start)continue
                val left=result.getBoundingBox(start).left
                val right=result.getBoundingBox(until-1).right
                val top=result.getLineTop(line)+2.dp.toPx()
                val bottom=result.getLineBottom(line)-2.dp.toPx()
                drawRoundRect(preferences.textColor.copy(alpha=.11f*alpha),Offset(left-2.dp.toPx(),top),
                    Size((right-left).coerceAtLeast(1f)+4.dp.toPx(),(bottom-top).coerceAtLeast(1f)),CornerRadius(4.dp.toPx()))
            }
        }
        shade(previous,1f-fade.value);shade(visibleRange,fade.value)
        if(result!=null && context.isHighlighted(page) && sentence==null) {
            drawCircle(preferences.textColor.copy(alpha=.35f),2.dp.toPx(),Offset(0f,result.getLineTop(0)+8.dp.toPx()))
        }
        drawContent()
    }
    val content:@Composable ()->Unit={
        Text(text,decorated,fontSize=preferences.fontSize.sp,fontFamily=preferences.fontFamily,
            color=preferences.textColor,lineHeight=preferences.lineHeight.sp,
            letterSpacing=preferences.letterSpacing.sp,fontWeight=FontWeight(preferences.fontWeight),
            textAlign=mapTextAlign(preferences.textAlignment).toComposeTextAlign(),onTextLayout={layout=it})
    }
    if(preferences.selectable)SelectionContainer {content()} else content()
}
