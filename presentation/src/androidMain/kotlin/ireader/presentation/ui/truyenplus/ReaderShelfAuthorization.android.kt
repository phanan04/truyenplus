package ireader.presentation.ui.truyenplus
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.Identity
import ireader.data.shelf.AndroidShelfAccess
@Composable actual fun rememberShelfAuthorization(onSuccess:()->Unit,onFailure:(String)->Unit):()->Unit {
 val context=LocalContext.current
 val success=rememberUpdatedState(onSuccess);val failure=rememberUpdatedState(onFailure)
 val alive=remember { booleanArrayOf(true) }
 DisposableEffect(Unit){onDispose{alive[0]=false}}
 val client=remember(context) { Identity.getAuthorizationClient(context) }
 var busy by remember { mutableStateOf(false) }
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
  busy=false
  if(result.resultCode!=android.app.Activity.RESULT_OK)failure.value("cancelled")
  else try { AndroidShelfAccess.accept(client.getAuthorizationResultFromIntent(result.data));success.value() }catch(_:Exception){failure.value("authentication")}
 }
 return {
  if(!busy && alive[0]) {
   busy=true
   client.authorize(AndroidShelfAccess.request()).addOnSuccessListener { result ->
    if(!alive[0])return@addOnSuccessListener
    if(result.hasResolution()) {
     val pending=result.pendingIntent
     if(pending==null){busy=false;failure.value("authentication")}
     else launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
    } else { busy=false;try{AndroidShelfAccess.accept(result);success.value()}catch(_:Exception){failure.value("authentication")} }
   }.addOnFailureListener { e->if(!alive[0])return@addOnFailureListener;busy=false;failure.value(if((e as? com.google.android.gms.common.api.ApiException)?.statusCode==10)"configuration" else "authentication") }
  }
 }
}
