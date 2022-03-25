package logicalclocks

class VectorClock(me: Int, n: Int) extends LogicalClock {
    type Rep = List[Int]

    private val vector = Array.fill(n)(0)

    def getTimestamp() : Rep = {
        vector.clone.toList
    }

    def localTick(): Unit = {
        vector(me) = vector(me) + 1
    }

    def mergeWith(merge: Any) : Unit = {
        val mergeVector = merge.asInstanceOf[Rep]
        assert(mergeVector.length == n)

        for (i <- 0 until n) {
            // Merge vectors component-wise
            vector(i) = vector(i).max(mergeVector(i))
        }
    }

    def compareWith(compare: Any): Boolean = {
        val otherVector = compare.asInstanceOf[Rep]
        // Returns true if logicalclocks.Before, logicalclocks.BeforeEqual or logicalclocks.Same
        return List(Before, BeforeEqual, Same).contains(getOrdering(otherVector))
    }

    def getOrdering(otherVector: Rep): TimestampOrdering = { 
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

    override def getSizeBits: Int = {
        vector.length * 32
    }
}
