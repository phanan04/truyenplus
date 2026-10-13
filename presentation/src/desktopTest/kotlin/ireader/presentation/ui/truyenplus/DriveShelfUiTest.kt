package ireader.presentation.ui.truyenplus
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.*
import ireader.domain.models.shelf.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*
import org.jetbrains.skia.EncodedImageFormat
@OptIn(ExperimentalComposeUiApi::class)
class DriveShelfUiTest {
 @Test fun emptyQueryShowsSourcesAndDownloadCallsOnlySelectedFile() {
  java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi"))
  var downloaded:ShelfEntry?=null
  fun scene(query:String,entries:List<ShelfEntry>):ImageComposeScene=ImageComposeScene(360,800,Density(1f)) {
   CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
    MaterialTheme(colorScheme=ireader.presentation.ui.core.theme.themes.TruyenPlusLightColors) { ReaderBackdrop { ReaderShelfScreen(listOf(ShelfSource("root","Kho của tôi")),query,ShelfSearchPage(entries,false),false,null,emptyMap(),
     onQuery={},onSource={},onAdd={},onRefresh={},onRemove={},onDownload={downloaded=it},onBook={},onLibrary={},onPrevious={},onNext={},onConnect={},onCancel={},showConnect=false) } }
   }
  }
  scene("",emptyList()).use { s->s.render();s.render();save(s,"sources");assertTrue(nodes(s).any { it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-query" });assertFalse(nodes(s).any { it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-download" });assertFalse(nodes(s).any { n->n.config.getOrNull(SemanticsProperties.Text)?.any { "Kết nối Google" in it.text }==true }) }
  val entry=ShelfEntry("root","a","Vạn Cổ Thần Đế.epub")
  scene("van co",listOf(entry)).use { s->s.render();s.render();save(s,"search");val action=nodes(s).first { it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-download" }.config[SemanticsActions.OnClick].action!!;action();assertEquals(entry,downloaded) }
 }
 private fun save(scene:ImageComposeScene,name:String) {
  val file=java.io.File("build/reports/drive-shelf/$name.png");file.parentFile.mkdirs()
  file.writeBytes(requireNotNull(scene.render().encodeToData(EncodedImageFormat.PNG)).bytes)
 }
 private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
  fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
  return scene.semanticsOwners.flatMap { all(it.rootSemanticsNode) }
 }
}
