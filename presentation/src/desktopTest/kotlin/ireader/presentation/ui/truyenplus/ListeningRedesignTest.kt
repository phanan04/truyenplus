package ireader.presentation.ui.truyenplus

import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ListeningRedesignTest {
 @Test fun expandedControllerExplainsChapterNavigationAndHasSeparateMinimize() {
  var previous=0;var next=0;var minimize=0;var settings=0
  scene {
   ReaderFloatingPlayer(true,1.25f,.25f,{previous++},{},{next++},{},{settings++},{},
    onMinimize={minimize++},voiceLabel="Giọng hệ thống",chapterLabel="Chương 273",paragraphIndex=11,paragraphCount=48)
  }.use {scene ->
   scene.render();scene.render()
   val text=nodes(scene).flatMap{it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.joinToString {it.text}
   assertTrue("Chương 273" in text);assertTrue("12 / 48" in text)
   click(scene,"listen-previous-chapter");click(scene,"listen-next-chapter");click(scene,"listen-minimize");click(scene,"listen-settings")
   assertEquals(listOf(1,1,1,1),listOf(previous,next,minimize,settings))
  }
 }
 @Test fun settingsKeepSpeechVolumeAccessibleAndCanDismissWithoutStoppingPlayback() {
  var close=0;var volume=0f
  scene {ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,false,30,ListeningAudioState(speechVolume=.7f),{},{},{},{},{},{},{},{},{},{},
   onDismiss={close++},onSpeechVolume={volume=it})}.use {scene ->
    scene.render();scene.render()
    val control=nodes(scene).first{it.config.getOrNull(SemanticsProperties.TestTag)=="listen-speech-volume"}
    control.config[SemanticsActions.SetProgress].action!!.invoke(.4f)
    click(scene,"listen-options-close")
    assertEquals(.4f,volume);assertEquals(1,close)
    assertFalse(nodes(scene).any{it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null})
   }
 }
 @Test fun settingsAndCompactPlaybackRemainReachableAcrossWindows() {
  val windows=listOf(Triple(280,640,2f),Triple(360,800,1.5f),Triple(480,320,2f),Triple(800,360,1.5f),Triple(840,600,1.5f))
  for((width,height,font) in windows) {
   ImageComposeScene(width,height,Density(1f,font)) {
    CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
     MaterialTheme {ReaderBackdrop {ReaderResponsiveFrame {
      androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter) {
       ReaderListeningDock(true,options={ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,false,30,ListeningAudioState(supported=true,musicTitle="Nhạc.mp3"),{},{},{},{},{},{},{},{},{},{},onDismiss={},onSpeechVolume={})},player={ReaderCollapsedPlayer(true,{},{},{},{})})
      }
     }}}
    }
   }.use {scene ->
    scene.render();scene.render()
    val output=java.io.File("build/reports/listening-redesign/settings-${width}x${height}-${font}.png")
    output.parentFile.mkdirs();output.writeBytes(requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes)
    fun visible(tag:String) {
     val node=nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}
     val bounds=node.boundsInRoot
     val touch=node.touchBoundsInRoot
     // A slider exposes its draggable track; button target checks apply to tap actions.
     if(node.config.getOrNull(SemanticsActions.SetProgress)!=null) {
      assertTrue(bounds.width>=47 && bounds.height>0,"Invisible slider: $bounds")
     } else assertTrue(touch.width>=47 && touch.height>=47,"Target too small: $tag $touch (visual $bounds) at $width x $height")
     assertTrue(bounds.left>=0 && bounds.top>=0 && bounds.right<=width+1 && bounds.bottom<=height+1,"Offscreen: $tag $bounds at $width x $height")
    }
    for(tag in listOf("listen-options-close","listen-play","listen-expand","listen-close","listen-voice-choice","listen-pitch-increase","listen-speech-volume"))visible(tag)
    for(page in listOf("music","reading","timer")) {
     click(scene,"listen-page-$page");scene.render();scene.render()
     visible("listen-play");visible("listen-options-close")
     val switches=nodes(scene).filter{it.config.getOrNull(SemanticsProperties.ToggleableState)!=null}
     assertTrue(switches.all{it.touchBoundsInRoot.height>=47 && it.boundsInRoot.bottom<=height+1},"Clipped switch on $page at $width x $height")
    }
   }
  }
 }
 private fun scene(content:@androidx.compose.runtime.Composable ()->Unit)=ImageComposeScene(412,800,Density(1f)) {
  CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()){MaterialTheme {ReaderBackdrop {content()}}}
 }
 private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
  fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
  return scene.semanticsOwners.flatMap{all(it.rootSemanticsNode)}
 }
 private fun click(scene:ImageComposeScene,tag:String){nodes(scene).first{it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config[SemanticsActions.OnClick].action!!.invoke()}
}
