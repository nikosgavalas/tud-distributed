trait LogicalClock {
    def localTick(): Unit
    def mergeWith(x: LogicalClock): Unit
    def compareWith(x: LogicalClock): TimestampOrdering
}
