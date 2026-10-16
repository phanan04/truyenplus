package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.models.entities.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.settings.category.EnhancedCategoryItem
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderCategoryRowTest {
    @Test fun expandedCollectionRulesKeepReadableTextWithFullGlass() {
        for(full in listOf(false,true))for(dark in listOf(false,true)) {
            ImageComposeScene(400,800) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalReaderFullGlass provides full) {
                    MaterialTheme(if(dark)androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()) {
                        ReaderAppBackdrop {EnhancedCategoryItem(category=CategoryWithCount(Category(4,"Stories",0,0),7),
                            rules=listOf(CategoryAutoRule(1,4,CategoryAutoRule.RuleType.GENRE,"Tiên hiệp")),
                            onDelete={},onRename={},onConfigureAutoRules={},onToggleRule={_,_->},onDeleteRule={})}
                    }
                }
            }.use {scene ->
                fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
                fun nodes()=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
                repeat(3){scene.render(it*100_000_000L)}
                val expand=nodes().first {it.config.getOrNull(SemanticsProperties.Text)?.any {"1" in it.text}==true && it.config.contains(SemanticsActions.OnClick)}
                assertTrue(expand.config[SemanticsActions.OnClick].action!!.invoke())
                repeat(6){scene.render((it+3)*100_000_000L)}
                val rule=nodes().first {it.config.getOrNull(SemanticsProperties.Text)?.any {"Tiên hiệp" in it.text}==true}
                val results=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(rule.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results))
                assertTrue(results.single().layoutInput.style.color.alpha>.9f,"Glass made a rule label transparent")
            }
        }
    }

    @Test fun collectionNameKeepsSpaceAtLargeFontAndHasAccessibleReordering() {
        val name="Bộ sưu tập truyện yêu thích có tên dài"
        val scene=ImageComposeScene(320,800,Density(1f,2f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme {EnhancedCategoryItem(category=CategoryWithCount(Category(4,name,0,0),7),rules=emptyList(),
                    onDelete={},onRename={},onConfigureAutoRules={},onToggleRule={_,_->},onDeleteRule={},onMoveUp={},onMoveDown={})}
            }
        }
        try {
            scene.render();scene.render()
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            val title=all.first {it.config.getOrNull(SemanticsProperties.TestTag)=="category-name"}
            assertTrue(title.boundsInRoot.width>=160,"Actions consume most of the name space")
            assertTrue(title.boundsInRoot.left>=0 && title.boundsInRoot.right<=320)
            val menu=all.first {it.config.getOrNull(SemanticsProperties.TestTag)=="category-actions"}
            assertTrue(menu.boundsInRoot.width>=48 && menu.boundsInRoot.height>=48)
            assertTrue(all.any {it.config.getOrNull(SemanticsActions.CustomActions)?.size==2})
        }finally {scene.close()}
    }
}
