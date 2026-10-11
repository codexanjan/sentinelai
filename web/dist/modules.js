const pending=new Map();
export function loadModule(name){
 if(name==='argon2'&&globalThis.hashwasm?.argon2id||name==='jsqr'&&globalThis.jsQR||name==='supabase'&&globalThis.supabase||name==='firebase-app'&&globalThis.firebase?.initializeApp||name==='firebase-auth'&&globalThis.firebase?.auth)return Promise.resolve();
 if(pending.has(name))return pending.get(name);
 const paths={argon2:'vendor/argon2.js',jsqr:'vendor/jsqr.js',supabase:'vendor/supabase.js','firebase-app':'vendor/firebase-app-compat.js','firebase-auth':'vendor/firebase-auth-compat.js'};
 if(!paths[name])return Promise.reject(Error('Unknown module.'));
 const task=new Promise((resolve,reject)=>{const script=document.createElement('script');script.src=paths[name];script.async=true;const timer=setTimeout(()=>{script.remove();pending.delete(name);reject(Error('This feature could not load. Check your connection and try again.'));},12000);script.onload=()=>{clearTimeout(timer);resolve();};script.onerror=()=>{clearTimeout(timer);script.remove();pending.delete(name);reject(Error('This feature is unavailable offline until downloaded. Reconnect and try again.'));};document.head.append(script);});
 pending.set(name,task);return task;
}
