class VectorClock(me: Int, n: Int) extends LogicalClock {
    private val vector = Array.fill(n)(0)

    def getVector(): Array[Int] = {
        vector.clone
    }

    def localTick(): Unit = {
        vector(me) = vector(me) + 1
        println("local tick in clock:" + me.toString)
    }

    def mergeWith(a: LogicalClock) : Unit = {
        val clock = a.asInstanceOf[VectorClock]
        // Get representation of passed vector clock
        val mergeVector = clock.getVector()
        assert(mergeVector.length == n)

        for (i <- 0 until n) {
            // Merge vectors component-wise
            vector(i) = vector(i).max(mergeVector(i))
        }
    }

    def compareWith(a: LogicalClock): TimestampOrdering = {
        val clock = a.asInstanceOf[VectorClock]
        // Get representation of passed vector clock
        val otherVector = clock.getVector()
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

        curOrdering
    }

    override def toString: String = {
        me + ": " + vector.mkString("(", ", ", ")")
    }
}
