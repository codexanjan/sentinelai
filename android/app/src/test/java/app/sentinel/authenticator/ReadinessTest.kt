package app.sentinel.authenticator

import app.sentinel.authenticator.core.*
import org.junit.Assert.*
import org.junit.Test

class ReadinessTest {
    private fun account()=Account(issuer="Example",label="Public test",secret="12345678901234567890".toByteArray())
    @Test fun localMetadataSurvivesAndLegacyVaultStillOpens() {
        val a=account().copy(folder="Work 😀",favorite=true,recoverySaved=true,lastVerifiedAt=123456789L)
        val rich=Payload.decode(Payload.encode(listOf(a),true)).single()
        assertEquals(a.folder,rich.folder);assertTrue(rich.favorite);assertTrue(rich.recoverySaved);assertEquals(a.lastVerifiedAt,rich.lastVerifiedAt)
        val legacy=Payload.decode(Payload.encode(listOf(a))).single()
        assertEquals("Personal",legacy.folder);assertFalse(legacy.favorite);assertArrayEquals(a.secret,legacy.secret)
    }
    @Test fun freshCopyUsesActualAccountPeriodAndKeepsBoundaryFresh() {
        val a=account();assertEquals(0L,Readiness.waitMillis(a,24));assertEquals(5120L,Readiness.waitMillis(a,25));assertEquals(1120L,Readiness.waitMillis(a,29));assertEquals(0L,Readiness.waitMillis(a,30))
        assertEquals(1120L,Readiness.waitMillis(a.copy(period=60),59))
    }
    @Test fun clockWarningIgnoresNormalElapsedTimeAndDetectsBothDirections() {
        assertFalse(Readiness.clockChanged(10000,20000,1000,11000))
        assertTrue(Readiness.clockChanged(10000,25000,1000,11000))
        assertTrue(Readiness.clockChanged(10000,5000,1000,11000))
    }
}
