package ireader.data.shelf

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import ireader.domain.models.shelf.*
import org.koin.core.component.inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AndroidShelfAccess:ShelfAccess,org.koin.core.component.KoinComponent {
 private val context:android.content.Context by inject()
 const val READ_SCOPE="https://www.googleapis.com/auth/drive.readonly"
 @Volatile private var accessToken:String?=null
 @Volatile private var expiresAt=0L
 private val tokenScope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+kotlinx.coroutines.Dispatchers.IO)
 @Volatile private var clearing:kotlinx.coroutines.Job?=null
 fun request():AuthorizationRequest=AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(READ_SCOPE))).build()
 fun accept(result:AuthorizationResult) {
  val value=result.accessToken?.takeIf { it.isNotBlank() }?:throw ShelfException(ShelfFailure.Authentication)
  accessToken=value;expiresAt=System.currentTimeMillis()+45*60_000
 }
 override fun invalidate(){
  val token=accessToken;accessToken=null;expiresAt=0
  if(token!=null) clearing=tokenScope.launch { try { com.google.android.gms.auth.GoogleAuthUtil.clearToken(context,token) }catch(_:Exception){} }
 }
 override suspend fun token():String {
  clearing?.join()
  accessToken?.takeIf { System.currentTimeMillis()<expiresAt }?.let { return it }
  return suspendCancellableCoroutine { c ->
   Identity.getAuthorizationClient(context).authorize(request()).addOnSuccessListener { result ->
    if(!c.isActive)return@addOnSuccessListener
    if(result.hasResolution()) c.resumeWithException(ShelfException(ShelfFailure.Authentication))
    else try { accept(result);c.resume(requireNotNull(accessToken)) }catch(e:Exception){c.resumeWithException(e)}
   }.addOnFailureListener { if(c.isActive)c.resumeWithException(ShelfException(ShelfFailure.Authentication)) }
  }
 }
}
actual fun createShelfAccess():ShelfAccess=AndroidShelfAccess
