package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import ireader.domain.models.common.FontFamilyModel
import ireader.domain.preferences.models.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderControlsV3Test {
 @Test fun thePlayerShowsTheEngineActuallyUsedAfterFallback() {
  val choice=ireader.domain.services.tts_service.v2.offline.OfflineVoiceSelection("piper-vi-vais-int8",0)
  assertEquals("Giọng hệ thống",readerVoiceLabel(choice,"Native Android TTS","Giọng hệ thống"))
  assertEquals("Piper",readerVoiceLabel(choice,"Piper","Giọng hệ thống"))
  assertEquals("Piper",readerVoiceLabel(choice,"","Giọng hệ thống"))
 }
 @Test fun floatingControlsFollowPaperAndCompactHasChapterActionsWithIndependentToggle() {
  var toggles=0;var previous=0;var next=0
  val paper=Color(0xffeadcb7);val ink=Color(0xff32291e)
  ImageComposeScene(360,640,Density(1f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
    MaterialTheme {ReaderPaperTheme(paper,ink) {
     assertEquals(paper,MaterialTheme.colorScheme.surface)
     assertEquals(ink,MaterialTheme.colorScheme.onSurface)
     Column {ReaderCollapsedPlayer(true,{},{toggles++},{},{},onPrevious={previous++},onNext={next++})
      ReaderFloatingPlayer(true,1f,.2f,{},{},{},{},{},{},onMinimize={toggles++})}
    }}
   }
  }.use {scene ->
   scene.render();scene.render()
   val nodes=nodes(scene)
   fun click(tag:String)=nodes.first{it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config[SemanticsActions.OnClick].action!!.invoke()
   click("listen-compact-previous");click("listen-compact-next");click("listen-compact-toggle");click("listen-expanded-toggle")
   assertEquals(1,previous);assertEquals(1,next);assertEquals(2,toggles)
   val speeds=nodes.flatMap{it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.map{it.text}.filter{it.endsWith("x")}
   assertFalse("2.25x" in speeds);assertTrue("0.5x" in speeds);assertTrue("2x" in speeds)
  }
 }
 @Test fun systemFontSurvivesSavingWithoutBecomingAGoogleFont() {
  for(family in listOf(FontFamilyModel.Default,FontFamilyModel.Serif,FontFamilyModel.SansSerif,FontFamilyModel.Monospace,FontFamilyModel.Cursive)) {
   assertEquals(family,decodeReaderFont(encodeReaderFont(FontType("ignored",family))).fontFamily)
  }
  assertEquals(FontFamilyModel.Serif,decodeReaderFont("Serif").fontFamily)
  assertEquals(FontFamilyModel.SansSerif,decodeReaderFont("Sans serif").fontFamily)
  assertEquals(FontFamilyModel.Custom("Lora"),decodeReaderFont("Lora").fontFamily)
 }
 private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
  fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
  return scene.semanticsOwners.flatMap{all(it.rootSemanticsNode)}
 }
}
