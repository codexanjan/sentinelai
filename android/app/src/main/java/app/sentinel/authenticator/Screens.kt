package app.sentinel.authenticator

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.sentinel.authenticator.core.*
import kotlinx.coroutines.delay

@Composable fun SentinelTheme(dark:Boolean,content:@Composable ()->Unit) {
    val colors=if(dark) darkColorScheme(primary=Color(0xFFBCACFF),onPrimary=Color(0xFF281656),secondary=Color(0xFF91F5CC),background=Color(0xFF0C0E1A),surface=Color(0xFF0C0E1A),surfaceVariant=Color(0xFF1A1D32),onSurfaceVariant=Color(0xFFC1C5E0))
        else lightColorScheme(primary=Color(0xFF6547D1),secondary=Color(0xFF087953),background=Color(0xFFF7F5FF),surface=Color(0xFFF7F5FF),surfaceVariant=Color(0xFFECE7FF))
    MaterialTheme(colorScheme=colors,content=content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AuthenticatorApp(activity:MainActivity) {
    Surface(Modifier.fillMaxSize()) {
        if(!activity.startupReady) { StartupIntro(activity);return@Surface }
        if(activity.onboarding) { Onboarding(activity);return@Surface }
        if(activity.showLogin) { LoginScreen(activity);return@Surface }
        if(!activity.unlocked) { Locked(activity);return@Surface }
        BackHandler(activity.route!="home") { activity.leaveFlow() }
        Scaffold(
            topBar={ TopAppBar(title={Column { Text(if(activity.route=="home")"Sentinel" else when(activity.route) {
                "readiness"->"Recovery studio";"settings"->"Settings";"manual"->"Add account";"scan"->"Scan setup QR";"review"->"Review account"
                "export"->"Encrypted backup";"restore"->"Open backup";"restore-review"->"Restore accounts";else->"Authenticator"
            },fontWeight=FontWeight.Bold);if(activity.route=="home")Text("Your accounts, protected.",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)} },
                navigationIcon={if(activity.route!="home")IconButton(onClick={activity.leaveFlow()}) { Icon(Icons.Outlined.ArrowBack,"Back") }},
                actions={if(activity.route=="home") { IconButton(onClick={activity.lock()}) { Icon(Icons.Outlined.Lock,"Lock vault") };IconButton(onClick={activity.route="settings"}) { Icon(Icons.Outlined.Settings,"Settings") } }}) },
            floatingActionButton={if(activity.route=="home" && !activity.demoMode)AddActions(activity)}
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when(activity.route) {
                    "home"->Home(activity)
                    "readiness"->RecoveryStudio(activity)
                    "manual"->Manual(activity)
                    "scan"->ScannerScreen(onResult={text ->
                        try { activity.candidate=Provisioning.parse(text);activity.route="review" } catch(e:Exception) { activity.notify(e.message ?: "Unsupported setup code.");activity.route="manual" }
                    },onBack={activity.route="manual"})
                    "review"->Review(activity)
                    "settings"->SettingsScreen(activity)
                    "export"->BackupForm(activity,false)
                    "restore"->BackupForm(activity,true)
                    "restore-review"->RestoreReview(activity)
                }
                if(activity.busy) Surface(Modifier.fillMaxSize().clickable(enabled=true,onClick={}),color=MaterialTheme.colorScheme.surface.copy(alpha=.92f)) {
                    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { CircularProgressIndicator();Spacer(Modifier.height(18.dp));Text("Protecting your vault…") }
                }
            }
        }
    }
}

@Composable private fun Onboarding(activity:MainActivity) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal=24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Outlined.Shield,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(30.dp));Spacer(Modifier.width(10.dp));Text("sentinel.",fontSize=28.sp,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.weight(1f));Text("AUTHENTICATOR",fontSize=10.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            Box(Modifier.fillMaxWidth().height(180.dp).background(Brush.linearGradient(listOf(Color(0xFF30255B),Color(0xFF161A31))),RoundedCornerShape(30.dp)),contentAlignment=Alignment.Center) {
                Box(Modifier.size(130.dp).border(1.dp,Color(0xFF70649A),CircleShape),contentAlignment=Alignment.Center) { Box(Modifier.size(98.dp).background(Color(0xFFB8A6FF),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Outlined.VerifiedUser,null,Modifier.size(60.dp),tint=Color(0xFF261743)) } }
                Surface(Modifier.align(Alignment.BottomEnd).padding(14.dp),shape=RoundedCornerShape(14.dp),color=Color(0xFF91F5CC)) { Text("OFFLINE. ON YOUR TERMS.",Modifier.padding(horizontal=14.dp,vertical=10.dp),color=Color(0xFF093627),fontSize=10.sp,fontWeight=FontWeight.Bold) }
            }
            Text("Your codes.\nYour rules.",fontSize=39.sp,lineHeight=43.sp,fontWeight=FontWeight.ExtraBold)
            Text("A private home for your verification codes. Open your vault in seconds. No account needed for local use.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { listOf("OFFLINE","ENCRYPTED","BACKUP READY").forEach { label -> Surface(Modifier.weight(1f),shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant) { Text(label,Modifier.padding(vertical=12.dp),fontSize=9.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center) } } }
            Spacer(Modifier.height(10.dp))
        }
        Column(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={activity.finishOnboarding();activity.continueLocal()},modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)) { Icon(Icons.Outlined.LockOpen,null);Spacer(Modifier.width(12.dp));Text("Open my vault",fontWeight=FontWeight.Bold) }
            OutlinedButton(onClick={activity.finishOnboarding();activity.startDemo()},modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(16.dp)) { Text("Try demo") }
            TextButton(onClick={activity.finishOnboarding();activity.showLogin=true},modifier=Modifier.fillMaxWidth()) { Text("Sign in / create account") }
        }
    }
}

