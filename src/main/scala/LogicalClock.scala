trait LogicalClock {
    // define type of implementing class
    type ImplLogicalClock 

    def localTick(): Unit 
    def mergeWith(x: ImplLogicalClock): Unit 
    def compareWith(x: ImplLogicalClock): Boolean
}
