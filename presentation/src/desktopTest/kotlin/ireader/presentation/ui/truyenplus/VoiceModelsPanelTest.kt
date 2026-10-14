package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.services.tts_service.v2.offline.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class VoiceModelsPanelTest {
 @Test fun customVoiceNamesStaySeparateFromModelAndSpeakerIdentity() {
  val model=OfflineVoiceCatalog.models.first()
  var renamed:Triple<String,Int,String>?=null
  ImageComposeScene(412,800,Density(1f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
    MaterialTheme {VoiceModelsPanel(OfflineVoiceState(),false,false,{},{},{},{_,_->},{_,_->},{},{},
      preferredNames=mapOf("${model.id}:0" to "Giọng của tôi"),onRename={id,speaker,name->renamed=Triple(id,speaker,name)})}
   }
  }.use {scene->
   scene.render();scene.render()
   assertTrue(nodes(scene).any {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text=="Giọng của tôi"}==true})
   nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)=="voice-rename-${model.id}"}.config[SemanticsActions.OnClick].action!!.invoke()
   scene.render();scene.render()
   nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)=="voice-name-input"}.config[SemanticsActions.SetText].action!!.invoke(androidx.compose.ui.text.AnnotatedString("Tên riêng"))
   scene.render();scene.render()
   nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)=="voice-name-save"}.config[SemanticsActions.OnClick].action!!.invoke()
   assertEquals(Triple(model.id,0,"Tên riêng"),renamed)
  }
 }
 @Test fun voiceSelectionDoesNotAddADarkScreenBehindTheFloatingPanel() {
  ImageComposeScene(412,800,Density(1f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
    MaterialTheme {Box(Modifier.fillMaxSize().background(Color.White)) {
     VoiceModelsPanel(OfflineVoiceState(),false,false,{},{},{},{_,_->},{_,_->},{},{})
    }}
   }
  }.use {scene ->
   scene.render();scene.render()
   val png=requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
   val image=javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
   assertTrue(((image.getRGB(1,1) shr 16) and 255)>250,"Voice selection dims the chapter")
  }
 }
 @Test fun unavailableModelsCannotBeSelectedAndDownloadUsesTheRealCatalogId() {
  val piper=OfflineVoiceCatalog.models.first()
  var state by mutableStateOf(OfflineVoiceState())
  var download="";var selected=""
  ImageComposeScene(412,800,Density(1f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
    MaterialTheme {VoiceModelsPanel(state,false,false,{download=it},{},{},{id,_->selected=id},{_,_->},{},{})}
   }
  }.use {scene ->
   scene.render();scene.render()
   fun node(tag:String)=nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}
   assertTrue(node("voice-use-${piper.id}").config.contains(SemanticsProperties.Disabled))
   node("voice-download-${piper.id}").config[SemanticsActions.OnClick].action!!.invoke()
   assertEquals(piper.id,download)
   state=state.copy(installed=setOf(piper.id))
   scene.render();scene.render()
   assertFalse(node("voice-use-${piper.id}").config.contains(SemanticsProperties.Disabled))
   node("voice-use-${piper.id}").config[SemanticsActions.OnClick].action!!.invoke()
   assertEquals(piper.id,selected)
  }
 }
 private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
  fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
  return scene.semanticsOwners.flatMap{all(it.rootSemanticsNode)}
 }
}
