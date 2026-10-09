package app.sentinel.authenticator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.sentinel.authenticator.data.VaultStore
import app.sentinel.authenticator.core.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceSecurityTest {
    @Test fun lockedRepositoryRejectsWrites() {
        val store=VaultStore(InstrumentationRegistry.getInstrumentation().targetContext)
        assertThrows(IllegalStateException::class.java) { store.save(emptyList()) }
        val before=store.token();store.lock();assertTrue(store.token()>before)
    }
    @Test fun backupUsesAndroidCryptoAndPreservesUnicode() {
        val account=Account(issuer="Example",label="Δοκιμή@example.test",secret=Aead.random(20))
        val password="A long unique пароль 🔐".toCharArray()
        val encrypted=PortableBackup.export(listOf(account),password)
        val recovered=PortableBackup.restore(encrypted,password).single()
        assertEquals(account.label,recovered.label);assertArrayEquals(account.secret,recovered.secret)
        assertEquals(Otp.code(account,59),Otp.code(recovered,59))
    }
    @Test fun appAllowsLoginNetworkWithoutBroadStoragePermission() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val requested=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions?.toList() ?: emptyList()
        assertTrue(requested.contains("android.permission.INTERNET"))
        assertFalse(requested.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse(requested.contains("android.permission.MANAGE_EXTERNAL_STORAGE"))
    }
}
