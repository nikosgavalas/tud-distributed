package logicalclocks

case class REVCTimestamp(scalar: BigInt, frame: Int, frameHistory: Map[Int, BigInt])

class ResEncVectorClock(me: Int, n: Int) extends LogicalClock {
    type Rep = REVCTimestamp

    // Prime assigned to this clock
    private val myPrime : Int = Primes.getPrime(me)

    // Current representation of REVC
    private var scalar : BigInt = 1
    private var frame : Int = 0 
    private val frameHistory = scala.collection.mutable.Map[Int, BigInt]()

    // Max size for scalar to indicate overflows
    private val bitsToOverflow : Int = 16 

    def getTimestamp(): Rep = {
        // Saves current representation of REVC into an immutable timestamp

        // Copy frameHistory to an immutable Map
        var frameHistoryCopy = Map[Int, BigInt](frameHistory.toSeq: _*)

        // Create and return new timestamp
        new REVCTimestamp(scalar, frame, frameHistoryCopy)
    }

    def localTick(): Unit = {
        val temp = scalar * myPrime
        if (causesOverflow(temp)) {
            reset(myPrime)
        } else {
            scalar = temp
        }
    }

    def mergeWith(merge: Any) : Unit = {
        val revcTimestamp = merge.asInstanceOf[Rep]
        val otherScalar = revcTimestamp.scalar
        val otherFrame = revcTimestamp.frame 
        val otherFrameHistory = revcTimestamp.frameHistory
        
        def mergeScalars(s1: BigInt, s2: BigInt) : BigInt = {
            // Returns LCM of two scalars
            (s1 * s2) / s1.gcd(s2)
        }

        def historyMerge() : Unit = {
            // Merges frameHistory with otherFrameHistory

            for ((tempFrame, tempScalar) <- otherFrameHistory) {
                if (frameHistory.contains(tempFrame)) {
                    // Update tempFrame value in frameHistory
                    frameHistory += (tempFrame -> mergeScalars(frameHistory.getOrElse(tempFrame, 0), tempScalar))
                } else {
                    // Add tempFrame to frameHistory
                    frameHistory += (tempFrame -> tempScalar)
                }
            }
        }

        if (frame > otherFrame) {
            frameHistory += (otherFrame -> mergeScalars(frameHistory.getOrElse(otherFrame, 0), otherScalar))
            historyMerge()
        } else if (otherFrame > frame) {
            frameHistory += (frame -> scalar)
            frame = otherFrame 
            scalar = otherScalar
            historyMerge()
        } else {
            val temp = mergeScalars(scalar, otherScalar)
            if (causesOverflow(temp)) {
                reset(1)
            } else {
                scalar = temp
            }
            historyMerge()
        }
    }

    def compareWith(compare : Any) : Boolean = {
        val t2 = compare.asInstanceOf[Rep]

        // Returns true if logicalclocks.Before, logicalclocks.BeforeEqual or logicalclocks.Same
        if (frame > t2.frame) {
            return false 
        } else {
            val scalarToCompare = t2.frameHistory.getOrElse(frame, t2.scalar)
            return (scalar <= scalarToCompare && scalarToCompare % scalar == 0)
        }
    }

    def causesOverflow(toCheck: BigInt) : Boolean = {
        // Returns whether toCheck causes an overflow
        return (toCheck.bitLength > bitsToOverflow)
    }

    def reset(newScalarValue: BigInt) : Unit = {
        // Resets current representation of REVC 
        frameHistory += (frame -> scalar)
        frame += 1 
        scalar = newScalarValue
    }
}