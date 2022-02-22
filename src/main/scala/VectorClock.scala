class VectorClock extends LogicalClock {
    type ImplLogicalClock = VectorClock

    def localTick(): Unit = {
        println("test local tick")
    }

    def mergeWith(a: ImplLogicalClock) : Unit = {
        println("test merge with")
    }
    
    def compareWith(a: ImplLogicalClock): Boolean = {
        println("test compare with")
        return true
    }
}
