package logicalclocks

import scala.collection.mutable.ArrayBuffer

/** The timestamp of DMTResEncVectorClock is represented by 
 *  an DMTREVCTimestamp, which consists of a BigInt for the 
 *  scalar, a integer for the frame number, a mapping 
 *  from integers to BigIntegers for the frame history, 
 *  and a mapping from integers to lists for the differences.
 */
class DMTREVCTimestamp(scalar: BigInt, frame: Int, frameHistory: Map[Int, BigInt], differences: Map[Int, List[Int]]) 
        extends REVCTimestamp(scalar, frame, frameHistory) {

    def getDifferences() : Map[Int, List[Int]] = {
        return differences
    }
}

/** DMTResEncVectorClock is an implementation of the differential 
 *  merge resettable encoded vector clock. (See 
 *  https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9234035)
 */
class DMTResEncVectorClock(me: Int, n: Int) extends ResEncVectorClock(me, n) {

    /** Keep track of latest merged frame from each of the other clocks.
     */
    protected val differences = scala.collection.mutable.Map[Int, scala.collection.mutable.ArrayBuffer[Int]]()
    for (i <- 0 until n) {
        differences += (i -> ArrayBuffer[Int]())
    }

    /** Returns a timestamp of the current state.
     *  Note: scalar is passed by value, so we 
     *  can not encounter referencing issues.
     *  
     *  @param receiver the receiver of the timestamp, which 
     *  is not relevant for the REVC implementation
     *  @return the current scalar
     */
    override def getTimestamp(receiver: Int): LCTimestamp = {
        // Copy frameHistory to an immutable Map
        val revcTimestamp = super.getTimestamp(receiver).asInstanceOf[REVCTimestamp]
        val differencesImmutable = scala.collection.mutable.Map[Int, List[Int]]()
        for ((tempFrame, tempBuffer) <- differences) {
            differencesImmutable += (tempFrame -> tempBuffer.toList)
        }
        var differencesCopy = Map[Int, List[Int]](differencesImmutable.toList: _*)
        
        // Reset differences for the receiver
        differences += (receiver -> ArrayBuffer[Int]())

        // Create and return new timestamp
        return new DMTREVCTimestamp(revcTimestamp.getScalar(), revcTimestamp.getFrame(),
            revcTimestamp.getFrameHistory(), differencesCopy)
    }

    /** Updates the frame history by merging the timestamp
     *  into the clock.
     *  
     *  Note that we are reusing REVC.mergeWith as a template function 
     *  which means this function will be passed an REVCTimestamp instead 
     *  of a DMTREVCTimestamp. Hence, we will first have to downcast.
     *  
     *  @param otherTimestamp the timestamp to be merged into the clock
     */ 
    def historyMerge(otherTimestamp : DMTREVCTimestamp) : Unit = {
        val dmtOtherTimestamp = otherTimestamp.asInstanceOf[DMTREVCTimestamp]
        val otherFrameHistory = dmtOtherTimestamp.getFrameHistory()
        val otherDifferences = dmtOtherTimestamp.getDifferences()

        // Differential merge
        for (tempFrame <- otherDifferences.getOrElse(me, otherFrameHistory.keys)) {
            val tempScalar = otherFrameHistory.getOrElse(tempFrame, BigInt(-1))
            if (frameHistory.contains(tempFrame)) {
                // Update tempFrame value in frameHistory
                addToFrameHistory(tempFrame, getLCM(frameHistory.getOrElse(tempFrame, 0), tempScalar))
            } else {
                // Add tempFrame to frameHistory
                addToFrameHistory(tempFrame, tempScalar)
            }
        }
    }

    /** Adds the frameIndex and value to the frameHistory map.
     *  
     *  Also keeps track of what parts of its history have changed 
     *  for each other DMTREVC since the last respective merge. 
     *  Note that this implementation is quite inefficient, but 
     *  it adheres exactly to the description in the paper that 
     *  presents the DMTREVC. 
     * 
     *  @param frameIndex the key for the new frameHistory entry 
     *  @param value the value for the new frameHistory entry
     */
    override protected def addToFrameHistory(frameIndex : Int, value : BigInt) : Unit = {
        val oldValue = frameHistory.getOrElse(frameIndex, -1)
        super.addToFrameHistory(frameIndex, value)
        val newValue = frameHistory.getOrElse(frameIndex, -1)

        if (oldValue != newValue) {
            // Update differences map 
            for ((otherIndex, otherList) <- differences) {
                if (!otherList.contains(frameIndex)) {
                    otherList += frameIndex
                    differences += (otherIndex -> otherList)
                }
            }
        } 
    }

    /** Returns the number of bits needed to represent the timestamp of the clock. 
     *  
     *  Note that in the REVC we use the scalar in the EVC, with an additional frame 
     *  and frameHistory defined in the REVC. In the DMTREVC we add the differences map. 
     *  Hence, we use the getSizeBits of the REVC, and add it to the number of bits needed
     *  to represent the differences map.
     * 
     *  @return the number of bits needed to represent the timestamp of this clock
     */ 
    override def getSizeBits: Int = {
        val revcSize = super.getSizeBits

        // the size of a list is 32 bits, plus the number of elements * 32 bits
        val diffsSize = differences.map{ case (_, intlist) => 32 + intlist.length * 32 }.sum

        return revcSize + diffsSize
    }
}