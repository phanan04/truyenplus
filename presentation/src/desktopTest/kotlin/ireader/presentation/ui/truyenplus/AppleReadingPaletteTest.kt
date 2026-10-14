package ireader.presentation.ui.truyenplus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.models.common.ColorModel
import ireader.domain.preferences.models.ReaderColors
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*
@OptIn(ExperimentalComposeUiApi::class)
class AppleReadingPaletteTest {
 @Test fun everyPaperChoiceIsVisibleAndSelectableOnNarrowLargeTextPane() {
  val themes=(1L..8L).map {ReaderColors(it,ColorModel(1f,1f,1f),ColorModel(0f,0f,0f))}
  var picked=0L
  ImageComposeScene(320,400,Density(1f,1.6f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderThemePalette(themes,1,{picked=it})}}
  }.use {s->
   s.render(0);s.render(1_000_000_000)
   fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
   val ns=s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
   for(id in 1L..8L){val n=ns.first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-theme-$id"};assertTrue(n.boundsInRoot.left>=0 && n.boundsInRoot.right<=320 && n.boundsInRoot.height>=48)}
   ns.first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-theme-8"}.config[SemanticsActions.OnClick].action!!.invoke();assertEquals(8,picked)
  }
 }
 @Test fun fourteenPapersDoNotOverflowAndTheRemainingChoicesStayReachable() {
  val themes=(1L..14L).map {ReaderColors(it,ColorModel(1f,1f,1f),ColorModel(0f,0f,0f))}
  var picked=0L
  ImageComposeScene(320,208,Density(1f,1.3f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderThemePalette(themes,1,{picked=it})}}
  }.use {s->
   s.render(0);s.render(1_000_000_000)
   fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
   fun ns()=s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
   val next=ns().first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-paper-next"}
   assertTrue(next.boundsInRoot.bottom<=208 && next.boundsInRoot.height>=48)
   next.config[SemanticsActions.OnClick].action!!.invoke();s.render(2_000_000_000)
   val last=ns().first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-theme-14"}
   assertTrue(last.boundsInRoot.height>=48 && last.boundsInRoot.bottom<=208)
   last.config[SemanticsActions.OnClick].action!!.invoke();assertEquals(14,picked)
  }
 }

}
