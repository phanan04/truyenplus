package ireader.presentation.ui.truyenplus

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable actual fun rememberReaderBlurSupported(): Boolean {
    val context=LocalContext.current
    val full=LocalReaderFullGlass.current
    return remember(context,full) {
        readerGlassCaptureAllowed(Build.VERSION.SDK_INT,
            (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice!=false,full)
    }
}
