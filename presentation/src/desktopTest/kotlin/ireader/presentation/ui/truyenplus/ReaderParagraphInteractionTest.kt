package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.core.source.model.Text
import ireader.domain.models.prefs.PreferenceValues
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.*
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderParagraphInteractionTest {
    @Test fun theTapThatWakesControlsDoesNotAlsoOpenParagraphActions() {
        var allow=false
        val original="Hoàng Yên Trần khẽ gật đầu."
        val preferences=ReaderTextPreferencesState(20,30,0,400,0,PreferenceValues.PreferenceTextAlignment.Left,
            androidx.compose.ui.graphics.Color.Black,null,0,4,0,true,false,false,0,false)
        ImageComposeScene(360,640) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),
                LocalReaderListening provides ReaderListeningContext(listOf(0),0,true,{}).copy(allowInspection={allow})) {
                MaterialTheme {StyleTextOptimized(Modifier,0,Text(original),false,preferences)}
            }
        }.use {scene ->
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            fun all()=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            fun touch()=all().first {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==original}==true}
                .config[SemanticsActions.OnClick].action!!.invoke()
            scene.render(0);scene.render(1_000_000_000)
            touch();scene.render(2_000_000_000)
            assertFalse(all().any {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-listen-from-here"})
            allow=true;touch();scene.render(3_000_000_000)
            assertTrue(all().any {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-listen-from-here"})
        }
    }
    @Test fun touchingTextCannotSeekUntilTheExplicitListeningActionIsChosen() {
        var selected:Int?=null
        val original="  Hoàng Yên Trần nói: ‘Đi thôi!’"
        val preferences=ReaderTextPreferencesState(20,30,0,400,12,PreferenceValues.PreferenceTextAlignment.Left,
            androidx.compose.ui.graphics.Color.Black,null,2,4,2,true,false,false,0,false)
        ImageComposeScene(360,640,Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),
                LocalReaderListening provides ReaderListeningContext(listOf(7),0,true,{selected=it})) {
                MaterialTheme {StyleTextOptimized(Modifier,7,Text(original),false,preferences)}
            }
        }.use {scene ->
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            fun all()=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            scene.render(0);scene.render(1_000_000_000)
            val paragraph=all().first {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==original}==true}
            paragraph.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
            assertNull(selected,"A tap waking the chrome must not change the spoken paragraph")
            scene.render(2_000_000_000);scene.render(3_000_000_000)
            val action=all().firstOrNull {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-listen-from-here"}
            assertNotNull(action,"The paragraph needs an explicit listening action")
            action.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(0,selected,"The action must select the mapped speech paragraph, not page 7")
        }
    }
}
