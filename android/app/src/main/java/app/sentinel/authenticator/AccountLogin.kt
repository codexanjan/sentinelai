package app.sentinel.authenticator

import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

data class AccountIdentity(val uid:String,val email:String?)
class AccountLogin(private val activity:MainActivity) {
    private val config=runCatching { JSONObject(activity.assets.open("auth-config.json").bufferedReader().use { it.readText() }) }.getOrElse { JSONObject() }
    private val url=config.optString("url").trimEnd('/')
    private val apiKey=config.optString("publishableKey")
    private val prefs=activity.getSharedPreferences("auth_encrypted",android.content.Context.MODE_PRIVATE)
    private val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val configured:Boolean get()=url.startsWith("https://")&&apiKey.isNotBlank()
    private fun key():SecretKey {
        val alias="sentinel.account-session.v1"
        if(ks.containsAlias(alias))return ks.getKey(alias,null) as SecretKey
        return KeyGenerator.getInstance("AES","AndroidKeyStore").apply { init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()) }.generateKey()
    }
    private fun stored():JSONObject=runCatching {
        val raw=prefs.getString("session",null) ?: return@runCatching JSONObject()
        val bytes=Base64.decode(raw,Base64.NO_WRAP);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)));c.updateAAD("sentinel-auth-v1".toByteArray());JSONObject(String(c.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8))
    }.getOrElse { JSONObject() }
    private fun store(data:JSONObject) {
        val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());c.updateAAD("sentinel-auth-v1".toByteArray());val encrypted=c.iv+c.doFinal(data.toString().toByteArray());check(prefs.edit().putString("session",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit())
    }
    fun currentUser():AccountIdentity? { val user=stored().optJSONObject("user") ?: return null;val id=user.optString("id");return if(id.matches(Regex("[0-9a-fA-F-]{36}")))AccountIdentity(id,user.optString("email")) else null }
    private fun identity(data:JSONObject):AccountIdentity {
        require(data.optString("access_token").isNotBlank()) { "Sign-in did not return a valid session." }
        val user=data.getJSONObject("user");val id=user.getString("id");require(id.matches(Regex("[0-9a-fA-F-]{36}")));return AccountIdentity(id,user.optString("email"))
    }
    private fun request(path:String,body:JSONObject,method:String="POST",token:String?=null):JSONObject {
        require(configured) { "Supabase is not connected yet. Try demo or local mode." }
        val connection=URL("$url/auth/v1/$path").openConnection() as HttpsURLConnection
        try {
            connection.requestMethod=method;connection.connectTimeout=15000;connection.readTimeout=20000;connection.instanceFollowRedirects=false
            connection.setRequestProperty("apikey",apiKey);connection.setRequestProperty("Content-Type","application/json");token?.let { connection.setRequestProperty("Authorization","Bearer $it") }
            connection.doOutput=true;connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status=connection.responseCode;val stream=if(status in 200..299)connection.inputStream else connection.errorStream
            val text=stream?.bufferedReader()?.use { it.readText().take(1024*1024) }.orEmpty()
            if(status !in 200..299)throw IllegalStateException(when(status){429->"Too many attempts. Wait and try again.";400,401,422->"Could not complete login. Check your email, password, email confirmation and provider setup.";else->"Account service is unavailable. Try again."})
            return if(text.isBlank())JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }
    fun email(email:String,password:String,signup:Boolean,name:String,onResult:(String?)->Unit) {
        activity.lifecycleScope.launch {
            try {
                val data=withContext(Dispatchers.IO) { request(if(signup)"signup" else "token?grant_type=password",JSONObject().put("email",email).put("password",password).apply { if(signup)put("data",JSONObject().put("display_name",name)) }) }
                if(data.optString("access_token").isBlank())onResult("Check your inbox to confirm your email, then sign in.")
                else { val user=identity(data);withContext(Dispatchers.IO){store(data)};activity.acceptAccount(user);onResult(null) }
            }catch(e:Exception){onResult(e.message ?: "Unable to connect. Try again.")}
        }
    }
    private fun pkce(kind:String):String {
        val bytes=ByteArray(32).also { SecureRandom().nextBytes(it) };val verifier=Base64.encodeToString(bytes,Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val data=stored();data.put("pkce_verifier",verifier).put("pkce_kind",kind).put("pkce_created",System.currentTimeMillis());store(data)
        return Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    private val callback="sentinelauth://auth/callback"
    suspend fun google(onResult:(String?)->Unit) { oauth("google",onResult) }
    fun apple(onResult:(String?)->Unit) { oauth("apple",onResult) }
    private fun oauth(provider:String,onResult:(String?)->Unit) {
        try {
            require(configured) { "Supabase is not connected yet. Try demo or local mode." }
            require(config.optBoolean(provider+"Enabled")) { (if(provider=="apple")"Apple" else "Google")+" login awaits provider credentials in Supabase." }
            val challenge=pkce("oauth");val target="$url/auth/v1/authorize?provider=$provider&redirect_to=${URLEncoder.encode(callback,"UTF-8")}&code_challenge=$challenge&code_challenge_method=s256"
            activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(target)));onResult(null)
        }catch(e:Exception){onResult(e.message ?: "Social sign-in is unavailable.")}
    }
    fun handleRedirect(uri:Uri?) {
        if(uri?.scheme!="sentinelauth"||uri.host!="auth"||uri.path!="/callback")return
        val data=stored();val verifier=data.optString("pkce_verifier");val code=uri.getQueryParameter("code")
        if(code.isNullOrBlank()||verifier.isBlank()||System.currentTimeMillis()-data.optLong("pkce_created")>600000){activity.notify("Sign-in callback expired or invalid. Try again.");return}
        activity.lifecycleScope.launch {
            try {
                val response=withContext(Dispatchers.IO){request("token?grant_type=pkce",JSONObject().put("auth_code",code).put("code_verifier",verifier))}
                val user=identity(response);withContext(Dispatchers.IO){store(response)}
                if(data.optString("pkce_kind")=="recovery"){activity.showLogin=true;activity.resetAccountPassword=true;activity.notify("Choose a new account password.")}
                else activity.acceptAccount(user)
            }catch(e:Exception){activity.notify(e.message ?: "Sign-in could not be completed.")}
            finally { val fresh=stored();fresh.remove("pkce_verifier");fresh.remove("pkce_kind");fresh.remove("pkce_created");runCatching { store(fresh) } }
        }
    }
    fun reset(email:String,onResult:(String)->Unit) {
        activity.lifecycleScope.launch { try { val challenge=pkce("recovery");withContext(Dispatchers.IO){request("recover?redirect_to=${URLEncoder.encode(callback,"UTF-8")}",JSONObject().put("email",email).put("code_challenge",challenge).put("code_challenge_method","s256"))};onResult("If an account exists, check its email for a reset link. Open it on this device.") }catch(e:Exception){onResult(e.message ?: "Cannot send reset link.")} }
    }
    fun updatePassword(password:String,onResult:(String?)->Unit) {
        activity.lifecycleScope.launch {try { val token=stored().optString("access_token");require(token.isNotBlank());withContext(Dispatchers.IO){request("user",JSONObject().put("password",password),"PUT",token)};signOut();activity.resetAccountPassword=false;activity.showLogin=true;onResult("Password updated. Sign in again.") }catch(e:Exception){onResult(e.message ?: "Password could not be updated.")} }
    }
    fun signOut() { val token=stored().optString("access_token");prefs.edit().clear().commit();if(token.isNotBlank())activity.lifecycleScope.launch { withContext(Dispatchers.IO){runCatching { request("logout",JSONObject(),token=token) }} } }
}
