class EncVectorClock(me: Int, n: Int) {
    private var scalar : BigInt = 1
    private val myPrime : Int = Primes.getPrime(me)

    def getScalar(): BigInt = {
        scalar
    }

    def localTick(): Unit = {
        scalar = scalar * myPrime
    }

    def mergeWith(a: LogicalClock) : Unit = {
        // Get representation of passed vector clock
        val clock = a.asInstanceOf[EncVectorClock]
        val mergeScalar = clock.getScalar()
        scalar = (scalar * mergeScalar) / scalar.gcd(mergeScalar)
    }

     def compareWith(a: LogicalClock): TimestampOrdering = {
        // Get representation of passed vector clock
        // Get representation of passed vector clock
        val clock = a.asInstanceOf[EncVectorClock]
        val otherScalar = clock.getScalar()
        
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
