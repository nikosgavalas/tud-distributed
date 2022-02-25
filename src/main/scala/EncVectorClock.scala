class EncVectorClock(me: Int, n: Int) {
  type ImplementedLogicalClock = EncVectorClock

    private var scalar : BigInt = 1
    private val myPrime : Int = Primes.getPrime(me)

    def getScalar(): BigInt = {
        return scalar
    }

    def localTick(): Unit = {
        scalar = scalar * myPrime
    }

    def mergeWith(a: ImplementedLogicalClock) : Unit = {
        // Get representation of passed vector clock
        val mergeScalar = a.getScalar()
        scalar = (scalar * mergeScalar) / scalar.gcd(mergeScalar)
        
    }

     def compareWith(a: ImplementedLogicalClock): TimestampOrdering = {
        // Get representation of passed vector clock
        val otherScalar = a.getScalar()
        
        // TODO: Think about a way to extract strict before and after relation
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
