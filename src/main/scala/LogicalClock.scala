trait LogicalClock {
    // define type of implementing class
    type ImplementedLogicalClock 

    def localTick(): Unit 
    def mergeWith(x: ImplementedLogicalClock): Unit 
    def compareWith(x: ImplementedLogicalClock): TimestampOrdering
}
