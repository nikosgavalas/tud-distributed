import logicalclocks.{DMTResEncVectorClock, EncVectorClock, LogicalClock, ResEncVectorClock, VectorClock}

import scala.collection.mutable

class ClocksWrapper(index: Int, numPeers: Int, selectedClocks: List[String]) {
    val clocks: mutable.Map[String, LogicalClock] = mutable.SortedMap[String, LogicalClock]()

    if (selectedClocks.contains("VC"))
        clocks.addOne(("VC", new VectorClock(index, numPeers)))
    if (selectedClocks.contains("EVC"))
        clocks.addOne(("EVC", new EncVectorClock(index, numPeers)))
    if (selectedClocks.contains("REVC"))
        clocks.addOne(("REVC", new ResEncVectorClock(index, numPeers)))
    if (selectedClocks.contains("DMTREVC"))
        clocks.addOne(("DMTREVC", new DMTResEncVectorClock(index, numPeers)))

    def getClock(clockStr: String): LogicalClock = {
        clocks(clockStr)
    }

    def tick(): Unit = {
        clocks.foreach{ case (_, clock) => clock.localTick() }
    }

    def getTimestamps: List[Any] = {
        clocks.map{ case (_, clock) => clock.getTimestamp() }.toList
    }

    def merge(timestamps: List[Any]): Unit = {
        clocks.zip(timestamps).foreach{ case ((_, clock), timestamp) => clock.mergeWith(timestamp) }
    }

    def allConsistent(timestamps: List[Any]): Boolean = {
        val checks = clocks.zip(timestamps).map{ case ((_, clock), timestamp) => clock.compareWith(timestamp) }.toList
        val all_true = checks.reduce((i, j) => i && j)
        val all_false = ! checks.reduce((i, j) => i || j)
        all_true || all_false
    }
}
