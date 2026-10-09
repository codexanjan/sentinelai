package app.sentinel.authenticator.core

import kotlin.math.abs

object Readiness {
    /** Leave at least six seconds when the user requests a fresh copy. */
    fun waitMillis(a:Account,seconds:Long):Long = Otp.remaining(a,seconds).let { if(it<=5)it*1000L+120L else 0L }
    fun clockChanged(oldWall:Long,newWall:Long,oldMonotonic:Long,newMonotonic:Long):Boolean =
        abs((newWall-oldWall)-(newMonotonic-oldMonotonic))>2000L
}