@Composable private fun Locked(activity:MainActivity) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(128.dp).background(Brush.linearGradient(listOf(Color(0xFF382663),Color(0xFF1B2037))),RoundedCornerShape(34.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Outlined.Shield,null,Modifier.size(70.dp),tint=Color(0xFFB8A6FF)) }
        Spacer(Modifier.height(28.dp));Text("Your private vault",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(12.dp));Text("Unlock with your fingerprint, face or device PIN to see your accounts.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick={if(activity.deviceSecure())activity.authenticate() else activity.openSecuritySettings()},enabled=!activity.busy,modifier=Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Outlined.LockOpen,null);Spacer(Modifier.width(8.dp));Text(if(activity.deviceSecure())"Unlock authenticator" else "Set up device screen lock") }
        if(activity.deviceSecure())TextButton(onClick={activity.openSecuritySettings()}) { Text("Device protection settings") }
        TextButton(onClick={activity.selectRestore()}) { Text("Restore an encrypted backup") }
        Text("Uninstalling may erase your local accounts. Keep a verified backup and your services’ recovery codes.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={activity.startDemo()}) { Text("Try demo without setup") }
        TextButton(onClick={activity.showLogin=true}) { Text("Sign in / create account") }
        if(activity.busy) { Spacer(Modifier.height(16.dp));CircularProgressIndicator() }
    }
}

@Composable private fun StartupIntro(activity:MainActivity) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),verticalArrangement=Arrangement.Center) {
        Icon(Icons.Outlined.Shield,null,Modifier.size(70.dp),tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(28.dp));Text("Your codes.\nYour rules.",fontSize=43.sp,lineHeight=47.sp,fontWeight=FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp));Text("A private space for your next sign-in.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));Text("OFFLINE CODES  ·  FOCUS  ·  RECOVERY",fontSize=11.sp,color=MaterialTheme.colorScheme.secondary)
        if(activity.startupError) { Spacer(Modifier.height(24.dp));Text("Device protection could not open. Your stored vault has not been removed.");Button(onClick={activity.recreate()}) { Text("Try again") } }
    }
}

