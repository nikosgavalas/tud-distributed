class VectorClock(me: Int, n: Int) extends LogicalClock {
    type ImplementedLogicalClock = VectorClock
    type InternalRepresentation = Array[Int]

    private val vector = new Array[Int](n)

    def getVector(): Array[Int] = {
        return vector.clone
    }

    def localTick(): Unit = {
        vector(me) = vector(me) + 1
    }

    def mergeWith(a: ImplementedLogicalClock) : Unit = {
        // Get representation of passed vector clock
        val mergeVector = a.getVector()
        assert(mergeVector.length == n)

        for (i <- 0 until n) {
            // Merge vectors component-wise
            vector(i) = vector(i).max(mergeVector(i))
        }
    }

     def compareWith(a: ImplementedLogicalClock): TimestampOrdering = {
        // Get representation of passed vector clock
        val otherVector = a.getVector()
        assert(otherVector.length == n)
        
        // Initialize current ordering with first element of vectors
        var curOrdering : TimestampOrdering = null
        if (vector(0) < otherVector(0)) {
            curOrdering = Before 
        } else if (vector(0) > otherVector(0)) {
            curOrdering = After 
        } else {
            curOrdering = Same
        }

        for (i <- 1 until n) {
            curOrdering match {
                case Before | BeforeEqual => {
                    if (vector(i) > otherVector(i)) {
                        return Concurrent
                    } else if (vector(i) == otherVector(i)) {
                        curOrdering = BeforeEqual
                    }
                }
                case After | AfterEqual => {
                    if (vector(i) < otherVector(i)) {
                        return Concurrent
                    } else if (vector(i) == otherVector(i)) {
                        curOrdering = AfterEqual
                    }
                } 
                case Same => {
                    if (vector(i) < otherVector(i)) {
                        curOrdering = BeforeEqual
                    } else if (vector(i) > otherVector(i)) {
                        curOrdering = AfterEqual
                    }
                }
            }
        }

        return curOrdering
     }

}
