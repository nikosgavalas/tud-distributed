class VectorClock(me: Int, n: Int) extends LogicalClock {
    type ImplLogicalClock = VectorClock

    private val vector = new Array[Int](n)

    def localTick(): Unit = {
        vector(me) = vector(me) + 1

    }

    def mergeWith(a: ImplLogicalClock) : Unit = {

        println("test merge with")
    }



     def compareWith(a: ImplLogicalClock): Boolean = {

         println("test compare with")
         return true

     }

     /* def canEqual(a: Any) = a.isInstanceOf[VectorClock] */

     /* def compareWith(a: ImplLogicalClock): Boolean = {

        def equals(that: Any): Boolean =
            that match {
                case that: VectorClock => that.canEqual(this) && this.hashCode == that.hashCode
                case _ => false
            }
        println("test compare with")
        return true
     } */


}
