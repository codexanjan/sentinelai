import {loadModule} from './modules.js';

export function firebaseConfigured(config){
 return !!(config?.apiKey && config?.projectId && config?.appId && config?.authDomain);
}

export async function createFirebaseAuth(config){
 if(!firebaseConfigured(config))throw Error('Firebase project setup is required before account sign-in.');
 await loadModule('firebase-app');
 await loadModule('firebase-auth');
 const sdk=globalThis.firebase;
 const app=sdk.apps.find(a=>a.name==='sentinel-account')||sdk.initializeApp(config,'sentinel-account');
 const auth=app.auth();
 await auth.setPersistence(sdk.auth.Auth.Persistence.SESSION);
 await new Promise((resolve,reject)=>{let off=()=>{};const timer=setTimeout(()=>{off();reject(Error('Account connection timed out.'));},6000);off=auth.onAuthStateChanged(()=>{clearTimeout(timer);off();resolve();},error=>{clearTimeout(timer);off();reject(error);});});
 const map=u=>u&&u.emailVerified?{id:u.uid,email:u.email}:null;
 const result=async fn=>{try{return await fn();}catch(error){return {data:{user:null},error:Error(friendly(error.code))};}};
 const actionSettings={url:location.origin+'/#signin',handleCodeInApp:false};
 return {auth:{
  getUser:()=>result(async()=>{const user=auth.currentUser;if(user){await user.reload();if(!user.emailVerified)await auth.signOut();}return {data:{user:map(auth.currentUser)},error:null};}),
  onAuthStateChange:callback=>auth.onAuthStateChanged(user=>callback(user?'SIGNED_IN':'SIGNED_OUT',{user:map(user)})),
  signInWithPassword:({email,password})=>result(async()=>{const {user}=await auth.signInWithEmailAndPassword(email,password);if(!user.emailVerified){await auth.signOut();throw {code:'auth/email-not-verified'};}return {data:{user:map(user)},error:null};}),
  signUp:({email,password,options})=>result(async()=>{const {user}=await auth.createUserWithEmailAndPassword(email,password);try{if(options?.data?.display_name)await user.updateProfile({displayName:options.data.display_name});await user.sendEmailVerification(actionSettings);return {data:{user:null,session:null},error:null};}finally{await auth.signOut();}}),
  signInWithOAuth:({provider})=>result(async()=>{if(!['google','apple'].includes(provider))throw {code:'auth/operation-not-allowed'};const p=provider==='google'?new sdk.auth.GoogleAuthProvider():new sdk.auth.OAuthProvider('apple.com');if(provider==='google')p.setCustomParameters({prompt:'select_account'});else{p.addScope('email');p.addScope('name');}const {user}=await auth.signInWithPopup(p);if(!map(user)){await auth.signOut();throw {code:'auth/email-not-verified'};}return {data:{user:map(user)},error:null};}),
  resetPasswordForEmail:email=>result(async()=>{await auth.sendPasswordResetEmail(email,actionSettings);return {error:null};}),
  updateUser:({password})=>result(async()=>{if(!auth.currentUser)throw {code:'auth/requires-recent-login'};await auth.currentUser.updatePassword(password);return {error:null};}),
  signOut:()=>result(async()=>{await auth.signOut();return {error:null};})
 }};
}

function friendly(code){return ({
 'auth/invalid-credential':'Email or password is incorrect.',
 'auth/user-not-found':'Email or password is incorrect.',
 'auth/wrong-password':'Email or password is incorrect.',
 'auth/email-already-in-use':'Could not create this account. Try signing in or resetting the password.',
 'auth/email-not-verified':'Confirm your email before signing in. Check your inbox.',
 'auth/popup-closed-by-user':'Sign-in was cancelled. Try again when you are ready.',
 'auth/cancelled-popup-request':'Another sign-in window is already open.',
 'auth/popup-blocked':'Allow the Google sign-in popup and try again.',
 'auth/operation-not-allowed':'This login provider needs to be enabled in Firebase.',
 'auth/unauthorized-domain':'This website needs to be authorized in your Firebase project.',
 'auth/invalid-api-key':'The Firebase project configuration needs to be corrected.',
 'auth/weak-password':'Use a stronger account password with at least 12 characters.',
 'auth/invalid-email':'Enter a valid email address.',
 'auth/too-many-requests':'Too many attempts. Please wait and try again.',
 'auth/network-request-failed':'Unable to connect. Check your internet connection.',
 'auth/requires-recent-login':'Sign in again before changing your account password.'
 })[code]||'Account sign-in could not be completed. Please try again.';}
