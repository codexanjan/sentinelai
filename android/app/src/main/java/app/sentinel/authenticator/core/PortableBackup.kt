package app.sentinel.authenticator.core

import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

object Payload {
    const val MAX_FILE = 10*1024*1024
    fun encode(accounts: List<Account>,includeMetadata:Boolean=false): ByteArray {
        require(accounts.size<=1000)
        val bytes=ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(if(includeMetadata)2 else 1); out.writeInt(accounts.size)
            accounts.forEach { a ->
                a.validate(); out.writeUTF(a.id);out.writeUTF(a.issuer);out.writeUTF(a.label)
                out.writeInt(a.secret.size);out.write(a.secret);out.writeUTF(a.algorithm);out.writeInt(a.digits);out.writeInt(a.period)
                if(includeMetadata) { out.writeUTF(a.folder);out.writeBoolean(a.favorite);out.writeBoolean(a.recoverySaved);out.writeLong(a.lastVerifiedAt) }
            }
        }
        return bytes.toByteArray()
    }
    fun decode(data: ByteArray): List<Account> {
        require(data.size<=MAX_FILE) { "Vault is too large." }
        DataInputStream(ByteArrayInputStream(data)).use { input ->
            val version=input.readInt();require(version in 1..2) { "Unsupported vault version." }
            val count=input.readInt();require(count in 0..1000) { "Invalid account count." }
            val items=(0 until count).map {
                val id=input.readUTF();val issuer=input.readUTF();val label=input.readUTF()
                val size=input.readInt();require(size in 10..128)
                val secret=ByteArray(size);input.readFully(secret)
                val a=Account(id,issuer,label,secret,input.readUTF(),input.readInt(),input.readInt())
                (if(version==2)a.copy(folder=input.readUTF(),favorite=input.readBoolean(),recoverySaved=input.readBoolean(),lastVerifiedAt=input.readLong()) else a).validate()
            }
            require(items.map { it.id }.distinct().size==items.size && input.available()==0) { "Invalid vault records." }
            return items
        }
    }
}

object Aead {
    fun random(size:Int)=ByteArray(size).also { SecureRandom().nextBytes(it) }
    fun encrypt(key:ByteArray,nonce:ByteArray,plain:ByteArray,aad:ByteArray):ByteArray {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,nonce));cipher.updateAAD(aad)
        return cipher.doFinal(plain)
    }
    fun decrypt(key:ByteArray,nonce:ByteArray,ciphertext:ByteArray,aad:ByteArray):ByteArray {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,nonce));cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }
}

object PortableBackup {
    private val magic="SENTINEL-BACKUP-1".toByteArray(Charsets.US_ASCII)
    private fun derive(password:CharArray,salt:ByteArray):ByteArray {
        val parameters=Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt).withMemoryAsKB(65536).withIterations(3).withParallelism(1).build()
        val generator=Argon2BytesGenerator();generator.init(parameters)
        return ByteArray(32).also { generator.generateBytes(password,it) }
    }
    fun export(accounts:List<Account>,password:CharArray):ByteArray {
        require(password.size>=12) { "Use a password of at least 12 characters." }
        val salt=Aead.random(16);val nonce=Aead.random(12);val header=magic+salt+nonce
        val key=derive(password,salt);val plain=Payload.encode(accounts)
        return try { header+Aead.encrypt(key,nonce,plain,header) } finally { key.fill(0);plain.fill(0) }
    }
    fun restore(file:ByteArray,password:CharArray):List<Account> {
        require(file.size in (magic.size+16+12+16)..Payload.MAX_FILE) { "Invalid backup size." }
        require(file.copyOfRange(0,magic.size).contentEquals(magic)) { "Unsupported backup format." }
        val salt=file.copyOfRange(magic.size,magic.size+16);val nonce=file.copyOfRange(magic.size+16,magic.size+28)
        val header=file.copyOfRange(0,magic.size+28);val key=derive(password,salt)
        var plain:ByteArray?=null
        return try {
            plain=Aead.decrypt(key,nonce,file.copyOfRange(header.size,file.size),header)
            Payload.decode(plain)
        } catch (_:Exception) { throw IllegalArgumentException("Cannot open backup. Check the password and file.") }
        finally { key.fill(0);plain?.fill(0) }
    }
}
