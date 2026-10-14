package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.*
import java.io.File
import java.net.URI
import kotlinx.coroutines.*
@Composable actual fun rememberReaderImportNameResolver():suspend (String)->String? = remember {{value->withContext(Dispatchers.IO) {
    runCatching {if(value.startsWith("file:"))File(URI(value)).name else File(value).name}.getOrNull()?.take(240)
}}}
