package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.Composable
@Composable expect fun rememberReaderImportNameResolver():suspend (String)->String?
