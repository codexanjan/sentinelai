package app.sentinel.authenticator.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import app.sentinel.authenticator.core.*
import java.io.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultStore(context:Context,namespace:String="local") {
    private val suffix=if(namespace=="local") "" else java.security.MessageDigest.getInstance("SHA-256").digest(namespace.toByteArray()).joinToString("") { "%02x".format(it) }
    private val directory=if(suffix.isEmpty())context.noBackupFilesDir else File(context.noBackupFilesDir,"account-$suffix").apply { mkdirs() }
    private val file=AtomicFile(File(directory,"vault.enc"))
    private var alias="sentinel.wrapping.v1$suffix"
    private var recovering=false
    private val aad="sentinel-vault-v1".toByteArray()
    private val keyStore=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private var dataKey:ByteArray?=null
    private var wrappedKey:ByteArray?=null
    private var wrapNonce:ByteArray?=null
    private var generation=0L
    val exists:Boolean get()=file.baseFile.exists()
    @Synchronized fun lock() { generation++;dataKey?.fill(0);dataKey=null;wrappedKey=null;wrapNonce=null;recovering=false }
    @Synchronized fun token():Long=generation
    private fun wrappingKey(allowNew:Boolean=false):SecretKey {
        if(keyStore.containsAlias(alias))return keyStore.getKey(alias,null) as SecretKey
        require(!exists || allowNew) { "Device protection key is unavailable. Restore an encrypted backup." }
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true).setUserAuthenticationParameters(0,KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            .setInvalidatedByBiometricEnrollment(false).build())
        return generator.generateKey()
    }
    @Synchronized fun authCipher():Cipher {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        if(exists) {
            val envelope=readEnvelope()
            alias=envelope.alias
            cipher.init(Cipher.DECRYPT_MODE,wrappingKey(),GCMParameterSpec(128,envelope.first))
        } else cipher.init(Cipher.ENCRYPT_MODE,wrappingKey())
        return cipher
    }
    @Synchronized fun recoveryCipher():Cipher {
        alias="sentinel.recovery.${java.util.UUID.randomUUID()}"
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,wrappingKey(true)) }
    }
    @Synchronized fun openRecovery(cipher:Cipher,token:Long) {
        require(token==generation)
        val key=Aead.random(32)
        try { cipher.updateAAD(aad);wrappedKey=cipher.doFinal(key);dataKey=key;wrapNonce=cipher.iv;recovering=true }
        catch(e:Exception) { key.fill(0);lock();throw e }
    }
    @Synchronized fun open(cipher:Cipher,token:Long):List<Account> {
        require(token==generation) { "Unlock was interrupted. Try again." }
        try {
            cipher.updateAAD(aad)
            if(exists) {
                val env=readEnvelope();val key=cipher.doFinal(env.second)
                val records=try {
                    val plain=Aead.decrypt(key,env.third.first,env.third.second,aad)
                    try { Payload.decode(plain) } finally { plain.fill(0) }
                } catch(e:Exception) { key.fill(0);throw e }
                dataKey=key;wrapNonce=env.first;wrappedKey=env.second;return records
            }
            val key=Aead.random(32);val wrapped=cipher.doFinal(key)
            dataKey=key;wrapNonce=cipher.iv;wrappedKey=wrapped
            save(emptyList());return emptyList()
        } catch(e:Exception) { lock();throw e }
    }
    @Synchronized fun save(records:List<Account>) {
        val key=dataKey ?: error("Vault is locked.")
        val plain=Payload.encode(records,includeMetadata=true);val nonce=Aead.random(12)
        val encrypted=try { Aead.encrypt(key,nonce,plain,aad) } finally { plain.fill(0) }
        var stream:FileOutputStream?=null
        try {
            if(recovering && exists) {
                val retained=File(directory,"recovery-original-${System.currentTimeMillis()}.enc")
                file.openRead().use { input -> FileOutputStream(retained).use { out -> input.copyTo(out);out.fd.sync() } }
            }
            stream=file.startWrite();val out=DataOutputStream(stream)
            out.writeInt(0x53564C32);out.writeUTF(alias);out.write(wrapNonce!!);out.writeInt(wrappedKey!!.size);out.write(wrappedKey!!)
            out.write(nonce);out.writeInt(encrypted.size);out.write(encrypted);out.flush()
            file.finishWrite(stream)
            recovering=false
        } catch(e:Exception) { if(stream!=null)file.failWrite(stream);throw e }
    }
    private data class Envelope(val alias:String,val first:ByteArray,val second:ByteArray,val third:Pair<ByteArray,ByteArray>)
    private fun readEnvelope():Envelope {
        require(file.baseFile.length()<=Payload.MAX_FILE) { "Invalid vault size." }
        DataInputStream(file.openRead()).use {
            require(it.readInt()==0x53564C32) { "Unsupported vault format." }
            val storedAlias=it.readUTF();require(storedAlias.length<=128 && storedAlias.startsWith("sentinel."))
            val wn=ByteArray(12);it.readFully(wn);val size=it.readInt();require(size==48)
            val wrapped=ByteArray(size);it.readFully(wrapped);val nonce=ByteArray(12);it.readFully(nonce)
            val cs=it.readInt();require(cs in 16..Payload.MAX_FILE)
            val data=ByteArray(cs);it.readFully(data);require(it.read()==-1)
            return Envelope(storedAlias,wn,wrapped,nonce to data)
        }
    }
}
