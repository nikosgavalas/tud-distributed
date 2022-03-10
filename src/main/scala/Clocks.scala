import scala.collection.mutable.ListBuffer

class Clocks(clocksActive: List[String]) {
    val clocks = new ListBuffer[LogicalClock]()

    def initialize(index: Int, numPeers: Int): Unit = {
        if (clocksActive.contains("VC"))
            clocks += new VectorClock(index, numPeers)
        if (clocksActive.contains("EVC"))
            clocks += new EncVectorClock(index, numPeers)
        if (clocksActive.contains("REVC"))
            clocks += new ResEncVectorClock(index, numPeers)
        if (clocksActive.contains("DMTREVC"))
            clocks += new DMTResEncVectorClock(index, numPeers)
    }

    def tick(): Unit = {
        clocks.foreach(clock => clock.localTick())
    }

    def getTimestamps(): List[Any] = {
        clocks.map(clock => clock.getTimestamp()).toList
    }

    def merge(timestamps: List[Any]): Unit = {
        clocks.zip(timestamps).foreach{case (clock, timestamp) => clock.mergeWith(timestamp)}
    }

    def allConsistent(timestamps: List[Any]): Boolean = {
        val checks = clocks.zip(timestamps).map{case (clock, timestamp) => clock.compareWith(timestamp)}.toList
        val all_true = checks.reduce((i, j) => i && j)
        val all_false = ! checks.reduce((i, j) => i || j)
        all_true || all_false
    }
}