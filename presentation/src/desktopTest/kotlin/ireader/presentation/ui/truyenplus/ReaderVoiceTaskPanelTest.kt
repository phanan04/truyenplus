package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderVoiceTaskPanelTest {
    @Test fun aSystemVoiceTaskUsesTheSameFrostAndLeavesPlaybackAtTheBottom() {
        ImageComposeScene(360,800,Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme(colorScheme=lightColorScheme()) {ReaderBackdrop {
                    ReaderListeningDock(false,{}, {ReaderCollapsedPlayer(false,{},{},{},{},onPrevious={},onNext={})},overlay={
                        ReaderFloatingTaskPanel("Giọng hệ thống",{}) {Text("Giọng tiếng Việt")}
                    })
                }}
            }
        }.use {s ->
            s.render(0);s.render(1_000_000_000)
            fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
            val nodes=s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
            val panel=nodes.first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-floating-task"}.boundsInRoot
            val player=nodes.first {it.config.getOrNull(SemanticsProperties.TestTag)=="listen-compact-toggle"}.boundsInRoot
            assertTrue(panel.bottom<=player.top)
            assertTrue(player.bottom>=790)
            assertTrue(nodes.any {it.config.getOrNull(SemanticsProperties.PaneTitle)=="Giọng hệ thống"})
        }
    }
}
