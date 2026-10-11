const {chromium}=require('playwright');
const assert=require('node:assert/strict'),fs=require('node:fs/promises');
(async()=>{
 const browser=await chromium.launch({headless:true});
 try{
  const context=await browser.newContext({serviceWorkers:'block'});
  const config={apiKey:'test-emulator-key',projectId:'demo-sentinel-auth',appId:'1:123:web:abc',authDomain:'demo-sentinel-auth.firebaseapp.com'};
  await context.route('**/auth-config.js',r=>r.fulfill({contentType:'text/javascript',body:'export const authConfig='+JSON.stringify({firebase:config,googleEnabled:true,appleEnabled:false})+';'}));
  const sdk=await fs.readFile('web/dist/vendor/firebase-auth-compat.js','utf8');
  await context.route('**/vendor/firebase-auth-compat.js',r=>r.fulfill({contentType:'text/javascript',body:sdk+'\nfirebase.initializeApp('+JSON.stringify(config)+',"sentinel-account").auth().useEmulator("http://127.0.0.1:9099",{disableWarnings:true});'}));
  const page=await context.newPage(),email='ui-'+Date.now()+'@example.test',password='UiTestPassword123!';
  await page.goto('http://127.0.0.1:4186/#signup');
  await page.locator('[name=name]').fill('Local test user');await page.locator('[name=email]').fill(email);
  await page.locator('[name=password]').fill(password);await page.locator('[name=confirm]').fill(password);
  await page.getByRole('button',{name:'Create account',exact:true}).click();
  await page.getByText('Check your inbox to confirm your email, then sign in.').waitFor();
  await page.getByRole('link',{name:'Sign in',exact:true}).last().click();
  await page.locator('[name=email]').fill(email);await page.locator('[name=password]').fill(password);
  await page.getByRole('button',{name:'Sign in',exact:true}).click();await page.getByText('Confirm your email before signing in. Check your inbox.').waitFor();
  const codes=await (await fetch('http://127.0.0.1:9099/emulator/v1/projects/demo-sentinel-auth/oobCodes')).json();
  const code=codes.oobCodes.find(c=>c.email===email&&c.requestType==='VERIFY_EMAIL').oobCode;
  await page.evaluate(code=>firebase.app('sentinel-account').auth().applyActionCode(code),code);
  await page.locator('[name=password]').fill(password);await page.getByRole('button',{name:'Sign in',exact:true}).click();
  await page.waitForFunction(()=>location.hash==='#vault');await page.getByRole('heading',{name:'Create your vault.'}).waitFor();
  const uid=await page.evaluate(()=>firebase.app('sentinel-account').auth().currentUser.uid);assert.ok(uid);
  await page.evaluate(()=>localStorage.setItem('sentinel.vault','test-local-vault-preserved'));
  await page.locator('[name=password]').fill('VaultOnlyPassword123!');await page.locator('[name=confirm]').fill('VaultOnlyPassword123!');
  await page.getByRole('button',{name:'Create encrypted vault',exact:true}).click();await page.locator('#floating-add').waitFor();
  const stored=await page.evaluate(uid=>({local:localStorage.getItem('sentinel.vault'),account:localStorage.getItem('sentinel.vault.user.'+uid)}),uid);
  assert.equal(stored.local,'test-local-vault-preserved');assert.ok(stored.account);assert.ok(!stored.account.includes(email));
  await page.locator('#account-login').click();await page.waitForFunction(()=>location.hash==='#signin');
  assert.equal(await page.evaluate(()=>firebase.app('sentinel-account').auth().currentUser),null);
  console.log('Real UI + Firebase emulator passed: signup, verification gate, email login, separate encrypted account vault, local-vault preservation and sign-out.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
