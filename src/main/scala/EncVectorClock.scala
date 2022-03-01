class EncVectorClock(me: Int, n: Int) {
    type Rep = BigInt

    private var scalar : BigInt = 1
    private val myPrime : Int = Primes.getPrime(me)

    def getTimestamp(): Rep = {
        scalar
    }

    def localTick(): Unit = {
        scalar = scalar * myPrime
    }

    def mergeWith(mergeScalar: Rep) : Unit = {
        scalar = (scalar * mergeScalar) / scalar.gcd(mergeScalar)
    }

     def compareWith(otherScalar: Rep): TimestampOrdering = {        
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
