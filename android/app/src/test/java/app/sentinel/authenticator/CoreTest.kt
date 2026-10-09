package app.sentinel.authenticator
import app.sentinel.authenticator.core.*
import org.junit.Test
import org.junit.Assert.*

class CoreTest {
    @Test fun allRfc6238Vectors() {
        val times=longArrayOf(59,1111111109,1111111111,1234567890,2000000000,20000000000)
        val expected=listOf(
            listOf("94287082","07081804","14050471","89005924","69279037","65353130"),
            listOf("46119246","68084774","67062674","91819424","90698825","77737706"),
            listOf("90693936","25091201","99943326","93441116","38618901","47863826"))
        val seeds=listOf("12345678901234567890","12345678901234567890123456789012","1234567890123456789012345678901234567890123456789012345678901234")
        listOf("SHA1","SHA256","SHA512").forEachIndexed { i,alg ->
            val account=Account(issuer="Test",label="example.test",secret=seeds[i].toByteArray(),algorithm=alg,digits=8)
            times.forEachIndexed { j,t -> assertEquals(expected[i][j],Otp.code(account,t)) }
        }
    }
    @Test fun allRfc4226Vectors() {
        listOf("755224","287082","359152","969429","338314","254676","287922","162583","399871","520489").forEachIndexed { i,v ->
            assertEquals(v,Otp.hotp("12345678901234567890".toByteArray(),i.toLong(),"SHA1",6))
        }
    }
    @Test fun parserAndBase32() {
        val a=Provisioning.parse("otpauth://totp/Example:alice%40example.test?secret=JBSWY3DPEHPK3PXP&issuer=Example")
        assertEquals("alice@example.test",a.label);assertEquals(30,a.period)
        assertArrayEquals("Hello!".toByteArray()+byteArrayOf(-34,-83,-66,-17),a.secret)
    }
    @Test fun maliciousParametersRejected() {
        listOf("digits=7","algorithm=MD5","period=0","period=no","issuer=Other","secret=AAAA", "digits=6&digits=8").forEach {
            assertThrows(IllegalArgumentException::class.java) { Provisioning.parse("otpauth://totp/Example:a?secret=JBSWY3DPEHPK3PXP&$it") }
        }
        assertThrows(IllegalArgumentException::class.java) { Provisioning.parse("https://example.test") }
        assertThrows(IllegalArgumentException::class.java) { Base32.decode("AAAAAAAAAAAAAAAA!") }
        assertThrows(IllegalArgumentException::class.java) { Base32.decode("AAAAAAAAAAAAAAAA========") }
        assertThrows(IllegalArgumentException::class.java) { Provisioning.parse("otpauth://totp/%FF?secret=JBSWY3DPEHPK3PXP") }
    }
    @Test fun timerBoundaries() {
        val a=Account(issuer="",label="test",secret=ByteArray(20))
        assertEquals(1,Otp.remaining(a,29));assertEquals(30,Otp.remaining(a,30));assertEquals(29,Otp.remaining(a,31))
        assertEquals(Otp.code(a,30),Otp.code(a,59))
    }
    @Test fun portableBackupRoundTripAndIntegrity() {
        val records=listOf(Account(issuer="Example",label="person@example.test",secret=Aead.random(20),algorithm="SHA256",digits=8,period=60))
        val password="a unique backup password".toCharArray()
        val encrypted=PortableBackup.export(records,password);val restored=PortableBackup.restore(encrypted,password)
        assertEquals(records[0].id,restored[0].id);assertArrayEquals(records[0].secret,restored[0].secret)
        assertEquals(Otp.code(records[0],1234567890),Otp.code(restored[0],1234567890))
        assertThrows(IllegalArgumentException::class.java) { PortableBackup.restore(encrypted,"incorrect password".toCharArray()) }
        encrypted[encrypted.lastIndex]=(encrypted.last().toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) { PortableBackup.restore(encrypted,password) }
    }
    @Test fun aeadRejectsChangedHeader() {
        val key=Aead.random(32);val nonce=Aead.random(12);val cipher=Aead.encrypt(key,nonce,"private".toByteArray(),"v1".toByteArray())
        assertThrows(Exception::class.java) { Aead.decrypt(key,nonce,cipher,"v2".toByteArray()) }
    }
    @Test fun payloadRejectsTrailingData() {
        assertThrows(IllegalArgumentException::class.java) { Payload.decode(Payload.encode(emptyList())+byteArrayOf(1)) }
    }
}
