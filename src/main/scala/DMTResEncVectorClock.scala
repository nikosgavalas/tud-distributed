import scala.collection.mutable.ArrayBuffer
case class DMTREVCTimestamp(scalar: BigInt, frame: Int, frameHistory: Map[Int, BigInt], differences: Map[Int, Array[Int]])

class DMTResEncVectorClock(me: Int, n: Int) extends LogicalClock {
    type Rep = DMTREVCTimestamp

    // Prime assigned to this clock
    private val myPrime : Int = Primes.getPrime(me)

    // Current representation of DMTREVC
    private var scalar : BigInt = 1
    private var frame : Int = 0 
    private val frameHistory = scala.collection.mutable.Map[Int, BigInt]()

    // Keep track of latest merged frame from each of the other clocks
    private val differences = scala.collection.mutable.Map[Int, scala.collection.mutable.ArrayBuffer[Int]]()
    for (i <- 0 until n) {
        differences += (i -> ArrayBuffer[Int]())
    }

    // Max size for scalar to indicate overflows
    private val bitsToOverflow : Int = 16 

    def getTimestamp(): Rep = {
        // Saves current representation of REVC into an immutable timestamp

        // Copy frameHistory to an immutable Map
        var frameHistoryCopy = Map[Int, BigInt](frameHistory.toSeq: _*)
        val differencesImmutable = scala.collection.mutable.Map[Int, Array[Int]]()
        for ((tempFrame, tempBuffer) <- differences) {
            differencesImmutable += (tempFrame -> tempBuffer.toArray)
        }
        var differencesCopy = Map[Int, Array[Int]](differencesImmutable.toSeq: _*)

        // Create and return new timestamp
        new DMTREVCTimestamp(scalar, frame, frameHistoryCopy, differencesCopy)
    }

    def localTick(): Unit = {
        val temp = scalar * myPrime
        if (causesOverflow(temp)) {
            reset(myPrime)
        } else {
            scalar = temp
        }
    }
    	
    def mergedInto(index: Int) {
        differences += (index -> ArrayBuffer[Int]())
    }

    def mergeWith(merge: Any) : Unit = {
        val revcTimestamp = merge.asInstanceOf[Rep]
        val otherScalar = revcTimestamp.scalar
        val otherFrame = revcTimestamp.frame 
        val otherFrameHistory = revcTimestamp.frameHistory
        val otherDifferences = revcTimestamp.differences
        
        def addToFrameHistory(frameIndex : Int, value : BigInt) : Unit = {
            if (!(frameHistory.contains(frameIndex) && value == frameHistory.getOrElse(frameIndex, -1))) {
                // frameHistory needs to be updated
                frameHistory += (frameIndex -> value)
                
                // Update differences map 
                for ((curOtherIndex, curOtherList) <- differences) {
                    if (!curOtherList.contains(frameIndex)) {
                        differences += (curOtherIndex -> curOtherList.append(frameIndex))
                    }
                }
            } 
        }

        def mergeScalars(s1: BigInt, s2: BigInt) : BigInt = {
            // Returns LCM of two scalars
            (s1 * s2) / s1.gcd(s2)
        }
        
        def historyMerge() : Unit = {
            // Differential merge
            for (tempFrame <- otherDifferences.getOrElse(me, Array[Int]())) {
                val tempScalar = otherFrameHistory.getOrElse(tempFrame, BigInt(-1))
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

        // Returns true if Before, BeforeEqual or Same
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