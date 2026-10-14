package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderUpdateActionTest {
    @Test fun longReleaseNotesCannotPushDownloadOrInstallBelowTheScreen() {
        val asset=ReaderUpdateAsset("arm64-v8a","https://example.com/app.apk","a".repeat(64),120_000_000)
        val update=ReaderAvailableUpdate(ReaderUpdateManifest(97,"2.0.43","app",notes="Nội dung cập nhật. ".repeat(500),assets=listOf(asset)),asset)
        val strings=LocalizeHelper()
        for(stage in listOf(ReaderUpdateStage.Available,ReaderUpdateStage.Ready))ImageComposeScene(360,640) {
            CompositionLocalProvider(LocalLocalizeHelper provides strings) {MaterialTheme {
                ReaderUpdateContent("2.0.42","https://example.com/update.json",true,ReaderUpdateState(stage,update),{},{},{},{})
            }}
        }.use {s ->
            s.render(0);s.render(1_000_000_000)
            fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
            val label=strings.localize(if(stage==ReaderUpdateStage.Ready)Res.string.tp_install_update else Res.string.tp_download_update)
            val action=s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsActions.OnClick)!=null && it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==label}==true}
            assertTrue(action.boundsInRoot.height>=48 && action.boundsInRoot.bottom<=640,"Primary update action below long notes: ${action.boundsInRoot}")
        }
    }
}
