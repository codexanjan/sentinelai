import {loadModule} from './modules.js';
const encoder = new TextEncoder();
const MAX_FILE = 10 * 1024 * 1024;
const MAGIC = encoder.encode('SENTINEL-BACKUP-1');
export const random = n => crypto.getRandomValues(new Uint8Array(n));
export const concat = (...parts) => { const result=new Uint8Array(parts.reduce((n,p)=>n+p.length,0));let offset=0;for(const p of parts){result.set(p,offset);offset+=p.length;}return result; };
export const base64 = bytes => {let out='';for(const n of bytes)out+=String.fromCharCode(n);return btoa(out);};
export const unbase64 = text => Uint8Array.from(atob(text),x=>x.charCodeAt(0));
export function decodeBase32(input) {
  const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567', text=input.replace(/\s/g,'').toUpperCase(),raw=text.replace(/=+$/,'');
  if(!text || text.length>208 || ![0,2,4,5,7].includes(raw.length%8) || raw.includes('='))throw Error('Enter a valid Base32 setup key.');
  if(raw.length!==text.length && (text.length%8!==0 || text.length-raw.length!==(8-raw.length%8)%8))throw Error('Invalid setup key padding.');
  let buffer=0,bits=0;const output=[];
  for(const letter of raw){const value=alphabet.indexOf(letter);if(value<0)throw Error('Setup keys use letters A–Z and numbers 2–7.');buffer=(buffer<<5)|value;bits+=5;if(bits>=8){bits-=8;output.push((buffer>>bits)&255);buffer&=(1<<bits)-1;}}
  if(buffer)throw Error('Invalid setup key trailing bits.');
  return new Uint8Array(output);
}
export function validate(account) {
  if(typeof account.id!=='string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(account.id))throw Error('Invalid account identifier.');
  if(typeof account.label!=='string'||!account.label.trim()||account.label.length>256 || typeof account.issuer!=='string'||account.issuer.length>256)throw Error('Enter an account name of up to 256 characters.');
  const seed=unbase64(account.secret);if(seed.length<10||seed.length>128)throw Error('The setup key must contain 10–128 decoded bytes.');
  if(!['SHA1','SHA256','SHA512'].includes(account.algorithm)||![6,8].includes(account.digits)||!Number.isInteger(account.period)||account.period<15||account.period>120)throw Error('Unsupported algorithm, digits or refresh period.');
  return account;
}
export function parseProvisioning(text) {
  if(encoder.encode(text).length>8192)throw Error('Setup code is too large.');
  let uri;try{uri=new URL(text);}catch{throw Error('This is not an authenticator setup code.');}
  if(uri.protocol!=='otpauth:'||uri.hostname!=='totp'||uri.hash||uri.port||uri.username||uri.password)throw Error('Only standard TOTP setup codes are supported.');
  const q={};const params=new URLSearchParams(uri.search.replace(/\+/g,'%2B'));
  for(const [key,value] of params){if(key in q)throw Error('Setup code has duplicate parameters.');if(value.includes('\uFFFD'))throw Error('Invalid setup code encoding.');q[key]=value;}
  let label;try{label=decodeURIComponent(uri.pathname.slice(1));}catch{throw Error('Invalid setup code encoding.');}
  const colon=label.indexOf(':'),prefix=colon<0?'':label.slice(0,colon).trim(),name=(colon<0?label:label.slice(colon+1)).trim(),issuer=q.issuer??prefix;
  if(prefix && q.issuer!==undefined && prefix!==issuer)throw Error('Issuer fields disagree. Use the service’s setup key.');
  if(name.includes(':')||issuer.includes(':'))throw Error('Invalid account label.');
  const number=(key,fallback)=>{if(q[key]===undefined)return fallback;if(!/^\d+$/.test(q[key]))throw Error('Invalid '+key+'.');return Number(q[key]);};
  return validate({id:crypto.randomUUID(),issuer,label:name,secret:base64(decodeBase32(q.secret??'')),algorithm:q.algorithm??'SHA1',digits:number('digits',6),period:number('period',30),folder:'Personal',favorite:false});
}
export async function otp(account,seconds=Math.floor(Date.now()/1000)) {
  const secret=unbase64(account.secret),counter=BigInt(Math.floor(seconds/account.period)),bytes=new Uint8Array(8);new DataView(bytes.buffer).setBigUint64(0,counter);
  const algorithm=account.algorithm.replace('SHA','SHA-');
  const key=await crypto.subtle.importKey('raw',secret,{name:'HMAC',hash:algorithm},false,['sign']);
  secret.fill(0);const hash=new Uint8Array(await crypto.subtle.sign('HMAC',key,bytes)),offset=hash[hash.length-1]&15;
  const value=((hash[offset]&127)<<24)|((hash[offset+1]&255)<<16)|((hash[offset+2]&255)<<8)|(hash[offset+3]&255);
  return String(value%(10**account.digits)).padStart(account.digits,'0');
}
export const sameAccount=(a,b)=>a.secret===b.secret && a.algorithm===b.algorithm && a.digits===b.digits && a.period===b.period;
export async function deriveKey(password,salt) {
  await loadModule('argon2');
  const bytes=await hashwasm.argon2id({password,salt,parallelism:1,iterations:3,memorySize:65536,hashLength:32,outputType:'binary'});
  try{return await crypto.subtle.importKey('raw',bytes,'AES-GCM',false,['encrypt','decrypt']);}finally{bytes.fill(0);}
}
export async function encryptJSON(data,key,salt) {
  const iv=random(12),aad=encoder.encode('sentinel-browser-v1'),plain=encoder.encode(JSON.stringify(data));
  try { const encrypted=await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:aad},key,plain);return {version:1,salt:base64(salt),iv:base64(iv),ciphertext:base64(new Uint8Array(encrypted))}; }
  finally {plain.fill(0);}
}
export async function decryptJSON(envelope,key) {
  if(envelope.version!==1)throw Error('Unsupported browser vault version.');
  let plain;
  try { plain=new Uint8Array(await crypto.subtle.decrypt({name:'AES-GCM',iv:unbase64(envelope.iv),additionalData:encoder.encode('sentinel-browser-v1')},key,unbase64(envelope.ciphertext)));
    const data=JSON.parse(new TextDecoder().decode(plain));if(!Array.isArray(data.accounts)||data.accounts.length>1000)throw Error();data.accounts.forEach(validate);if(new Set(data.accounts.map(a=>a.id)).size!==data.accounts.length)throw Error();return data;
  } catch {throw Error('Cannot unlock this vault. Check your password.');}finally{plain?.fill(0);}
}
// Java DataOutputStream compatible modified UTF-8 keeps Android backups portable.
function modifiedUTF8(text){const bytes=[];for(let i=0;i<text.length;i++){const c=text.charCodeAt(i);if(c>=1&&c<=127)bytes.push(c);else if(c<=2047)bytes.push(192|(c>>6),128|(c&63));else bytes.push(224|(c>>12),128|((c>>6)&63),128|(c&63));}if(bytes.length>65535)throw Error('Text field too long.');return new Uint8Array(bytes);}
function decodeModifiedUTF8(bytes){const chars=[];for(let i=0;i<bytes.length;){const b=bytes[i++];if(b>=1&&b<=127)chars.push(b);else if((b&224)===192){if(i>=bytes.length||(bytes[i]&192)!==128)throw Error('Invalid backup text.');const c=((b&31)<<6)|(bytes[i++]&63);if(c!==0&&c<128)throw Error('Invalid backup text.');chars.push(c);}else if((b&240)===224){if(i+1>=bytes.length||(bytes[i]&192)!==128||(bytes[i+1]&192)!==128)throw Error('Invalid backup text.');const c=((b&15)<<12)|((bytes[i++]&63)<<6)|(bytes[i++]&63);if(c<2048)throw Error('Invalid backup text.');chars.push(c);}else throw Error('Invalid backup text.');}return String.fromCharCode(...chars);}
export function encodePayload(accounts){if(accounts.length>1000)throw Error('Account limit exceeded.');const parts=[];const int=n=>{const b=new Uint8Array(4);new DataView(b.buffer).setInt32(0,n);parts.push(b);};const utf=s=>{const b=modifiedUTF8(s),size=new Uint8Array(2);new DataView(size.buffer).setUint16(0,b.length);parts.push(size,b);};int(1);int(accounts.length);for(const a of accounts){validate(a);utf(a.id);utf(a.issuer);utf(a.label);const seed=unbase64(a.secret);int(seed.length);parts.push(seed);utf(a.algorithm);int(a.digits);int(a.period);}return concat(...parts);}
export function decodePayload(bytes){if(bytes.length>MAX_FILE)throw Error('Backup is too large.');let offset=0;const take=n=>{if(n<0||offset+n>bytes.length)throw Error('Invalid backup records.');const out=bytes.slice(offset,offset+n);offset+=n;return out;};const int=()=>new DataView(take(4).buffer).getInt32(0);const utf=()=>decodeModifiedUTF8(take(new DataView(take(2).buffer).getUint16(0)));if(int()!==1)throw Error('Unsupported backup version.');const count=int();if(count<0||count>1000)throw Error('Invalid account count.');const accounts=[];for(let i=0;i<count;i++){const id=utf(),issuer=utf(),label=utf(),size=int();if(size<10||size>128)throw Error('Invalid setup key.');accounts.push(validate({id,issuer,label,secret:base64(take(size)),algorithm:utf(),digits:int(),period:int(),folder:'Personal',favorite:false}));}if(offset!==bytes.length||new Set(accounts.map(a=>a.id)).size!==count)throw Error('Invalid backup records.');return accounts;}
export async function exportPortable(accounts,password){if(password.length<12)throw Error('Use a backup password of at least 12 characters.');const salt=random(16),iv=random(12),header=concat(MAGIC,salt,iv),key=await deriveKey(password,salt),plain=encodePayload(accounts);try{return concat(header,new Uint8Array(await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:header},key,plain)));}finally{plain.fill(0);}}
export async function importPortable(bytes,password){if(bytes.length>MAX_FILE||bytes.length<MAGIC.length+44)throw Error('Invalid backup size.');if(!MAGIC.every((v,i)=>bytes[i]===v))throw Error('Only Sentinel encrypted backups are supported.');const salt=bytes.slice(MAGIC.length,MAGIC.length+16),iv=bytes.slice(MAGIC.length+16,MAGIC.length+28),header=bytes.slice(0,MAGIC.length+28),key=await deriveKey(password,salt);let plain;try{plain=new Uint8Array(await crypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:header},key,bytes.slice(header.length)));return decodePayload(plain);}catch{throw Error('Cannot open backup. Check the password and file.');}finally{plain?.fill(0);}}
