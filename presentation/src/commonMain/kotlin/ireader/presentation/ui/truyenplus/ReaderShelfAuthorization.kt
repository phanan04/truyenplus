package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.Composable
/** Explicit UI consent only; background catalog code never launches a prompt. */
@Composable expect fun rememberShelfAuthorization(onSuccess:()->Unit,onFailure:(String)->Unit):()->Unit
