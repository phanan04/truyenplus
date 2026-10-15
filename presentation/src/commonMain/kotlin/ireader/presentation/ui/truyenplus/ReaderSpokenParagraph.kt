package ireader.presentation.ui.truyenplus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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
    val paper=LocalReaderPaper.current?.paper ?: androidx.compose.material3.MaterialTheme.colorScheme.background
    val dark=paper.luminance()<.4f
    val accent=if(dark)Color(0xffe8bd68) else Color(0xff99681b)
    val sentenceFill=lerp(paper,Color(0xffd8a746),if(dark).20f else .22f)
    val wordFill=lerp(sentenceFill,accent,if(dark).28f else .18f)
    var previous by remember { mutableStateOf<IntRange?>(null) }
    var last by remember { mutableStateOf<IntRange?>(null) }
    val fade=remember { Animatable(1f) }
    val word=context.wordRange(page,text)
    val wordFade=remember {Animatable(1f)}
    var previousWord by remember {mutableStateOf<IntRange?>(null)}
    var lastWord by remember {mutableStateOf<IntRange?>(null)}
    LaunchedEffect(word) {
        previousWord=lastWord;lastWord=word
        wordFade.snapTo(0f);wordFade.animateTo(1f,tween(100))
    }
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
        fun shade(range:IntRange?,alpha:Float,fill:Color=sentenceFill) {
            if(result==null || range==null || range.isEmpty() || range.last>=text.length || alpha<=0)return
            val first=result.getLineForOffset(range.first);val end=result.getLineForOffset(range.last)
            for(line in first..end) {
                val start=maxOf(range.first,result.getLineStart(line))
                val until=minOf(range.last+1,result.getLineEnd(line,visibleEnd=true))
                if(until<=start)continue
                val firstBox=result.getBoundingBox(start)
                val lastBox=result.getBoundingBox(until-1)
                val left=minOf(firstBox.left,lastBox.left)
                val right=maxOf(firstBox.right,lastBox.right)
                val top=result.getLineTop(line)+2.dp.toPx()
                val bottom=result.getLineBottom(line)-2.dp.toPx()
                drawRoundRect(fill.copy(alpha=alpha),Offset(left-2.dp.toPx(),top),
                    Size((right-left).coerceAtLeast(1f)+4.dp.toPx(),(bottom-top).coerceAtLeast(1f)),CornerRadius(4.dp.toPx()))
            }
        }
        shade(previous,1f-fade.value);shade(visibleRange,fade.value)
        shade(previousWord,1f-wordFade.value,wordFill);shade(word,wordFade.value,wordFill)
        if(result!=null && visibleRange!=null && !visibleRange.isEmpty() && visibleRange.last<text.length) {
            val line=result.getLineForOffset(visibleRange.first)
            // A small gutter marker never changes glyph positions or paragraph height.
            drawRoundRect(accent.copy(alpha=.75f*fade.value),Offset(-6.dp.toPx(),result.getLineTop(line)+3.dp.toPx()),
                Size(2.dp.toPx(),(result.getLineBottom(line)-result.getLineTop(line)-6.dp.toPx()).coerceAtLeast(1f)),CornerRadius(1.dp.toPx()))
        }
        if(result!=null && context.isHighlighted(page) && sentence==null) {
            drawCircle(accent.copy(alpha=.75f),2.dp.toPx(),Offset(-5.dp.toPx(),result.getLineTop(0)+8.dp.toPx()))
        }
        drawContent()
    }
    val content:@Composable ()->Unit={
        Text(text,decorated,style=androidx.compose.ui.text.TextStyle(textIndent=androidx.compose.ui.text.style.TextIndent(firstLine=preferences.paragraphIndent.sp)),fontSize=preferences.fontSize.sp,fontFamily=preferences.fontFamily,
            color=preferences.textColor,lineHeight=preferences.lineHeight.sp,
            letterSpacing=preferences.letterSpacing.sp,fontWeight=FontWeight(preferences.fontWeight),
            textAlign=mapTextAlign(preferences.textAlignment).toComposeTextAlign(),onTextLayout={layout=it})
    }
    // Listening owns a short tap; text selection stays available in normal reading.
    content()
}
