package logicalclocks

/** The timestamp of VectorClock is represented by 
 *  a list of integers, where each integer corresponds
 *  to a vector clock instance.
 */
class VCTimestamp(vector: List[Int]) extends LCTimestamp  {
    def getVector() : List[Int] = {
        return vector
    }
}

/** VectorClock is an implementation of the vector clock.
 */
class VectorClock(me: Int, n: Int) extends LogicalClock {

    /** Current clock value.
     */
    protected val vector = Array.fill(n)(0)

    /** Returns a timestamp of the current state.
     * 
     *  @param receiver the receiver of the timestamp, which 
     *  is not relevant for the VectorClock implementation
     *  @return a copy of the current vector clock list
     */
    override def getTimestamp(receiver: Int) : LCTimestamp = {
        return new VCTimestamp(vector.clone.toList)
    }
    
    /** Increments the value of this clock in the 
     *  vector clock list.
     */
    override def localTick(): Unit = {
        vector(me) = vector(me) + 1
    }

    /** Updates the vector clock list by merging timestamp 
     *  x into the clock.
     * 
     *  @param x the timestamp to be merged into the clock
     */ 
    override def mergeWith(mergeTimestamp: LCTimestamp) : Unit = {
        val mergeVector = mergeTimestamp.asInstanceOf[VCTimestamp].getVector()

        for (i <- 0 until n) {
            // Merge vectors component-wise
            vector(i) = vector(i).max(mergeVector(i))
        }
    }

    /** Returns whether the current clock value is logically before, 
     *  beforeEqual or equal to the passed timestamp compareTimestamp. 
     * 
     *  @param compareTimestamp the timestamp to be compared with the clock
     *  @return whether the current clock value happened before x
     */ 
    override def happenedBefore(compareTimestamp: LCTimestamp): Boolean = {
        val otherVector = compareTimestamp.asInstanceOf[VCTimestamp].getVector()
        for (i <- 0 until otherVector.length) {
            if (otherVector(i) > vector(i)) {
                return false
            }
        }

        return true
    }

    /** Converts the current vector clock value to a string.
     *  @return a string representation of the current vector clock value
     */ 
    override def toString: String = {
        return me + ": " + vector.mkString("(", ", ", ")")
    }

    /** Returns the number of bits needed to represent the timestamp of the clock.
     * 
     *  @return the number of bits needed to represent the timestamp of this clock
     */ 
    override def getSizeBits: Int = {
        return vector.length * 32
    }
}
