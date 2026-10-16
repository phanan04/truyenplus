package ireader.presentation.ui.component.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable

@Composable
fun ISnackBarHost(snackBarHostState: androidx.compose.material3.SnackbarHostState)  : SnackbarHostState {
    ireader.presentation.ui.truyenplus.ReaderGlassSnackbarHost(snackBarHostState)
    return  snackBarHostState
}
