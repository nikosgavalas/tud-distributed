import ChildActor.{BeginMessage, ControlMessage}
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import logicalclocks.DMTResEncVectorClock

import scala.util.Random

object Config {
    val doWork: Boolean = false
    val maxWorkTime: Int = 20
    val debug: Boolean = true  // akka ignores the loglevel config for some reason so we 'll do it "manually"
}

object ChildActor {
    sealed trait Message

    // messages from the parent
    final case class ControlMessage(printBitsizes: Boolean,
                                    maxMessagesPerChild: Int,
                                    peers: List[ActorRef[ChildActor.Message]],
                                    parentActor: ActorRef[ParentActor.Message],
                                    childIndex: Int,
                                    selectedClocks: List[String]
                                   ) extends Message

    // messages to/from other children
    final case class PeerMessage(content: String, timestamps: List[Any]) extends Message

    // message from the parent that signals the start of message deliveries
    final case class BeginMessage() extends Message

    var allPeers: List[ActorRef[Message]] = List()

    def broadcast(peerMessage: PeerMessage, context: ActorContext[Message]): Unit = {
        // send to all except self
        for (i <- allPeers) {
            if (i.path.name != context.self.path.name)
                i ! peerMessage
        }
    }

    def doWork(): Unit = {
        Thread.sleep(Random.between(1, Config.maxWorkTime))
    }

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var myIndex: Int = -1
        var messageCounter: Int = 0
        var printBits = false
        var maxMessages = 1
        var parentActor: ActorRef[ParentActor.Message] = null

        var clocks: ClocksWrapper = null
        var selectedClocksList: List[String] = null

        context.log.debug(s"ChildActor ${context.self.path.name} up")

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(printBitsizes, maxMessagesPerChild, peers, parent, childIndex, selectedClocks) =>
                    // store the configs
                    printBits = printBitsizes
                    allPeers = peers
                    maxMessages = maxMessagesPerChild
                    parentActor = parent
                    myIndex = childIndex
                    selectedClocksList = selectedClocks

                    // each actor initializes their clocks
                    clocks = new ClocksWrapper(myIndex, peers.length, selectedClocksList)

                    context.log.debug(s"${context.self.path.name} received peers $peers")

                case BeginMessage() =>
                    clocks.tick()
                    allPeers(1) ! PeerMessage("init msg", clocks.getTimestamps)

                case PeerMessage(content, timestamps) =>
                    if (Config.debug) {
                        context.log.debug(s"${context.self.path.name} received '$content' with timestamps: ${selectedClocksList.zip(timestamps)}")
                        context.log.debug(s"${selectedClocksList.zip(clocks.getMemSizes)}")
                    }

                    // merge and tick
                    clocks.merge(timestamps)
                    clocks.tick()

                    if (Config.doWork)
                        doWork()

                    if (!clocks.allConsistent(timestamps)) {
                        println("clocks inconsistent")
                        sys.exit(1)
                    }

                    // tick and send to a randomly selected peer
                    clocks.tick()
                    val receivingPeer = Random.between(0, allPeers.length)
                    allPeers(receivingPeer) ! PeerMessage("msg", clocks.getTimestamps)
                    // for the DMTREVC specifically, we need to also call mergedInto
                    if (selectedClocksList.contains("DMTREVC")) {
                        clocks.getClock("DMTREVC").asInstanceOf[DMTResEncVectorClock].mergedInto(receivingPeer)
                    }

                    messageCounter += 1

                    // only the first child logs its bitsizes
                    if (printBits && myIndex == 1)
                        println(s"eventnum/bitsize: $messageCounter ${clocks.getMemSizes.sum}")

                    if (messageCounter == maxMessages) {
                        parentActor ! ParentActor.ChildDone()
                    }
            }

            Behaviors.same
        }
    }
}

object ParentActor {
    sealed trait Message

    final case class SpawnActors(printBitsizes: Boolean,
                                 maxMessagesPerChild: Int,
                                 nActors: Int,
                                 selectedClocks: List[String]) extends Message

    final case class ChildDone() extends Message

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var numberOfChildren = 0
        var childrenDone = 0
        var processList: List[ActorRef[ChildActor.Message]] = null
        val startTime = System.nanoTime

        Behaviors.receive { (context, message) =>
            message match {
                case SpawnActors(printBitsizes, maxMessagesPerChild, nActors, selectedClocks) =>
                    numberOfChildren = nActors

                    // upon receiving the message, spawn the children
                    processList = (0 until numberOfChildren)
                      .map(childIndex => context.spawn(ChildActor(), "Process-" + childIndex)).toList

                    // send relevant information to each child
                    processList.zip(0 until numberOfChildren).foreach{case (child, childIndex) =>
                        child ! ControlMessage(printBitsizes, maxMessagesPerChild, processList, context.self,
                            childIndex, selectedClocks)}

                    // send a BeginMessage to the first child to trigger the message deliveries
                    processList.head ! BeginMessage()

                case ChildDone() =>
                    // when all children finish, exit.
                    childrenDone += 1
                    if (childrenDone == numberOfChildren) {
                        for (process <- processList) {
                            context.stop(process)
                        }
                        println(s"duration: ${(System.nanoTime - startTime) / 1e9d} seconds")
                        sys.exit(0)
                    }
            }
            Behaviors.same
        }
    }
}

object Main extends App {

    if (args.length < 4) {
        println("Run with arguments: <print bitsizes: true/false> <number of messages per child: int> <number of actors: int> <clocks separated with commas: str>")
        sys.exit(1)
    }
    val printBitsizes: Boolean = args(0).toBoolean
    val maxMessagesPerChild: Int = args(1).toInt
    val nActors: Int = args(2).toInt
    val selectedClocks: List[String] = args(3).split(",").toList.sorted
    println(s"Starting execution with: $printBitsizes bitsize output, $maxMessagesPerChild messages, $nActors actors and $selectedClocks clocks")

    // start the actor system
    val parentActor: ActorSystem[ParentActor.SpawnActors] = ActorSystem(ParentActor(), "Main")

    // send a message to ParentActor to spawn children
    parentActor ! SpawnActors(printBitsizes, maxMessagesPerChild, nActors, selectedClocks)
}