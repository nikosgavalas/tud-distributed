class ResEncVectorClock(me: Int, n: Int) {
    case class REVCTimestamp(scalar: BigInt, frame: Int, frameHistory: Map[Int, BigInt])
    type Rep = REVCTimestamp

    private val myPrime : Int = Primes.getPrime(me)
    private var scalar : BigInt = 1
    private var frame : Int = 0 
    private val frameHistory = scala.collection.mutable.Map[Int, BigInt]()

    def getTimestamp(): Rep = {
        // TODO, fix frameHistory type
        new REVCTimestamp(scalar, frame, frameHistory.clone())
    }

    def localTick(): Unit = {
        // TODO
    }

    def mergeWith(revcTimestamp: Rep) : Unit = {
        val otherScalar = revcTimestamp.scalar
        val otherFrame = revcTimestamp.frame 
        val otherFrameHistory = revcTimestamp.frameHistory
        // TODO
    }

     def compareWith(otherScalar: Rep): TimestampOrdering = {        
        // TODO 
     }
}