@Composable private fun AddActions(activity:MainActivity) {
    var expanded by remember { mutableStateOf(false) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted)activity.route="scan" else { activity.route="manual";activity.notify("Camera permission denied. Enter the setup key instead.") } }
    Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(expanded)Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surfaceVariant,shadowElevation=10.dp) {
            Column(Modifier.padding(12.dp)) {
                TextButton(onClick={expanded=false;activity.route="manual"}) { Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(12.dp));Text("Manual setup key") }
                TextButton(onClick={expanded=false;if(ContextCompat.checkSelfPermission(activity,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)activity.route="scan" else permission.launch(Manifest.permission.CAMERA)}) { Icon(Icons.Outlined.QrCodeScanner,null);Spacer(Modifier.width(12.dp));Text("Scan QR code") }
            }
        }
        FloatingActionButton(onClick={expanded=!expanded},shape=RoundedCornerShape(22.dp),containerColor=MaterialTheme.colorScheme.primary) { Icon(if(expanded)Icons.Outlined.VisibilityOff else Icons.Outlined.Add,if(expanded)"Close add options" else "Add account options") }
    }
}

@Composable private fun Home(activity:MainActivity) {
    var query by remember { mutableStateOf("") };var now by remember { mutableLongStateOf(System.currentTimeMillis()/1000) }
    var selected by remember { mutableStateOf<Account?>(null) };var filter by remember { mutableStateOf("All") }
    var focus by remember { mutableStateOf(false) };var focusId by remember { mutableStateOf<String?>(null) }
    var clockShift by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var wall=System.currentTimeMillis();var mono=android.os.SystemClock.elapsedRealtime()
        while(true) { val w=System.currentTimeMillis();val m=android.os.SystemClock.elapsedRealtime();if(Readiness.clockChanged(wall,w,mono,m))clockShift=true;wall=w;mono=m;now=w/1000;delay(1000) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp)) {
        Surface(shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth().padding(bottom=14.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(if(activity.demoMode)"EXPLORE SENTINEL" else "ON YOUR DEVICE",fontSize=10.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(7.dp));Text("Ready for your next sign-in.",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("${activity.accounts.count { it.recoverySaved }} / ${activity.accounts.size} recovery plans marked ready",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick={activity.route="readiness"}) { Icon(Icons.Outlined.VerifiedUser,"Open Recovery studio",Modifier.size(36.dp),tint=MaterialTheme.colorScheme.secondary) }
            }
        }
        OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Search your accounts")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,shape=RoundedCornerShape(18.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Focus",fontSize=12.sp);Switch(checked=focus,onCheckedChange={focus=it;focusId=null},modifier=Modifier.padding(start=4.dp));Spacer(Modifier.weight(1f));Text("Copy fresh",fontSize=12.sp);Switch(checked=activity.smartCopy,onCheckedChange={activity.smartCopy=it},modifier=Modifier.padding(start=4.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) { (listOf("All","Favorites")+activity.accounts.map { it.folder }.distinct().sorted()).forEach { name -> FilterChip(selected=filter==name,onClick={filter=name},label={Text(name,fontSize=12.sp)}) } }
        if(focus)Text("Tap one account to reveal. Other codes stay hidden.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
        if(clockShift)Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.errorContainer) { Text("Your clock changed. Check automatic date and time if codes stop working.",Modifier.padding(12.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onErrorContainer) }
        Spacer(Modifier.height(10.dp))
        val results=activity.accounts.filter { (filter=="All" || filter=="Favorites"&&it.favorite || filter==it.folder) && (it.label.contains(query,true)||it.issuer.contains(query,true)) }
        if(activity.accounts.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Outlined.QrCode2,null,Modifier.size(70.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(18.dp));Text("Your space is ready.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Tap + to scan a QR or enter a setup key.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            if(results.isEmpty())Text("No accounts in this view. Try All or another search.",Modifier.padding(vertical=14.dp))
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=105.dp)) {
                items(results,key={it.id}) { a ->
                    val remaining=Otp.remaining(a,now);val visible=!focus||focusId==a.id
                    Surface(shape=RoundedCornerShape(25.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.fillMaxWidth().then(if(focusId==a.id)Modifier.border(1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(25.dp)) else Modifier)) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.15f)) { Text((a.issuer.ifBlank { a.label }.take(1)).uppercase(),Modifier.padding(14.dp),fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary) }
                                Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)) { Text(a.issuer.ifBlank { "Account" },fontWeight=FontWeight.Bold);Text(a.label,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                                IconButton(onClick={selected=a}) { Icon(Icons.Outlined.Settings,"Manage ${a.issuer}") }
                            }
                            if(focus)TextButton(onClick={focusId=if(focusId==a.id)null else a.id}) { Text(if(visible)"Account in focus" else "Reveal this account") }
                            Spacer(Modifier.height(15.dp))
                            Row(verticalAlignment=Alignment.CenterVertically) { Text(if(visible)Otp.code(a,now).chunked(if(a.digits==6)3 else 4).joinToString(" ") else "••• •••",fontSize=35.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace,color=MaterialTheme.colorScheme.primary,modifier=Modifier.weight(1f));IconButton(enabled=visible&&activity.copyWaitingId==null,onClick={activity.copyCode(a)}) { Icon(Icons.Outlined.ContentCopy,"Copy ${a.issuer} code") } }
                            Spacer(Modifier.height(9.dp));LinearProgressIndicator(progress={remaining.toFloat()/a.period},modifier=Modifier.fillMaxWidth().height(3.dp));Spacer(Modifier.height(9.dp))
                            Row { Text(a.folder,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f));Text(if(activity.copyWaitingId==a.id)"Preparing fresh code…" else "${remaining}s",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                            Row(Modifier.padding(top=6.dp),verticalAlignment=Alignment.CenterVertically) {
                                IconButton(enabled=!activity.demoMode,onClick={activity.mutation(activity.accounts.map { if(it.id==a.id)it.copy(favorite=!it.favorite) else it })}) { Icon(Icons.Outlined.Star,"Favorite ${a.issuer}",tint=if(a.favorite)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                TextButton(onClick={selected=a}) { Text(if(a.recoverySaved)"✓ Recovery saved" else "Recovery needed",fontSize=11.sp,color=if(a.recoverySaved)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                Spacer(Modifier.weight(1f));IconButton(enabled=!activity.demoMode,onClick={selected=a}) { Icon(Icons.Outlined.Delete,"Delete ${a.issuer}",tint=MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { a -> AccountActions(activity,a,onDismiss={selected=null}) }
}

@Composable private fun AccountActions(activity:MainActivity,a:Account,onDismiss:()->Unit) {
    var label by remember(a.id) { mutableStateOf(a.label) };var folder by remember(a.id) { mutableStateOf(a.folder) }
    var favorite by remember(a.id) { mutableStateOf(a.favorite) };var recovery by remember(a.id) { mutableStateOf(a.recoverySaved) };var deleting by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(deleting)"Delete account?" else "Account details")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(deleting)Text("Removing ${a.issuer.ifBlank { a.label }} does not turn off 2FA at the service. Check you can still sign in with recovery codes or another device.")
        else {
            OutlinedTextField(label,{label=it},label={Text("Account name")},singleLine=true)
            OutlinedTextField(folder,{folder=it},label={Text("Folder")},singleLine=true)
            Row(verticalAlignment=Alignment.CenterVertically) { Switch(checked=favorite,onCheckedChange={favorite=it});Spacer(Modifier.width(12.dp));Text("Favorite account",fontSize=13.sp) }
            Row(verticalAlignment=Alignment.CenterVertically) { Switch(checked=recovery,onCheckedChange={recovery=it});Spacer(Modifier.width(12.dp));Text("Recovery codes saved elsewhere",fontSize=13.sp) }
            if(a.lastVerifiedAt>0)Text("Last sign-in drill: ${java.time.Instant.ofEpochMilli(a.lastVerifiedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}",fontSize=11.sp)
            OutlinedButton(onClick={onDismiss();activity.route="readiness"}) { Text("Practice recovery") }
            if(!activity.demoMode)TextButton(onClick={deleting=true}) { Text("Delete account",color=MaterialTheme.colorScheme.error) }
            else Text("Public demo. Editing is disabled.",fontSize=11.sp)
        }
    }},confirmButton={TextButton(enabled=!activity.demoMode&&label.isNotBlank()&&label.length<=256&&folder.isNotBlank()&&folder.length<=64&&!activity.busy,onClick={activity.mutation(if(deleting)activity.accounts.filterNot { it.id==a.id } else activity.accounts.map { if(it.id==a.id)it.copy(label=label.trim(),folder=folder.trim(),favorite=favorite,recoverySaved=recovery).validate() else it });onDismiss()}) { Text(if(deleting)"Delete" else "Save") }},dismissButton={TextButton(onClick=onDismiss) { Text("Close") }})
}

@Composable private fun RecoveryStudio(activity:MainActivity) {
    var drill by remember { mutableStateOf<Account?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=32.dp)) {
        item { Text("A plan for your next phone.",fontSize=29.sp,lineHeight=34.sp,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.height(12.dp));Text("Recovery studio",fontSize=12.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(16.dp));Text("${activity.accounts.count { it.recoverySaved }} / ${activity.accounts.size} recovery plans marked ready",style=MaterialTheme.typography.titleMedium);Text("This is your checklist. Sentinel cannot verify a service’s recovery codes or sign-in.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedButton(onClick={activity.route="settings"},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Backup,null);Spacer(Modifier.width(10.dp));Text("Create or restore a backup") } }
        items(activity.accounts,key={it.id}) { a -> Surface(shape=RoundedCornerShape(23.dp),color=MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(20.dp)) { Text(a.issuer.ifBlank { a.label },fontWeight=FontWeight.Bold);Text(if(a.recoverySaved)"✓ Recovery codes saved elsewhere" else "Recovery codes need attention",fontSize=12.sp,color=if(a.recoverySaved)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text(if(a.lastVerifiedAt>0)"Sign-in drill last marked: ${java.time.Instant.ofEpochMilli(a.lastVerifiedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}" else "Sign-in drill not recorded yet",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick={drill=a},modifier=Modifier.fillMaxWidth()) { Text("Practice recovery") } } } }
        item { Text("Keep automatic date and time enabled. Check the real website before entering a code. TOTP does not protect against phishing.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    drill?.let { a -> AlertDialog(onDismissRequest={drill=null},title={Text("Practice your way back in")},text={Column(verticalArrangement=Arrangement.spacedBy(15.dp)) { Text("1. Locate ${a.issuer} recovery codes in separate safe storage.");Text("2. Confirm your Sentinel backup and its password are available.");Text("3. Test a normal sign-in on another device with a fresh code. Keep this app open and never disable 2FA.");Text("Mark this only after completing the sign-in yourself.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }},confirmButton={TextButton(enabled=!activity.demoMode,onClick={activity.mutation(activity.accounts.map { if(it.id==a.id)it.copy(lastVerifiedAt=System.currentTimeMillis()) else it }) { activity.route="readiness" };drill=null}) { Text(if(activity.demoMode)"Sample checklist" else "I tested my sign-in") }},dismissButton={TextButton(onClick={drill=null}) { Text("Close") }}) }
}

@Composable private fun Manual(activity:MainActivity) {
    var issuer by remember { mutableStateOf("") };var label by remember { mutableStateOf("") };var secret by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) };var algorithm by remember { mutableStateOf("SHA1") };var digits by remember { mutableStateOf("6") };var period by remember { mutableStateOf("30") }
    var error by remember { mutableStateOf<String?>(null) };var reveal by remember { mutableStateOf(false) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted)activity.route="scan" else activity.notify("Camera permission denied. Enter the setup key instead.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Button(onClick={if(ContextCompat.checkSelfPermission(activity,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)activity.route="scan" else permission.launch(Manifest.permission.CAMERA)},modifier=Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Outlined.QrCodeScanner,null);Spacer(Modifier.width(10.dp));Text("Scan QR code") }
        Text("OR ENTER A SETUP KEY",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(issuer,{issuer=it},label={Text("Service / issuer (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(label,{label=it},label={Text("Account name")},placeholder={Text("you@example.com")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(secret,{secret=it},label={Text("Setup key")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=if(reveal)VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={IconButton(onClick={reveal=!reveal}) { Icon(if(reveal)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(reveal)"Hide setup key" else "Show setup key") }},keyboardOptions=KeyboardOptions(autoCorrectEnabled=false,keyboardType=KeyboardType.Password))
        Text("Get this key from the service’s 2FA settings. Never share it with anyone.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={advanced=!advanced}) { Text(if(advanced)"Hide advanced options" else "Advanced options") }
        if(advanced) {
            Text("Match the values supplied by the service.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("SHA1","SHA256","SHA512").forEach { FilterChip(selected=algorithm==it,onClick={algorithm=it},label={Text(it)}) } }
            OutlinedTextField(digits,{digits=it},label={Text("Digits (6 or 8)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(period,{period=it},label={Text("Period in seconds (15–120)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
        }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Button(onClick={try { activity.candidate=Account(issuer=issuer.trim(),label=label.trim(),secret=Base32.decode(secret),algorithm=algorithm,digits=digits.toIntOrNull() ?: 0,period=period.toIntOrNull() ?: 0).validate();secret="";activity.route="review" } catch(e:Exception) { error=e.message ?: "Check the account details." }},modifier=Modifier.fillMaxWidth().height(56.dp)) { Text("Review account") }
    }
}

@Composable private fun Review(activity:MainActivity) {
    val a=activity.candidate ?: return
    val duplicate=activity.accounts.any { it.secret.contentEquals(a.secret)&&it.algorithm==a.algorithm&&it.digits==a.digits&&it.period==a.period }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Icon(Icons.Outlined.VerifiedUser,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary)
        Text("Check your account",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(a.issuer.ifBlank { "Account" },style=MaterialTheme.typography.titleLarge);Text(a.label)
        Text("Time-based code  •  ${a.digits} digits\n${a.algorithm}  •  ${a.period}-second refresh",color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(duplicate)Text("This setup key is already in your vault. Continue only if you intentionally want another copy.",color=MaterialTheme.colorScheme.error)
        Text("After adding, enter the first code on the service to complete its 2FA setup. Adding here does not automatically enable 2FA.")
        Button(onClick={activity.saveCandidate()},modifier=Modifier.fillMaxWidth().height(56.dp)) { Text(if(duplicate)"Add another copy" else "Add account") }
        TextButton(onClick={activity.candidate=null;activity.route="manual"}) { Text("Cancel") }
    }
}

@Composable private fun SettingsScreen(activity:MainActivity) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text(if(activity.accountEmail.isBlank())"Local vault" else activity.accountEmail,style=MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick={activity.signOutAccount()},modifier=Modifier.fillMaxWidth()) { Text(if(activity.accountEmail.isBlank())"Sign in / sign up" else "Sign out") }
        Text("SECURITY",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
        InfoRow(Icons.Outlined.Lock,"Immediate privacy lock","The vault locks when this app goes into the background. Screenshots are blocked.")
        HorizontalDivider()
        Text("RECOVERY",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
        Text("Create and verify a backup before uninstalling or changing phones. Its password cannot be reset.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick={activity.authenticate(after={activity.route="export"})},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Backup,null);Spacer(Modifier.width(8.dp));Text("Create encrypted backup") }
        OutlinedButton(onClick={activity.selectRestore()},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Restore,null);Spacer(Modifier.width(8.dp));Text("Restore from backup") }
        HorizontalDivider()
        Row(verticalAlignment=Alignment.CenterVertically) { Text("Dark appearance",Modifier.weight(1f));Switch(checked=activity.dark,onCheckedChange={activity.toggleTheme()}) }
        HorizontalDivider()
        OutlinedButton(onClick={activity.route="readiness"},modifier=Modifier.fillMaxWidth()) { Text("Open Recovery studio") }
        Text("Codes not working?",style=MaterialTheme.typography.titleMedium)
        Text("Check automatic date and time, select the right account, and try a fresh code. If you lost all copies, use the service’s recovery codes or account recovery.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Sentinel Authenticator 0.4.0\nOffline edition • Android 11 and newer",style=MaterialTheme.typography.bodySmall)
        Text("Cloud sync is not included in this build. Account login needs internet. Code generation remains local.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun BackupForm(activity:MainActivity,restore:Boolean) {
    var password by remember { mutableStateOf("") };var confirm by remember { mutableStateOf("") };var reveal by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Icon(if(restore)Icons.Outlined.Restore else Icons.Outlined.Backup,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary)
        Text(if(restore)"Open your recovery copy" else "Protect your recovery copy",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(if(restore)"Enter the password used to create this Sentinel backup. Your existing accounts will be preserved." else "Use a unique password of at least 12 characters. You will need it on the new device. We cannot reset it.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(password,{password=it},label={Text("Backup password")},singleLine=true,modifier=Modifier.fillMaxWidth(),visualTransformation=if(reveal)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false),trailingIcon={IconButton(onClick={reveal=!reveal}) { Icon(if(reveal)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,"Show or hide password") }})
        if(!restore)OutlinedTextField(confirm,{confirm=it},label={Text("Confirm password")},singleLine=true,modifier=Modifier.fillMaxWidth(),visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false))
        Button(enabled=if(restore)password.isNotEmpty() else password.length>=12 && password==confirm,onClick={val value=password;password="";confirm="";if(restore)activity.decryptRestore(value) else activity.prepareExport(value)},modifier=Modifier.fillMaxWidth().height(56.dp)) { Text(if(restore)"Preview restore" else "Choose where to save") }
        if(!restore)Text("Your backup is encrypted before it leaves the app. Restore it on another device and test sign-in before deleting your original vault.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun RestoreReview(activity:MainActivity) {
    val records=activity.restorePreview ?: return
    val duplicates=records.count { a -> activity.accounts.any { it.secret.contentEquals(a.secret)&&it.algorithm==a.algorithm&&it.digits==a.digits&&it.period==a.period } }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("${records.size} accounts in backup",style=MaterialTheme.typography.headlineMedium)
        if(activity.recoveryMode)Text("The original vault cannot be unlocked. Restoring creates a new protected vault from this backup. The unreadable original is retained encrypted on this device. Confirm only if this is your intended recovery copy.",color=MaterialTheme.colorScheme.error)
        Text("$duplicates identical entries will be skipped. Your current accounts stay in place. Different entries will be added separately, including any that share a name.")
        records.forEach { Text("${it.issuer.ifBlank { "Account" }} · ${it.label}",style=MaterialTheme.typography.bodyMedium) }
        Button(onClick={activity.mergeRestore()},modifier=Modifier.fillMaxWidth().height(56.dp)) { Text("Merge accounts") }
        TextButton(onClick={activity.leaveFlow()}) { Text("Cancel") }
    }
}

@Composable private fun InfoRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,description:String) {
    Row(verticalAlignment=Alignment.Top) { Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=2.dp).size(24.dp));Spacer(Modifier.width(14.dp));Column { Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(4.dp));Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
}
