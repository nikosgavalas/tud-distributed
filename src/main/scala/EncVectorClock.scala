class EncVectorClock(me: Int, n: Int) extends LogicalClock {
    type Rep = BigInt

    // Prime assigned to this clock
    private val myPrime : Int = Primes.getPrime(me)

    // Representation of EVC
    private var scalar : BigInt = 1

    def getTimestamp(): Rep = {
        scalar
    }

    def localTick(): Unit = {
        scalar = scalar * myPrime
    }

    def mergeWith(merge: Any) : Unit = {
        val mergeScalar = merge.asInstanceOf[Rep]
        scalar = (scalar * mergeScalar) / scalar.gcd(mergeScalar)
    }

     def compareWith(compare: Any): Boolean = {        
        val otherScalar = compare.asInstanceOf[Rep]
        // Returns true if Before, BeforeEqual or Same
        return List(Before, BeforeEqual, Same).contains(getOrdering(otherScalar))
     }

     def getOrdering(otherScalar: Rep) : TimestampOrdering = {
        if (scalar < otherScalar && otherScalar % scalar == 0) {
            return BeforeEqual
        } else if (scalar > otherScalar && scalar % otherScalar == 0) {
            return AfterEqual
        } else if (scalar == otherScalar) {
            return Same 
        } else {
            return Concurrent
        }
     }
}