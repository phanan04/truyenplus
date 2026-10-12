package ireader.presentation.core

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.savedstate.read
import ireader.presentation.core.ui.*
import ireader.presentation.ui.truyenplus.ReaderAppScope

private fun NavBackStackEntry.stringArg(key: String) = arguments?.read { getStringOrNull(key) } ?: savedStateHandle.get<String>(key)

/** The shipped app has no source, update, account, plugin-store or community graph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonNavHost(navController: NavHostController, startDestination: String = "main", additionalRoutes: (NavGraphBuilder.() -> Unit)? = null) {
    AnimatedNavHost(navController = navController, startDestination = startDestination.takeIf(ReaderAppScope::allows) ?: "main",
        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background) {
        composable("main") { MainStarterScreen() }
        composable("bookDetail/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { entry ->
            entry.stringArg("bookId")?.toLongOrNull()?.let { BookDetailScreenSpec(it).Content() }
        }
        composable("reader/{bookId}/{chapterId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType }, navArgument("chapterId") { type = NavType.StringType })) { entry ->
            val book = entry.stringArg("bookId")?.toLongOrNull()
            val chapter = entry.stringArg("chapterId")?.toLongOrNull()
            if (book != null && chapter != null) ModuleLoadingGuard(loadingMessage = "Đang mở sách…") { ReaderScreenSpec(book, chapter).Content() }
        }
        composable(NavigationRoutes.TTS_V2, arguments = listOf("bookId", "chapterId", "sourceId", "readingParagraph").map { key -> navArgument(key) { type = NavType.StringType } }) { entry ->
            val book = entry.stringArg("bookId")?.toLongOrNull()
            val chapter = entry.stringArg("chapterId")?.toLongOrNull()
            val source = entry.stringArg("sourceId")?.toLongOrNull()
            val paragraph = entry.stringArg("readingParagraph")?.toIntOrNull() ?: 0
            if (book != null && chapter != null && source != null) ReaderScreenSpec(book, chapter, listenOnOpen = true, listeningParagraph = paragraph).Content()
        }
        composable(NavigationRoutes.category) { CategoryScreenSpec().Content() }
        composable(NavigationRoutes.fontSettings) { FontScreenSpec().Content() }
        composable(NavigationRoutes.ttsEngineManager) { TTSEngineManagerScreenSpec().Content() }
        composable(NavigationRoutes.textReplacement) {
            val vm: ireader.presentation.ui.settings.textreplacement.TextReplacementViewModel = getIViewModel()
            ireader.presentation.ui.settings.textreplacement.TextReplacementScreen(vm = vm)
        }
    }
}
