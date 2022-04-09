import logicalclocks.{LCTimestamp, LogicalClockComparator, DMTResEncVectorClock, EncVectorClock, LogicalClock, ResEncVectorClock, VectorClock}

class ClocksWrapper(index: Int, numPeers: Int, selectedClocks: List[String]) {
    var clocks: List[(LogicalClockComparator, LogicalClock)] = List()

    if (selectedClocks.contains("VC"))
        clocks = clocks:+(VectorClock, new VectorClock(index, numPeers))
    if (selectedClocks.contains("EVC"))
        clocks = clocks:+(EncVectorClock, new EncVectorClock(index, numPeers))
    if (selectedClocks.contains("REVC"))
        clocks = clocks:+(ResEncVectorClock, new ResEncVectorClock(index, numPeers))
    if (selectedClocks.contains("DMTREVC"))
        clocks = clocks:+(ResEncVectorClock, new DMTResEncVectorClock(index, numPeers))

    def tick(): Unit = {
        clocks.foreach{ case (_, clock) => clock.localTick() }
    }

    def getTimestamps(receiver: Int): List[LCTimestamp] = {
        return clocks.map{ case (_, clock) => clock.getTimestamp(receiver) }.toList
    }

    def merge(timestamps: List[LCTimestamp]): Unit = {
        clocks.zip(timestamps).foreach{ case ((_, clock), timestamp) => clock.mergeWith(timestamp) }
    }

    def allConsistent(timestamps: List[LCTimestamp]): Boolean = {
        val checks = clocks.zip(timestamps).map{ case ((comparator, clock), timestamp) => comparator.happenedBefore(clock.getTimestamp(), timestamp) }.toList
        val all_true = checks.reduce((i, j) => i && j)
        val all_false = ! checks.reduce((i, j) => i || j)
        return all_true || all_false
    }

    def getMemSizes: List[Int] = {
        return clocks.map{ case (_, clock) => clock.getSizeBits }.toList
    }
}
