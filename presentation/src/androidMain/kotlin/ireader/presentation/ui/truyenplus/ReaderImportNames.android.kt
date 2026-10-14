package ireader.presentation.ui.truyenplus
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
@Composable actual fun rememberReaderImportNameResolver():suspend (String)->String? {
    val context=LocalContext.current.applicationContext
    return remember(context) {{value->withContext(Dispatchers.IO) {
        try {val uri=Uri.parse(value)
            if(uri.scheme=="file")uri.lastPathSegment
            else context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {cursor->
                if(cursor.moveToFirst())cursor.getString(0)?.take(240) else null
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){null}
    }}}
}
