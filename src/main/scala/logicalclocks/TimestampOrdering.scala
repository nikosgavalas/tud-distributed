package logicalclocks

// This has been inspired by the Akka logicalclocks.VectorClock implementation
// https://github.com/akka/akka/blob/main/akka-cluster/src/main/scala/akka/cluster/VectorClock.scala
sealed trait TimestampOrdering
case object After extends TimestampOrdering
case object AfterEqual extends TimestampOrdering
case object Same extends TimestampOrdering
case object Before extends TimestampOrdering
case object BeforeEqual extends TimestampOrdering
case object Concurrent extends TimestampOrdering
