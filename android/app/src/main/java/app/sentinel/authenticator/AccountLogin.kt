package app.sentinel.authenticator

import android.net.Uri
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AccountIdentity(val uid:String,val email:String?)
/** Account authentication is separate from the device-protected OTP vault. */
class AccountLogin(private val activity:MainActivity) {
 private val config=runCatching {JSONObject(activity.assets.open("auth-config.json").bufferedReader().use {it.readText()})}.getOrElse {JSONObject()}
 private val settings=config.optJSONObject("firebase") ?: JSONObject()
 val configured:Boolean get()=settings.optString("apiKey").isNotBlank()&&settings.optString("appId").isNotBlank()&&settings.optString("projectId").isNotBlank()
 private val auth:FirebaseAuth?=if(configured) {
  val app=FirebaseApp.getApps(activity).find {it.name=="sentinel-account"} ?: FirebaseApp.initializeApp(activity,FirebaseOptions.Builder().setApplicationId(settings.getString("appId")).setApiKey(settings.getString("apiKey")).setProjectId(settings.getString("projectId")).build(),"sentinel-account")
  FirebaseAuth.getInstance(app)
 } else null
 private fun service():FirebaseAuth=requireNotNull(auth) {"Firebase account setup is unavailable. Use the local vault or demo."}
 fun currentUser():AccountIdentity?=auth?.currentUser?.takeIf {it.isEmailVerified}?.let {AccountIdentity(it.uid,it.email)}
 fun email(email:String,password:String,signup:Boolean,name:String,onResult:(String?)->Unit) {
  activity.lifecycleScope.launch {try {
   val client=service()
   if(signup) {
    val user=requireNotNull(client.createUserWithEmailAndPassword(email,password).waitResult().user)
    try {user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).waitResult();user.sendEmailVerification().waitResult()} finally {client.signOut()}
    onResult("Check your inbox to verify your email, then sign in.")
   } else {
    val user=requireNotNull(client.signInWithEmailAndPassword(email,password).waitResult().user)
    if(!user.isEmailVerified) {client.signOut();onResult("Verify your email before signing in. Check your inbox.")} else {activity.acceptAccount(AccountIdentity(user.uid,user.email));onResult(null)}
   }
  }catch(e:Exception){onResult(friendly(e))}}
 }
 suspend fun google(onResult:(String?)->Unit) {
  try {
   val client=service();require(config.optBoolean("googleEnabled")) {"Google sign-in is not enabled yet."}
   val clientId=config.optString("googleWebClientId");require(clientId.endsWith(".apps.googleusercontent.com")) {"Google sign-in configuration is incomplete."}
   val request=GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build()).build()
   val credential=CredentialManager.create(activity).getCredential(activity,request).credential
   require(credential is CustomCredential && credential.type in setOf(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)) {"Google did not return a valid sign-in credential."}
   val token=GoogleIdTokenCredential.createFrom(credential.data).idToken
   val user=requireNotNull(client.signInWithCredential(GoogleAuthProvider.getCredential(token,null)).waitResult().user)
   require(user.isEmailVerified) {"Google account email could not be verified."}
   activity.acceptAccount(AccountIdentity(user.uid,user.email));onResult(null)
  }catch(e:Exception){onResult(friendly(e))}
 }
 fun apple(onResult:(String?)->Unit) {
  if(!config.optBoolean("appleEnabled")) {onResult("Apple sign-in needs an Apple developer account configuration. Google and email are available.");return}
  activity.lifecycleScope.launch {try {
   val client=service();val provider=OAuthProvider.newBuilder("apple.com").setScopes(listOf("email","name")).build()
   val user=requireNotNull((client.pendingAuthResult ?: client.startActivityForSignInWithProvider(activity,provider)).waitResult().user)
   require(user.isEmailVerified);activity.acceptAccount(AccountIdentity(user.uid,user.email));onResult(null)
  }catch(e:Exception){onResult(friendly(e))}}
 }
 // Firebase verification and reset emails use Firebase's hosted action page.
 fun handleRedirect(uri:Uri?) {}
 fun reset(email:String,onResult:(String)->Unit) {activity.lifecycleScope.launch {try {service().sendPasswordResetEmail(email).waitResult();onResult("If an account exists, check its email and finish the reset using the link.")}catch(e:Exception){onResult(friendly(e))}}}
 fun updatePassword(password:String,onResult:(String?)->Unit) {activity.lifecycleScope.launch {try {val user=requireNotNull(service().currentUser) {"Sign in again before changing your account password."};user.updatePassword(password).waitResult();signOut();activity.resetAccountPassword=false;activity.showLogin=true;onResult("Account password updated. Sign in again.")}catch(e:Exception){onResult(friendly(e))}}}
 fun signOut() {auth?.signOut();activity.lifecycleScope.launch {runCatching {CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())}}}
 private fun friendly(e:Exception):String=when(e) {
  is GetCredentialCancellationException->"Google sign-in was cancelled."
  is NoCredentialException->"Add a Google account in Android Settings, then try again."
  is FirebaseAuthException->when(e.errorCode) {
   "ERROR_INVALID_CREDENTIAL","ERROR_WRONG_PASSWORD","ERROR_USER_NOT_FOUND"->"Email or password is incorrect."
   "ERROR_EMAIL_ALREADY_IN_USE"->"Could not create this account. Try signing in or resetting the password."
   "ERROR_TOO_MANY_REQUESTS"->"Too many attempts. Please wait and try again."
   "ERROR_REQUIRES_RECENT_LOGIN"->"Sign in again before changing your account password."
   else->"Could not complete account sign-in. Check your connection and try again."
  }
  else->if(e is IllegalArgumentException||e is IllegalStateException)e.message ?: "Sign-in could not be completed." else "Cannot connect to account sign-in. Check your connection and try again."
 }
}
private suspend fun <T> Task<T>.waitResult():T=suspendCancellableCoroutine {continuation->
 addOnSuccessListener {if(continuation.isActive)continuation.resume(it)}
 addOnFailureListener {if(continuation.isActive)continuation.resumeWithException(it)}
 addOnCanceledListener {continuation.cancel()}
}
