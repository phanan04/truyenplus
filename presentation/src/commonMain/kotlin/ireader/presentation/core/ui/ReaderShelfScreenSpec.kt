package ireader.presentation.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ireader.presentation.core.LocalNavigator
import ireader.presentation.core.navigateTo
import ireader.presentation.core.safePopBackStack
import ireader.presentation.ui.truyenplus.*

/** A separate destination; the library contains only books already imported. */
class ReaderShelfScreenSpec {
    @Composable fun Content() {
        val navigator = requireNotNull(LocalNavigator.current)
        ReaderBackdrop {
            CompositionLocalProvider(LocalReaderDockInset provides 24.dp) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    ReaderContentPane(maxWidth = 900.dp) {
                        ReaderShelfRoute(onLibrary = { navigator.safePopBackStack() },
                            onBook = { navigator.navigateTo(BookDetailScreenSpec(it)) })
                    }
                }
            }
        }
    }
}
