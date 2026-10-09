package app.sentinel.authenticator

import android.Manifest
import android.app.KeyguardManager
import android.content.*
import android.hardware.biometrics.BiometricPrompt
import android.os.*
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import app.sentinel.authenticator.core.*
import app.sentinel.authenticator.data.VaultStore
import kotlinx.coroutines.*

class MainActivity:ComponentActivity() {
    private lateinit var vault:VaultStore
    lateinit var accountLogin:AccountLogin
    var startupReady by mutableStateOf(false)
    var startupError by mutableStateOf(false)
    var smartCopy by mutableStateOf(true)
    var copyWaitingId by mutableStateOf<String?>(null)
    private var copyJob:Job?=null
    var showLogin by mutableStateOf(true)
    var resetAccountPassword by mutableStateOf(false)
    var demoMode by mutableStateOf(false)
    var accountEmail by mutableStateOf("")
    var unlocked by mutableStateOf(false); private set
    var accounts by mutableStateOf<List<Account>>(emptyList());private set
    var busy by mutableStateOf(false);private set
    var route by mutableStateOf("home")
    var candidate by mutableStateOf<Account?>(null)
    var restorePreview by mutableStateOf<List<Account>?>(null)
    var onboarding by mutableStateOf(true)
    var dark by mutableStateOf(true)
    var recoveryMode by mutableStateOf(false);private set
    private var pendingExport:ByteArray?=null
    private var pendingRestore:ByteArray?=null
    private var authentication:CancellationSignal?=null
    private val exportPicker=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val encrypted=pendingExport;pendingExport=null
        if(uri!=null && encrypted!=null) lifecycleScope.launch {
            try { withContext(Dispatchers.IO) {
                val output=contentResolver.openOutputStream(uri,"wt") ?: error("No output")
                output.use { it.write(encrypted);it.flush() }
            };notify("Encrypted backup saved. Keep its password safe.") }
            catch(_:Exception) { notify("Backup was not saved. Try another location.") }
        }
    }
    private val restorePicker=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) lifecycleScope.launch {
            try {
                pendingRestore=withContext(Dispatchers.IO) {
                    val input=contentResolver.openInputStream(uri) ?: error("No input")
                    input.use {
                        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
                        while(true) { val n=it.read(buffer);if(n<0)break;total+=n;require(total<=Payload.MAX_FILE);out.write(buffer,0,n) }
                        out.toByteArray()
                    }
                }
                authenticate(after={route="restore"},onUnavailable={authenticateRecovery()})
            } catch(_:Exception) { pendingRestore=null;notify("Cannot read that backup. Choose a valid file under 10 MB.") }
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { SentinelTheme(dark) { AuthenticatorApp(this) } }
        lifecycleScope.launch {
            try {
                val login=withContext(Dispatchers.IO) { AccountLogin(this@MainActivity) }
                val current=withContext(Dispatchers.IO) { login.currentUser() }
                val opened=withContext(Dispatchers.IO) { VaultStore(this@MainActivity,current?.uid ?: "local") }
                accountLogin=login;vault=opened;accountEmail=current?.email ?: ""
                val prefs=getSharedPreferences("public_settings",MODE_PRIVATE)
                showLogin=current==null && !prefs.getBoolean("local_mode",false)
                onboarding=!prefs.getBoolean("onboarded",false);dark=prefs.getBoolean("dark",true)
                startupReady=true
                accountLogin.handleRedirect(intent?.data)
            } catch(_:Exception) { startupError=true }
        }
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);setIntent(intent);if(::accountLogin.isInitialized)accountLogin.handleRedirect(intent.data) }
    override fun onStop() {
        lock();super.onStop()
    }
    fun finishOnboarding() {
        onboarding=false;getSharedPreferences("public_settings",MODE_PRIVATE).edit().putBoolean("onboarded",true).apply()
    }
    fun toggleTheme() {
        dark=!dark;getSharedPreferences("public_settings",MODE_PRIVATE).edit().putBoolean("dark",dark).apply()
    }
    fun continueLocal() { lock();demoMode=false;accountEmail="";vault=VaultStore(this);showLogin=false;getSharedPreferences("public_settings",MODE_PRIVATE).edit().putBoolean("local_mode",true).apply() }
    fun acceptAccount(user:AccountIdentity) { lock();demoMode=false;vault=VaultStore(this,user.uid);accountEmail=user.email ?: "Signed in";showLogin=false;getSharedPreferences("public_settings",MODE_PRIVATE).edit().putBoolean("local_mode",false).apply() }
    fun signOutAccount() { accountLogin.signOut();lock();demoMode=false;accountEmail="";showLogin=true;getSharedPreferences("public_settings",MODE_PRIVATE).edit().putBoolean("local_mode",false).apply() }
    fun startDemo() { lock();demoMode=true;window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);showLogin=false;accountEmail="demo@sentinel.example";accounts=listOf("Google","GitHub","Design workspace").map { Account(issuer=it,label="demo@example.test",secret=Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"),folder=if(it=="Google")"Personal" else "Work",favorite=it=="Google",recoverySaved=it!="Design workspace") };unlocked=true;route="home";notify("Demo uses public test keys. Do not add real secrets.") }
    fun deviceSecure()=getSystemService(KeyguardManager::class.java).isDeviceSecure
    fun openSecuritySettings() { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
    fun lock() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);copyJob?.cancel();copyJob=null;copyWaitingId=null
        authentication?.cancel();authentication=null
        if(::vault.isInitialized)vault.lock()
        accounts.forEach { it.secret.fill(0) };candidate?.secret?.fill(0);restorePreview?.forEach { it.secret.fill(0) }
        accounts=emptyList();candidate=null;restorePreview=null;unlocked=false;route="home";busy=false;recoveryMode=false
        if(demoMode) { demoMode=false;showLogin=true;accountEmail="" }
    }
    fun authenticate(after:()->Unit={},onUnavailable:(()->Unit)?=null) {
        if(busy)return
        if(!deviceSecure()) { notify("Set a PIN, pattern or password in device settings first.");return }
        val cipher=try { vault.authCipher() } catch(_:Exception) { if(onUnavailable!=null)onUnavailable() else notify("Device protection is unavailable. Keep the original vault and restore a backup.");return }
        val token=vault.token();val signal=CancellationSignal();authentication=signal
        val prompt=BiometricPrompt.Builder(this).setTitle("Unlock Sentinel")
            .setSubtitle("Use your fingerprint, face or device screen lock")
            .setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        busy=true
        prompt.authenticate(BiometricPrompt.CryptoObject(cipher),signal,mainExecutor,object:BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) {
                authentication=null
                lifecycleScope.launch {
                    try {
                        val records=withContext(Dispatchers.IO) { vault.open(result.cryptoObject!!.cipher!!,token) }
                        if(vault.token()==token) { accounts.forEach { it.secret.fill(0) };accounts=records;unlocked=true;busy=false;after() }
                        else { records.forEach { it.secret.fill(0) };busy=false }
                    } catch(_:Exception) { busy=false;if(onUnavailable!=null)onUnavailable() else notify("Cannot unlock this vault. Restore a verified encrypted backup if the device key was lost.") }
                }
            }
            override fun onAuthenticationError(errorCode:Int,errString:CharSequence) { authentication=null;busy=false }
        })
    }
    private fun authenticateRecovery() {
        if(!deviceSecure()) { notify("Set a secure device screen lock before restoring.");return }
        val cipher=try { vault.recoveryCipher() } catch(_:Exception) { notify("Device protection is unavailable.");return }
        val token=vault.token();val signal=CancellationSignal();authentication=signal;busy=true
        BiometricPrompt.Builder(this).setTitle("Protect restored vault").setSubtitle("Confirm your device screen lock to restore your backup")
            .setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
            .authenticate(BiometricPrompt.CryptoObject(cipher),signal,mainExecutor,object:BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) {
                    authentication=null
                    try { vault.openRecovery(result.cryptoObject!!.cipher!!,token);accounts=emptyList();unlocked=true;recoveryMode=true;route="restore" }
                    catch(_:Exception) { notify("Recovery unlock failed. Try again.") }
                    finally { busy=false }
                }
                override fun onAuthenticationError(errorCode:Int,errString:CharSequence) { authentication=null;busy=false }
            })
    }
    fun leaveFlow() {
        if(recoveryMode) { lock();return }
        candidate?.secret?.fill(0);candidate=null;restorePreview?.forEach { it.secret.fill(0) };restorePreview=null;route="home"
    }
    fun mutation(next:List<Account>,after:()->Unit={route="home"}) {
        if(demoMode) { notify("Demo is read-only. Use your own vault for changes.");return }
        if(!unlocked||busy || (recoveryMode && route!="restore-review"))return
        val token=vault.token();busy=true
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { vault.save(next) }
                if(vault.token()==token && unlocked) { accounts.filter { old -> next.none { it.secret===old.secret } }.forEach { it.secret.fill(0) };accounts=next;after() }
            } catch(_:Exception) { notify("Change was not saved. Check available storage and unlock again.") }
            finally { busy=false }
        }
    }
    fun saveCandidate() {
        val a=candidate ?: return
        if(accounts.size>=1000) { notify("This vault supports up to 1,000 accounts.");return }
        mutation(accounts+a) { candidate=null;route="home";notify("Account added. Finish verification on the service using its code.") }
    }
    fun copyCode(a:Account) {
        if(!unlocked)return
        if(copyWaitingId!=null)return
        val token=vault.token()
        copyJob=lifecycleScope.launch {
        try {
        val wait=if(smartCopy)Readiness.waitMillis(a,System.currentTimeMillis()/1000) else 0L
        if(wait>0) { copyWaitingId=a.id;notify("Waiting for a fresh code. Keep Sentinel open.");delay(wait) }
        if(!unlocked || vault.token()!=token)return@launch
        val value=Otp.code(a,System.currentTimeMillis()/1000)
        val clipboard=getSystemService(ClipboardManager::class.java)
        val clip=ClipData.newPlainText("Verification code",value)
        val clipboardToken=java.util.UUID.randomUUID().toString()
        clip.description.extras=PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE",true);putString("sentinel.copy.id",clipboardToken) }
        clipboard.setPrimaryClip(clip);notify("Code copied")
        lifecycleScope.launch { delay(30000)
            if(clipboard.primaryClip?.description?.extras?.getString("sentinel.copy.id")==clipboardToken && clipboard.primaryClip?.getItemAt(0)?.text?.toString()==value) clipboard.clearPrimaryClip()
        }
        } finally { copyWaitingId=null }
        }
    }
    fun prepareExport(password:String) {
        if(demoMode) { notify("Demo backups are disabled.");return }
        if(!unlocked||busy)return
        val snapshot=accounts;val token=vault.token();busy=true
        lifecycleScope.launch {
            val chars=password.toCharArray()
            try {
                val encrypted=withContext(Dispatchers.Default) { PortableBackup.export(snapshot,chars) }
                if(unlocked && vault.token()==token) {
                    pendingExport=encrypted;exportPicker.launch("sentinel-backup-${java.time.LocalDate.now()}.sentinel")
                }
            } catch(_:Exception) { notify("Could not prepare backup. Unlock and try again.") }
            finally { chars.fill('\u0000');busy=false }
        }
    }
    fun selectRestore() { if(demoMode){notify("Exit demo before restoring.");return};restorePicker.launch(arrayOf("application/octet-stream","*/*")) }
    fun decryptRestore(password:String) {
        if(!unlocked||busy)return
        val encrypted=pendingRestore ?: run { notify("Select a backup file first.");return }
        val token=vault.token();busy=true
        lifecycleScope.launch {
            val chars=password.toCharArray()
            try {
                val records=withContext(Dispatchers.Default) { PortableBackup.restore(encrypted,chars) }
                if(unlocked && vault.token()==token) { restorePreview=records;route="restore-review" }
                else records.forEach { it.secret.fill(0) }
            } catch(_:Exception) { notify("Cannot open backup. Check the password and file.") }
            finally { chars.fill('\u0000');busy=false }
        }
    }
    fun mergeRestore() {
        val imported=restorePreview ?: return
        val next=accounts.toMutableList()
        imported.forEach { record ->
            if(next.none { it.secret.contentEquals(record.secret) && it.algorithm==record.algorithm && it.digits==record.digits && it.period==record.period })
                next.add(if(next.any { it.id==record.id }) record.copy(id=java.util.UUID.randomUUID().toString()) else record)
        }
        if(next.size>1000) { notify("Merge would exceed the 1,000 account limit.");return }
        mutation(next) { restorePreview=null;pendingRestore=null;recoveryMode=false;route="home";notify("Backup restored. Test your accounts before deleting the original.") }
    }
    fun notify(message:String) { Toast.makeText(this,message,Toast.LENGTH_LONG).show() }
}
