package logicalclocks

trait LogicalClock {
    type Rep

    def getTimestamp(): Rep

    def localTick(): Unit

    def mergeWith(x: Any): Unit

    def compareWith(x: Any): Boolean

    def getSizeBits: Int
}
