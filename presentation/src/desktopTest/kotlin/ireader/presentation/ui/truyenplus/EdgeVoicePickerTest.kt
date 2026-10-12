package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.services.tts_service.v2.EdgeVoice
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import java.util.Locale
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class EdgeVoicePickerTest {
    @Test fun prioritizedVoicesAreLocalizedAndInvokeSelection() {
        val previous = Locale.getDefault(); Locale.setDefault(Locale.forLanguageTag("vi"))
        val selected = mutableListOf<EdgeVoice>(); var system = 0
        val scene = ImageComposeScene(412, 800, Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ReaderVoiceChoiceDialog(true, EdgeVoice.NamMinh, { selected += it }, { system++ }, {}) }
            }
        }
        try {
            scene.render(0); scene.render(1_000_000_000)
            fun nodes(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap(::nodes)
            val all = scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
            fun choice(label: String) = all.first { n ->
                n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true &&
                    n.config.getOrNull(SemanticsActions.OnClick)?.action != null
            }
            val female = choice("Hoài My · Nữ"); val male = choice("Nam Minh · Nam"); val native = choice("Giọng hệ thống")
            assertTrue(female.boundsInRoot.top < male.boundsInRoot.top && male.boundsInRoot.top < native.boundsInRoot.top)
            assertTrue(male.config[SemanticsProperties.Selected])
            requireNotNull(female.config[SemanticsActions.OnClick].action).invoke()
            requireNotNull(male.config[SemanticsActions.OnClick].action).invoke()
            requireNotNull(native.config[SemanticsActions.OnClick].action).invoke()
            assertEquals(listOf(EdgeVoice.HoaiMy, EdgeVoice.NamMinh), selected)
            assertEquals(1, system)
            assertTrue(all.any { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text.contains("cần Internet") } == true })
        } finally { scene.close(); Locale.setDefault(previous) }
    }
}
