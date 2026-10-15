package ireader.presentation.ui.truyenplus

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable actual fun rememberReaderBlurSupported(): Boolean {
    val context=LocalContext.current
    return remember(context) {
        Build.VERSION.SDK_INT>=32 && (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice==false
    }
}
