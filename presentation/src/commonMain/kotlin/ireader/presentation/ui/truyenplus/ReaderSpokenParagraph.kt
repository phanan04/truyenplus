package ireader.presentation.ui.truyenplus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import ireader.presentation.core.toComposeTextAlign
import ireader.presentation.ui.reader.ReaderTextPreferencesState
import ireader.domain.models.prefs.mapTextAlign

/** Static paragraph wash and engine-driven marker. Never changes glyph geometry or audio work. */
@Composable
internal fun ReaderSpokenParagraph(text:String,page:Int,context:ReaderListeningContext,
    preferences:ReaderTextPreferencesState,modifier:Modifier,onSelect:(()->Unit)?=null,actionLabel:String="") {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val sentence=context.sentenceRange(page,text)
    val visibleRange=sentence.takeIf {context.highlight}
    val paper=LocalReaderPaper.current?.paper ?: androidx.compose.material3.MaterialTheme.colorScheme.background
    val dark=paper.luminance()<.4f
    val pigment=if(dark)Color(0xffb89add) else Color(0xffe4af45)
    val accent=if(dark)Color(0xffc1a3e5) else Color(0xffa37423)
    val active=context.highlight && (context.isHighlighted(page) || sentence!=null)
    val cardAlpha=animateFloatAsState(if(active)1f else 0f,tween(180),label="spoken paragraph")
    val dim=context.highlight && context.focusSurroundings && !active && context.paragraphForReaderPage(page)!=null
    val textAlpha=animateFloatAsState(if(dim).78f else 1f,tween(180),label="reading focus")
    val wash=remember(paper,pigment,dark) { Brush.linearGradient(listOf(
        lerp(paper,pigment,if(dark).12f else .14f),lerp(paper,pigment,.045f))) }
    val sentenceFill=lerp(paper,pigment,if(dark).38f else .36f)
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
    val decorated=modifier.graphicsLayer {alpha=textAlpha.value}.drawWithContent {
        val result=layout
        if(cardAlpha.value>0f) {
            val inset=7.dp.toPx()
            val origin=Offset(-inset,-3.dp.toPx())
            val extent=Size(size.width+inset*2,size.height+6.dp.toPx())
            val corner=CornerRadius(12.dp.toPx())
            // A static low-opacity edge suggests paper depth without an offscreen blur/shadow.
            drawRoundRect(Color.Black.copy(alpha=(if(dark).08f else .035f)*cardAlpha.value),
                origin+Offset(0f,1.5.dp.toPx()),extent,corner)
            drawRoundRect(wash,origin,extent,corner,alpha=cardAlpha.value)
            drawRoundRect(accent.copy(alpha=.72f*cardAlpha.value),
                Offset(-inset,3.dp.toPx()),Size(2.5.dp.toPx(),(size.height-6.dp.toPx()).coerceAtLeast(1f)),CornerRadius(2.dp.toPx()))
        }
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
                    Size((right-left).coerceAtLeast(1f)+4.dp.toPx(),(bottom-top).coerceAtLeast(1f)),CornerRadius(6.dp.toPx()))
            }
        }
        shade(previous,1f-fade.value);shade(visibleRange,fade.value)
        shade(previousWord,1f-wordFade.value,wordFill);shade(word,wordFade.value,wordFill)
        drawContent()
    }
    val interactive=if(onSelect==null)decorated else decorated
        .readerSpeechTap(text,{layout},onSelect)
        // Keep each speech target separate from the reader container's click action.
        .semantics(mergeDescendants=true) {
            onClick(actionLabel){onSelect();true}
            customActions=listOf(CustomAccessibilityAction(actionLabel){onSelect();true})
        }
    val content:@Composable ()->Unit={
        Text(text,interactive,style=androidx.compose.ui.text.TextStyle(textIndent=androidx.compose.ui.text.style.TextIndent(firstLine=preferences.paragraphIndent.sp)),fontSize=preferences.fontSize.sp,fontFamily=preferences.fontFamily,
            color=preferences.textColor,lineHeight=preferences.lineHeight.sp,
            letterSpacing=preferences.letterSpacing.sp,fontWeight=FontWeight(preferences.fontWeight),
            textAlign=mapTextAlign(preferences.textAlignment).toComposeTextAlign(),onTextLayout={layout=it})
    }
    // Listening owns a short tap; text selection stays available in normal reading.
    content()
}

/** Only occupied text lines seek. Indents, trailing line space and paragraph gaps remain chrome targets. */
internal fun readerTextContains(text:String,layout:TextLayoutResult,point:Offset):Boolean {
    if(text.isEmpty() || point.x<0 || point.y<0 || point.x>=layout.size.width || point.y>=layout.size.height)return false
    val line=layout.getLineForVerticalPosition(point.y)
    val start=layout.getLineStart(line)
    val end=layout.getLineEnd(line,visibleEnd=true).coerceAtMost(text.length)
    val first=(start until end).firstOrNull {!text[it].isWhitespace()} ?: return false
    val last=(end-1 downTo first).first {!text[it].isWhitespace()}
    val a=layout.getBoundingBox(first);val b=layout.getBoundingBox(last)
    return point.x>=minOf(a.left,b.left) && point.x<maxOf(a.right,b.right) &&
        point.y>=minOf(a.top,b.top) && point.y<maxOf(a.bottom,b.bottom)
}

@Composable private fun Modifier.readerSpeechTap(text:String,layout:()->TextLayoutResult?,onSelect:()->Unit):Modifier {
    val select by rememberUpdatedState(onSelect)
    val currentLayout by rememberUpdatedState(layout)
    return pointerInput(text) {
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false)
            var moved=false
            do {
                val event=awaitPointerEvent()
                val change=event.changes.firstOrNull {it.id==down.id} ?: break
                moved=moved || (change.position-down.position).getDistance()>viewConfiguration.touchSlop || event.changes.size>1
                if(!change.pressed) {
                    val result=currentLayout()
                    if(!moved && !change.isConsumed && result!=null &&
                        change.uptimeMillis-down.uptimeMillis<viewConfiguration.longPressTimeoutMillis &&
                        readerTextContains(text,result,down.position) && readerTextContains(text,result,change.position)) {
                        change.consume();select()
                    }
                    break
                }
            } while(true)
        }
    }
}
