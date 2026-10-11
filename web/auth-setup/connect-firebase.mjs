// Accepts only Firebase's public web configuration. No admin credentials belong in the app.
import {readFile,writeFile} from 'node:fs/promises';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';

export function validateFirebaseConfig(input){
 if(!input||typeof input!=='object'||Array.isArray(input))throw Error('Provide the public Firebase web app configuration.');
 if('private_key' in input||'client_email' in input||input.type==='service_account')throw Error('Service-account credentials must never be used in a website.');
 const {apiKey,projectId,appId,authDomain}=input;
 if(!/^AIza[A-Za-z0-9_-]{35}$/.test(apiKey||''))throw Error('Use the Firebase web API key from apps:sdkconfig.');
 if(!/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(projectId||''))throw Error('Invalid Firebase project ID.');
 if(!/^1:\d+:web:[a-f0-9]+$/.test(appId||''))throw Error('Use a registered Firebase web app ID.');
 if(authDomain!==projectId+'.firebaseapp.com')throw Error('Use this Firebase project’s default authDomain.');
 return {apiKey,projectId,appId,authDomain};
}

export async function verifyFirebaseDomain(config,fetcher=fetch){
 const r=await fetcher('https://identitytoolkit.googleapis.com/v1/projects?key='+encodeURIComponent(config.apiKey),{signal:AbortSignal.timeout(12000)});
 if(!r.ok)throw Error('Firebase Authentication project check failed (HTTP '+r.status+'); no files changed.');
 const data=await r.json();
 if(!data.authorizedDomains?.includes('sentinel-authenticator.vercel.app'))throw Error('Authorize sentinel-authenticator.vercel.app in Firebase Authentication first.');
 return data;
}

export function addFirebasePolicy(vercel,config){
 const headers=vercel.headers?.find(r=>r.source==='/(.*)')?.headers;
 const csp=headers?.find(h=>h.key.toLowerCase()==='content-security-policy');
 if(!csp)throw Error('Existing content security policy was not found.');
 const directives=csp.value.split(';').map(x=>x.trim()).filter(Boolean);
 const set=(key,value)=>{const i=directives.findIndex(x=>x.startsWith(key+' '));if(i>=0)directives[i]=key+' '+value;else directives.push(key+' '+value);};
 set('connect-src',"'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com https://www.googleapis.com");
 set('frame-src','https://'+config.authDomain+' https://apis.google.com');
 const scripts=directives.find(x=>x.startsWith('script-src '));
 set('script-src',scripts.slice('script-src '.length).replace(/\s+https:\/\/apis\.google\.com/g,'')+' https://apis.google.com');
 csp.value=directives.join('; ');
 if(!headers.some(h=>h.key.toLowerCase()==='cross-origin-opener-policy'))headers.push({key:'Cross-Origin-Opener-Policy',value:'same-origin-allow-popups'});
 return vercel;
}

async function main(){
 const filename=process.argv[2];if(!filename)throw Error('Usage: node connect-firebase.mjs <public-firebase-web-config.json>');
 const input=JSON.parse(await readFile(filename,'utf8'));
 const config=validateFirebaseConfig(input.result?.sdkConfig||input.sdkConfig||input);
 await verifyFirebaseDomain(config);
 const here=path.dirname(fileURLToPath(import.meta.url));
 const web=path.resolve(process.env.SENTINEL_WEB_DIST||path.join(here,'../dist'));
 const policy=addFirebasePolicy(JSON.parse(await readFile(path.join(web,'vercel.json'),'utf8')),config);
 await writeFile(path.join(web,'vercel.json'),JSON.stringify(policy,null,2)+'\n');
 await writeFile(path.join(web,'auth-config.js'),'export const authConfig='+JSON.stringify({firebase:config,googleEnabled:true,appleEnabled:false})+';\n');
 console.log('Firebase domain verified and public web configuration written. Google provider activation and production sign-in must still be verified before publication.');
}
if(process.argv[1]&&import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href)main().catch(e=>{console.error(e.message);process.exitCode=1;});
