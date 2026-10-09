package app.sentinel.authenticator.core

import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class Account(val id: String = UUID.randomUUID().toString(), val issuer: String, val label: String,
    val secret: ByteArray, val algorithm: String = "SHA1", val digits: Int = 6, val period: Int = 30,
    val folder:String="Personal",val favorite:Boolean=false,val recoverySaved:Boolean=false,val lastVerifiedAt:Long=0L) {
    fun validate(): Account {
        require(label.isNotBlank() && label.length <= 256 && issuer.length <= 256) { "Enter an account name of up to 256 characters." }
        require(secret.size in 10..128) { "Setup key must contain between 10 and 128 decoded bytes." }
        require(algorithm in setOf("SHA1", "SHA256", "SHA512")) { "Unsupported algorithm." }
        require(digits == 6 || digits == 8) { "Only 6 or 8 digits are supported." }
        require(period in 15..120) { "Period must be between 15 and 120 seconds." }
        require(folder.isNotBlank() && folder.length<=64 && folder.none { it.code<32 } && lastVerifiedAt>=0) { "Invalid account organization data." }
        require(runCatching { UUID.fromString(id) }.isSuccess) { "Invalid account identifier." }
        return this
    }
}

object Base32 {
    private const val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    fun decode(input: String): ByteArray {
        val text = input.filterNot { it.isWhitespace() }.uppercase(java.util.Locale.ROOT)
        require(text.length <= 208 && text.isNotEmpty()) { "Enter a valid Base32 setup key." }
        val raw = text.trimEnd('=')
        require('=' !in raw && raw.length % 8 in setOf(0,2,4,5,7)) { "Invalid setup key length." }
        if (raw.length != text.length) require(text.length % 8 == 0 && text.length-raw.length == (8-raw.length%8)%8) { "Invalid Base32 padding." }
        var buffer = 0; var bits = 0
        val result = ArrayList<Byte>()
        for (c in raw) {
            val value = alphabet.indexOf(c)
            require(value >= 0) { "Setup key may only use letters A-Z and numbers 2-7." }
            buffer = (buffer shl 5) or value; bits += 5
            if (bits >= 8) { bits -= 8; result.add((buffer shr bits).toByte()); buffer = buffer and ((1 shl bits)-1) }
        }
        require(buffer == 0) { "Setup key has invalid trailing bits." }
        return result.toByteArray()
    }
}

object Otp {
    fun hotp(secret: ByteArray, counter: Long, algorithm: String, digits: Int): String {
        require(counter >= 0 && digits in setOf(6,8) && algorithm in setOf("SHA1","SHA256","SHA512"))
        val name = "Hmac$algorithm"
        val mac = Mac.getInstance(name); mac.init(SecretKeySpec(secret,name))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = hash.last().toInt() and 15
        val value = ((hash[offset].toInt() and 127) shl 24) or ((hash[offset+1].toInt() and 255) shl 16) or
            ((hash[offset+2].toInt() and 255) shl 8) or (hash[offset+3].toInt() and 255)
        return (value % if (digits==6) 1000000 else 100000000).toString().padStart(digits,'0')
    }
    fun code(account: Account, seconds: Long): String = hotp(account.secret,seconds/account.period,account.algorithm,account.digits)
    fun remaining(account: Account, seconds: Long): Int = account.period - (seconds % account.period).toInt()
}

object Provisioning {
    fun parse(text: String): Account {
        require(text.toByteArray().size <= 8192) { "QR payload is too large." }
        val uri = try { URI(text) } catch (_: Exception) { throw IllegalArgumentException("This is not a valid authenticator setup code.") }
        require(uri.scheme.equals("otpauth",true) && uri.host == "totp" && uri.fragment == null && uri.port == -1 && uri.userInfo == null) { "Only standard TOTP setup codes are supported." }
        fun decode(v:String)=try { URLDecoder.decode(v.replace("+","%2B"),"UTF-8").also { require('\uFFFD' !in it) } } catch (_:Exception) { throw IllegalArgumentException("Invalid QR encoding.") }
        val query = mutableMapOf<String,String>()
        (uri.rawQuery ?: "").split('&').filter { it.isNotEmpty() }.forEach {
            val pair=it.split('=',limit=2); val key=decode(pair[0])
            require(!query.containsKey(key)) { "QR has duplicate parameters." }
            query[key]=decode(pair.getOrElse(1){""})
        }
        val label=decode((uri.rawPath ?: "").removePrefix("/"))
        val prefix=label.substringBefore(':',"").trim()
        val accountName=if (':' in label) label.substringAfter(':').trim() else label.trim()
        val issuer=query["issuer"] ?: prefix
        require(prefix.isEmpty() || query["issuer"] == null || prefix==issuer) { "Issuer fields disagree. Use the service's setup key instead." }
        require(':' !in accountName && ':' !in issuer) { "Invalid account label." }
        val digits=query["digits"]?.let { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid digits.") } ?: 6
        val period=query["period"]?.let { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid period.") } ?: 30
        return Account(issuer=issuer,label=accountName,secret=Base32.decode(query["secret"] ?: ""),
            algorithm=query["algorithm"] ?: "SHA1",digits=digits,period=period).validate()
    }
}
