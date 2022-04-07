package logicalclocks

/** The timestamp of EncVectorClock is represented by 
 *  a Big Integer. 
 */
class EVCTimestamp(scalar: BigInt) extends LCTimestamp  {
    def getScalar() : BigInt = {
        return scalar
    }
}

/** EncVectorClock is an implementation of the encoded 
 *  vector clock. (See 
 *  https://dl.acm.org/doi/abs/10.1145/3154273.3154305)
 */
class EncVectorClock(me: Int, n: Int) extends LogicalClock {
    
    /** The unique prime given to this instance of 
     *  EncVectorClock.
     */
    protected val myPrime : Int = Primes.getPrime(me)

    /** Current clock value.
     */
    protected var scalar : BigInt = 1

    /** Returns a timestamp of the current state.
     *  Note: scalar is passed by value, so we 
     *  can not encounter referencing issues.
     * 
     *  @param receiver the receiver of the timestamp, which 
     *  is not relevant for the EVC implementation
     *  @return the current scalar wrapped in EVCTimestamp
     */
    override def getTimestamp(receiver: Int): LCTimestamp = {
        return new EVCTimestamp(scalar)
    }

    /** Increments the value of this clock in the 
     *  scalar by multiplying by the assigned prime.
     */
    override def localTick(): Unit = {
        scalar = scalar * myPrime
    }

    /** Updates the scalar by merging the timestamp 
     *  into the clock.
     * 
     *  @param mergeTimestamp the timestamp to be merged into the clock
     */ 
    override def mergeWith(mergeTimestamp: LCTimestamp) : Unit = {
        val otherScalar = mergeTimestamp.asInstanceOf[EVCTimestamp].getScalar()
        scalar = getLCM(scalar, otherScalar)
    }

    /** Returns whether the current clock value is logically before, 
     *  beforeEqual or equal to the passed timestamp compareTimestamp. 
     * 
     *  @param compareTimestamp the timestamp to be compared with the clock
     *  @return whether the current clock value happened before x
     */ 
    override def happenedBefore(compareTimestamp: LCTimestamp): Boolean = {        
        val otherScalar = compareTimestamp.asInstanceOf[EVCTimestamp].getScalar()
        return (scalar <= otherScalar && otherScalar % scalar == 0)
    }

    /** Returns the LCM of the two scalars.
     * 
     *  @param s1 the first scalar 
     *  @param s2 the second scalar 
     *  @return the LCM of the two scalars
     */
    protected def getLCM(s1: BigInt, s2: BigInt) : BigInt = {
        // Returns LCM of two scalars
        (s1 * s2) / s1.gcd(s2)
    }

    /** Returns the number of bits needed to represent the timestamp of the clock.
     * 
     *  @return the number of bits needed to represent the timestamp of this clock
     */  
    override def getSizeBits: Int = {
        return scalar.bitLength
    }
}