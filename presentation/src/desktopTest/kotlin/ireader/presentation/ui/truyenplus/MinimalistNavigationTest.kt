package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class MinimalistNavigationTest {
    @Test fun legacyAppearancesBecomeNeutralAndSystemFollowsDevice() {
        assertEquals(ReaderAppearanceMode.Dark, ReaderAppearanceMode.fromId("violet"))
        assertEquals(ReaderAppearanceMode.Dark, ReaderAppearanceMode.fromId("graphite"))
        assertEquals(ReaderAppearanceMode.Light, ReaderAppearanceMode.fromId("pearl"))
        assertEquals(ReaderAppearanceMode.Light, ReaderAppearanceMode.fromId("amber"))
        assertEquals(ReaderAppearanceMode.Light, ReaderAppearanceMode.fromId("minimalist"))
        assertEquals(ReaderAppearanceMode.System, ReaderAppearanceMode.fromId("unknown"))
        assertFalse(ReaderAppearanceMode.System.isDark(false))
        assertTrue(ReaderAppearanceMode.System.isDark(true))
        assertEquals(listOf("system", "light", "dark"), ReaderAppearanceMode.entries.map { it.id })
    }
    @Test fun librarySearchRequestsFocusAndEditsRealQuery() = kotlinx.coroutines.runBlocking {
        var acknowledged=false
        val query=mutableStateOf("")
        val scene=ImageComposeScene(width=390,height=844,coroutineContext=coroutineContext) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ReaderLibraryScreen(emptyList(),emptyList(),0,query.value,false,false,emptySet(),
                    {query.value=it},{},{},{},{},{},{},{},requestSearch=true,onSearchFocused={acknowledged=true}) }
            }
        }
        try {
            repeat(8) {scene.render(it*500_000_000L);kotlinx.coroutines.yield()}
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
            val field=all.first { it.config.getOrNull(SemanticsActions.SetText)!=null }
            assertEquals(true,field.config.getOrNull(SemanticsProperties.Focused))
            assertTrue(acknowledged)
            requireNotNull(field.config.getOrNull(SemanticsActions.SetText)?.action).invoke(androidx.compose.ui.text.AnnotatedString("Vạn Cổ"))
            assertEquals("Vạn Cổ",query.value)
        } finally {scene.close()}
    }
    @Test fun homeContinueActionOpensMostRecentlyReadBook() {
        var opened:Long?=null
        val books=listOf(ReaderBookUi(1,"Older",read=2,total=10,lastRead=10),ReaderBookUi(2,"Recent",read=3,total=12,lastRead=20))
        val scene=ImageComposeScene(width=390,height=844) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ReaderHomeScreen(books,false,{opened=it},{},{},{}) }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
            val action=all.first { it.config.getOrNull(SemanticsProperties.TestTag)=="home-continue" }
            requireNotNull(action.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            assertEquals(2L,opened)
        } finally { scene.close() }
    }
    @Test fun searchIsSeparateAndAllTabsRemainReachableAtSmallAndLargeFontSizes() {
        for ((width, scale) in listOf(280 to 2f, 360 to 1f, 800 to 1f)) {
            var searched = 0
            val selected = mutableStateOf(ReaderDestination.Home)
            val scene = ImageComposeScene(width=width, height=180, density=Density(1f,scale)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(), LocalReaderViewport provides ReaderViewport(width.dp,180.dp,scale)) {
                    MaterialTheme { ReaderBottomNav(selected.value, { selected.value=it }, onSearch={ searched++ }) }
                }
            }
            try {
                fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
                fun all() = scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
                scene.render(0);scene.render(1_000_000_000)
                for ((i,destination) in ReaderAppScope.destinations.withIndex()) {
                    val target=all().first { it.config.getOrNull(SemanticsProperties.TestTag)=="nav-${destination.name}" }
                    assertTrue(target.boundsInRoot.width>=48f && target.boundsInRoot.height>=48f)
                    assertTrue(target.boundsInRoot.left>=0 && target.boundsInRoot.right<=width)
                    requireNotNull(target.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                    scene.render((i+2)*1_000_000_000L);scene.render((i+2)*1_000_000_000L+500_000_000)
                    assertEquals(destination,selected.value)
                    val active=all().first { it.config.getOrNull(SemanticsProperties.TestTag)=="nav-${destination.name}" }
                    assertEquals(true,active.config.getOrNull(SemanticsProperties.Selected))
                }
                val search=all().first { it.config.getOrNull(SemanticsProperties.TestTag)=="nav-search" }
                assertTrue(search.boundsInRoot.width>=48f && search.boundsInRoot.height>=48f)
                assertNull(search.config.getOrNull(SemanticsProperties.Selected))
                requireNotNull(search.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                assertEquals(1,searched)
                assertEquals(ReaderDestination.Settings,selected.value,"Search must have its own action")
            } finally { scene.close() }
        }
    }
}
