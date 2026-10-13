package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
@Composable actual fun rememberShelfAuthorization(onSuccess:()->Unit,onFailure:(String)->Unit):()->Unit {
 val failure=rememberUpdatedState(onFailure)
 return { failure.value("unsupported") }
}
