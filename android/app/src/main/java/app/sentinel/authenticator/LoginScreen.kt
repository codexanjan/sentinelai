package app.sentinel.authenticator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable fun LoginScreen(activity:MainActivity) {
    if(activity.resetAccountPassword) { ResetNativePassword(activity);return }
    var signup by remember { mutableStateOf(false) };var forgot by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") };var email by remember { mutableStateOf("") };var password by remember { mutableStateOf("") };var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") };var working by remember { mutableStateOf(false) };val scope=rememberCoroutineScope()
    fun result(text:String?) { working=false;password="";confirm="";message=text ?: "" }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal=26.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(12.dp));Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Outlined.Shield,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(30.dp));Spacer(Modifier.width(10.dp));Text("sentinel.",fontSize=28.sp,fontWeight=FontWeight.Bold) }
        Spacer(Modifier.height(10.dp));Text(if(forgot)"Reset account password." else if(signup)"A safer start.\nYour own space." else "Welcome back.\nYour safe space awaits.",fontSize=33.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold)
        Text("Account login and your device-protected vault are separate. Your setup keys stay on this device.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(!forgot&&!signup) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { Button(onClick={activity.continueLocal()},modifier=Modifier.weight(1f).height(50.dp),shape=RoundedCornerShape(14.dp)) { Text("Open local vault") };OutlinedButton(onClick={activity.startDemo()},modifier=Modifier.weight(1f).height(50.dp),shape=RoundedCornerShape(14.dp)) { Text("Try demo") } } }
        if(!forgot) {
            OutlinedButton(enabled=!working,onClick={working=true;scope.launch { activity.accountLogin.google(::result) }},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(12.dp)) { Text("G   Continue with Google") }
            OutlinedButton(enabled=!working,onClick={working=true;activity.accountLogin.apple(::result)},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(12.dp)) { Text("Continue with Apple") }
            Row(verticalAlignment=Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f));Text("  or use email  ",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);HorizontalDivider(Modifier.weight(1f)) }
        }
        if(signup&&!forgot)OutlinedTextField(name,{name=it.take(100)},label={Text("Your name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(email,{email=it.take(254)},label={Text("Email address")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,autoCorrectEnabled=false))
        if(!forgot)OutlinedTextField(password,{password=it},label={Text("Password")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false))
        if(signup&&!forgot)OutlinedTextField(confirm,{confirm=it},label={Text("Confirm password")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false))
        if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodyMedium)
        Button(enabled=!working,onClick={
            val address=email.trim().lowercase();message=""
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) { message="Enter a valid email address." }
            else if(forgot) { working=true;activity.accountLogin.reset(address) { result(it) } }
            else if(!signup&&address=="demo@sentinel.example") { if(password=="SentinelDemo123!") { password="";activity.startDemo() } else message="The demo password is incorrect." }
            else if(password.length<12)message="Use a password of at least 12 characters."
            else if(signup&&(name.isBlank()||password!=confirm))message="Enter your name and matching passwords."
            else { working=true;val value=password;password="";confirm="";activity.accountLogin.email(address,value,signup,name,::result) }
        },modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(12.dp)) { if(working)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Text(if(forgot)"Send reset link" else if(signup)"Create account" else "Sign in") }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=!working,onClick={signup=!signup;forgot=false;message="";password="";confirm=""}) { Text(if(signup)"Already a member? Sign in" else "Create an account") }
            if(!signup&&!forgot)TextButton(onClick={forgot=true;message="";password=""}) { Text("Forgot password?") }
        }
        if(forgot)TextButton(onClick={forgot=false}) { Text("Back to sign in") }
        if(!forgot)Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("DEMO ACCESS",color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
                Text("Read-only demo with public test accounts. Real vaults stay separate.",style=MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick={activity.startDemo()},modifier=Modifier.fillMaxWidth()) { Text("Explore sample accounts") }
                Text("Email: demo@sentinel.example\nPassword: SentinelDemo123!",style=MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick={activity.continueLocal()},modifier=Modifier.fillMaxWidth()) { Text("Continue with a local vault") }
        if(!activity.accountLogin.configured)Text("Firebase account setup is unavailable. Demo and local vault work now.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun ResetNativePassword(activity:MainActivity) {
    var password by remember { mutableStateOf("") };var confirm by remember { mutableStateOf("") };var message by remember { mutableStateOf("") };var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(26.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("New account password",style=MaterialTheme.typography.headlineMedium)
        Text("This changes your login, not the device protection for your vault.")
        OutlinedTextField(password,{password=it},label={Text("New password (12+ characters)")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        OutlinedTextField(confirm,{confirm=it},label={Text("Confirm password")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        Text(message)
        Button(enabled=!busy,onClick={if(password.length<12||password!=confirm)message="Enter matching passwords of at least 12 characters." else {busy=true;activity.accountLogin.updatePassword(password){message=it ?: "";busy=false;password="";confirm=""}}},modifier=Modifier.fillMaxWidth()) {Text("Update password")}
        TextButton(onClick={activity.resetAccountPassword=false;activity.signOutAccount()}) {Text("Cancel")}
    }
}
