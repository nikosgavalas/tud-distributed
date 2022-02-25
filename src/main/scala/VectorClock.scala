class VectorClock(me: Int, n: Int) extends LogicalClock {
    type ImplementedLogicalClock = VectorClock
    type InternalRepresentation = Array[Int]

    private val vector = new Array[Int](n)

    def localTick(): Unit = {
        vector(me) = vector(me) + 1

    }

    def mergeWith(a: ImplementedLogicalClock) : Unit = {

        println("test merge with")
    }



     def compareWith(a: ImplementedLogicalClock): Boolean = {

         println("test compare with")
         return true

     }

}
