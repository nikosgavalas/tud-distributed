trait LogicalClock {
    type Rep

    def getTimestamp() : Rep
    def localTick(): Unit
    def mergeWith(x: LogicalClock): Unit
    def compareWith(x: LogicalClock): TimestampOrdering
}
